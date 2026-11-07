<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import { useLongPress } from "../use-long-press";
import { navSectionOf, navSections, navTargets, useScreen, type NavSection } from "../router";

const props = defineProps<{
  /** A battle is on: the Mapa button shows it, and is what leads back to it. */
  inBattle?: boolean;
}>();

const emit = defineEmits<{ longpress: [] }>();

const { t } = useI18n();
const { current, go } = useScreen();

const icons: Record<NavSection, string> = {
  equipo: "party",
  diario: "journal",
  mapa: "map",
  evoluciones: "digievolution",
  wiki: "wiki",
};

const active = computed(() => navSectionOf(current.value));

function isBattleButton(section: NavSection): boolean {
  return section === "mapa" && props.inBattle === true;
}

// Holding a button opens the app panel instead of navigating; the click that ends the hold navigates nowhere.
const { onPointerDown, onPointerMove, cancel, consumeHeld } = useLongPress(() => emit("longpress"));

function onClick(section: NavSection): void {
  if (consumeHeld()) {
    return;
  }

  go(navTargets[section]);
}
</script>

<template>
  <nav class="nav">
    <button
      v-for="section in navSections"
      :key="section"
      type="button"
      class="nav-button"
      :class="[
        section === active ? 'btn-on' : 'btn',
        { 'battle-alert': isBattleButton(section) && section !== active },
      ]"
      :aria-current="section === active ? 'page' : undefined"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="cancel"
      @pointercancel="cancel"
      @pointerleave="cancel"
      @contextmenu.prevent
      @click="onClick(section)"
    >
      <img :src="iconUrl(isBattleButton(section) ? 'battle' : icons[section])" alt="" />
      <!-- The Mapa button holds both its labels, one hidden, so a battle starting does not move the bar. -->
      <span v-if="section === 'mapa'" class="label">
        <span :class="{ off: isBattleButton(section) }">{{ t("handheld.nav.mapa") }}</span>
        <span :class="{ off: !isBattleButton(section) }">{{ t("handheld.nav.battle") }}</span>
      </span>
      <span v-else>{{ t(`handheld.nav.${section}`) }}</span>
    </button>
  </nav>
</template>

<style scoped>
/*
 * Each column is as wide as its label in the language on show, and the room left is shared out evenly, so no text or
 * icon ends up against the frame ("Journal", "Squadra" and "Combate" do not fit columns fixed for the Spanish labels).
 * The longest set (Spanish, French: 586 px) fits the 592 px the five buttons have, with the 4 px padding of each.
 */
.nav {
  grid-template-columns: repeat(5, auto);
}

/* A battle is on and the player is on another tab. */
.battle-alert {
  animation: battle-alert 1s steps(1) infinite;
}

.label {
  display: grid;
  justify-items: center;
}

.label > * {
  grid-area: 1 / 1;
}

.off {
  visibility: hidden;
}

@keyframes battle-alert {
  50% {
    filter: brightness(1.5);
  }
}

.nav-button {
  padding: 0 4px;
  gap: 4px;
  /* The hold is read from pointer events, so the browser must not start a pan, zoom or selection. */
  touch-action: none;
}
</style>
