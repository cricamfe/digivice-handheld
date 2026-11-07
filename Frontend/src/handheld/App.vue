<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch, type Component } from "vue";
import "./kit.css";
import NavBar from "./components/NavBar.vue";
import AppPanel from "./components/AppPanel.vue";
import BatallaCartasScreen from "./screens/BatallaCartasScreen.vue";
import BatallaScreen from "./screens/BatallaScreen.vue";
import DiarioScreen from "./screens/DiarioScreen.vue";
import EquipoScreen from "./screens/EquipoScreen.vue";
import EsperaScreen from "./screens/EsperaScreen.vue";
import EvolucionesScreen from "./screens/EvolucionesScreen.vue";
import MapaScreen from "./screens/MapaScreen.vue";
import MisionScreen from "./screens/MisionScreen.vue";
import MobiusScreen from "./screens/MobiusScreen.vue";
import SeabedRutaScreen from "./screens/SeabedRutaScreen.vue";
import SeabedScreen from "./screens/SeabedScreen.vue";
import SubastasScreen from "./screens/SubastasScreen.vue";
import TecnicasScreen from "./screens/TecnicasScreen.vue";
import WikiBuscar from "./screens/wiki/WikiBuscar.vue";
import WikiCarta from "./screens/wiki/WikiCarta.vue";
import WikiEnemigo from "./screens/wiki/WikiEnemigo.vue";
import WikiEquipo from "./screens/wiki/WikiEquipo.vue";
import WikiIndice from "./screens/wiki/WikiIndice.vue";
import WikiLista from "./screens/wiki/WikiLista.vue";
import WikiNpc from "./screens/wiki/WikiNpc.vue";
import WikiObjeto from "./screens/wiki/WikiObjeto.vue";
import WikiSobre from "./screens/wiki/WikiSobre.vue";
import WikiTienda from "./screens/wiki/WikiTienda.vue";
import { waitKindOf, type WaitKind } from "./connection";
import { mapKindOf, placeOf, rememberPlace } from "./map-view";
import { navSectionOf, useScreen, type ScreenName } from "./router";
import { holdGameState } from "@/composables/use-game-state";
import { useGameStore } from "@/stores/use-game-store";

const store = useGameStore();
const { current, param, go } = useScreen();

/** The screen of every name the nav can reach. */
const screens: Record<ScreenName, Component> = {
  equipo: EquipoScreen,
  mapa: MapaScreen,
  batalla: BatallaScreen,
  batallaCartas: BatallaCartasScreen,
  seabed: SeabedScreen,
  seabedRuta: SeabedRutaScreen,
  mobius: MobiusScreen,
  wikiMobius: MobiusScreen,
  diario: DiarioScreen,
  mision: MisionScreen,
  subastas: SubastasScreen,
  evoluciones: EvolucionesScreen,
  tecnicas: TecnicasScreen,
  wikiIndice: WikiIndice,
  wikiLista: WikiLista,
  wikiBuscar: WikiBuscar,
  wikiEnemigo: WikiEnemigo,
  wikiCarta: WikiCarta,
  wikiSobre: WikiSobre,
  wikiTienda: WikiTienda,
  wikiEquipo: WikiEquipo,
  wikiObjeto: WikiObjeto,
  wikiNpc: WikiNpc,
};

/**
 * Why there is no state to draw, or null when there is one. The core clears the state when it loses the game
 * (`syncHealth`), so no screen is left showing old data; the router keeps the screen the player was on, and it is
 * back as soon as the game sends its state again.
 */
const rawWaitKind = computed(() =>
  store.currentState !== null ? null : waitKindOf(store.healthStatus, store.lastErrorCode),
);

/**
 * The screens keep drawing the last state for this long when it goes, before the waiting screen replaces them. A read that
 * fails once clears the state for a moment: replacing the screen for it unmounts it (the Mapa reloads its picture, the
 * screens lose their own state) and flashes the waiting screen for a few frames.
 */
const WaitGraceMs = 1200;
/** The last state the game sent, for the screens to go on drawing during the grace. */
let heldState = store.currentState;
watch(
  () => store.currentState,
  (state) => {
    if (state !== null) {
      heldState = state;
      holdGameState(state);
    }
  },
  { immediate: true, flush: "sync" },
);

const waitKind = ref<WaitKind | null>(rawWaitKind.value);
let graceTimer: number | undefined;
watch(rawWaitKind, (kind) => {
  window.clearTimeout(graceTimer);
  if (kind === null || waitKind.value !== null || heldState === null) {
    waitKind.value = kind;
    return;
  }

  graceTimer = window.setTimeout(() => {
    waitKind.value = rawWaitKind.value;
  }, WaitGraceMs);
});
watch(
  waitKind,
  (kind) => {
    if (kind !== null) {
      // The waiting screen is up and the screens are gone: nothing draws the old state any more. After the render, or the
      // screens would still be asked for it.
      heldState = null;
      holdGameState(null);
    }
  },
  { flush: "post" },
);
onBeforeUnmount(() => window.clearTimeout(graceTimer));

/**
 * What the Mapa tab shows for the place the player is in. While waiting there is no place, so it keeps the last one:
 * losing the state for a moment is not a change of place, and must not drop the screen the player was on.
 */
const rawMapKind = computed<ReturnType<typeof mapKindOf>>((previous) => {
  const state = store.currentState;
  return state === null ? (previous ?? "mapa") : mapKindOf(placeOf(state.player.mapId));
});

/**
 * `rawMapKind`, but a change of kind has to last this long to count. The game goes through other map ids while it changes
 * place; switching the whole screen for one that stays a few frames (and back) drops the screen the player was on, which
 * mounts again and reloads its picture. The cost: a battle opens this much later.
 */
const MapKindGraceMs = 350;
const mapKind = ref(rawMapKind.value);
let kindTimer: number | undefined;
watch(rawMapKind, (kind) => {
  window.clearTimeout(kindTimer);
  if (kind === mapKind.value) {
    return;
  }

  kindTimer = window.setTimeout(() => {
    mapKind.value = kind;
  }, MapKindGraceMs);
});
onBeforeUnmount(() => window.clearTimeout(kindTimer));

// Sync, so `mapKind` and the screens never see a scene before the place it was opened in is noted.
watch(
  () => store.currentState?.player.mapId,
  () => {
    const player = store.currentState?.player;
    if (player !== undefined) {
      rememberPlace(player.mapId, player.previousMapId);
    }
  },
  { immediate: true, flush: "sync" },
);

// The Mapa tab picks its screen by itself. When the game moves to another kind of place (a battle
// starts, the player dives into the Seabed), a Mapa screen follows; any other tab stays put.
watch(mapKind, () => {
  if (navSectionOf(current.value) === "mapa") {
    go("mapa");
  }
});

const view = computed(() => {
  // The Mapa tab picks its screen by the place the player is in, unless a place was asked for by id
  // (the Wiki's places, the links to where something is): that is always the plain map.
  const name = current.value === "mapa" && param.value === null ? mapKind.value : current.value;
  return screens[name];
});

/**
 * Tells the core whether a map of the field is on the screen: the only thing the position and the characters of the field are
 * for, and most of what it reads.
 */
const mapOnScreen = computed(
  () =>
    waitKind.value === null &&
    [screens.mapa, screens.seabedRuta, screens.mobius].includes(view.value as Component),
);
watch(mapOnScreen, (shown) => window.DigiviceNative?.setMapOnScreen?.(shown), { immediate: true });

const inBattle = computed(() => mapKind.value === "batalla" || mapKind.value === "batallaCartas");

const isPanelOpen = ref(false);
</script>

<template>
  <div class="screen">
    <!-- No nav bar while waiting: there is nothing to go to. Holding the screen opens the app panel instead. -->
    <EsperaScreen v-if="waitKind !== null" :kind="waitKind" @longpress="isPanelOpen = true" />
    <template v-else>
      <component :is="view" />
      <!-- The search has no nav bar in the design: its keyboard takes that room. -->
      <NavBar
        v-if="current !== 'wikiBuscar'"
        @longpress="isPanelOpen = true"
        :in-battle="inBattle"
      />
    </template>
  </div>
  <AppPanel v-if="isPanelOpen" @close="isPanelOpen = false" />
</template>
