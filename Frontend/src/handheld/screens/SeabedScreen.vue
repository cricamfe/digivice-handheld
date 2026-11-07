<script setup lang="ts">
import { gameWord } from "../language";
import { useTexts } from "../texts";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import MapImage from "../components/MapImage.vue";
import { iconUrl } from "../assets";
import { serverOf } from "../map-view";
import {
  buildSeabedOverview,
  routeNumber,
  type SeabedDock,
  type SeabedRoute,
} from "../seabed-sheet";
import type { SeabedNote } from "../seabed-docks";
import { useScreen } from "../router";
import { toDevice, useCover } from "../use-cover";
import { useGameState } from "@/composables/use-game-state";
import { DigimonRepository } from "@/repositories/digimon.repository";

const { t } = useI18n();
const texts = useTexts();
const { go, back } = useScreen();
const gameState = useGameState();

// The routes are the same on both servers; the names of the docks are the server's own.
const server = computed(() => serverOf(gameState.value));
const overview = computed(() => buildSeabedOverview(texts, server.value));
/** Not the map of a place: the routes of the Seabed, over the world map of the server they go through. */
const serverName = computed(() =>
  t("handheld.seabed.routesOn", { server: t(`handheld.seabed.${overview.value.server}`) }),
);

const picked = ref<string | null>(null);
/** The route the player picked, else the one the player is on, else the first. */
const selected = computed(() => {
  const { seabedRoute } = gameState.value.player;
  const wanted = picked.value ?? (seabedRoute === null ? "" : String(routeNumber(seabedRoute)));
  return (
    overview.value.routes.find((route) => route.id === wanted) ?? overview.value.routes[0] ?? null
  );
});

/** The docks of the picked route that have a point on the picture: the others are only in the list below. */
type PlacedDock = SeabedDock & { x: number; y: number };
function isPlaced(dock: SeabedDock): dock is PlacedDock {
  return dock.x !== null && dock.y !== null;
}
const placedDocks = computed(() => selected.value?.docks.filter(isPlaced) ?? []);

const stage = ref<HTMLElement | null>(null);
const { onLoad, place } = useCover(stage, () => overview.value.imageUrl);

/** A pixel point of the stage for a point of the picture, or null until the picture has loaded. */
function spotOf(x: number, y: number): { x: number; y: number } | null {
  const spot = place(x, y);
  return spot.left.endsWith("px")
    ? { x: Number.parseFloat(spot.left), y: Number.parseFloat(spot.top) }
    : null;
}

/** Pixels between two dots of the line, measured on the screen so the spacing is the same on every slope. */
const DotGap = 7;
/** The dot is 4 px, as whole device pixels. */
const DotSize = 4;

/**
 * The dots of a route, as left/top of their boxes. They are placed along each piece of the line at an even distance in
 * screen pixels, and every one sits on a whole device pixel, so a diagonal is a regular staircase and no dot is blurred
 * between two pixels. The dots two pieces share are drawn once.
 */
function dotsOf(route: SeabedRoute | null): { left: number; top: number }[] {
  const seen = new Set<string>();
  const half = toDevice(DotSize) / 2;
  const result: { left: number; top: number }[] = [];
  for (const { from, to } of route?.segments ?? []) {
    const start = spotOf(from.x, from.y);
    const end = spotOf(to.x, to.y);
    if (start === null || end === null) {
      continue;
    }

    const steps = Math.max(1, Math.round(Math.hypot(end.x - start.x, end.y - start.y) / DotGap));
    for (let step = 0; step <= steps; step++) {
      const left = toDevice(start.x + ((end.x - start.x) * step) / steps - half);
      const top = toDevice(start.y + ((end.y - start.y) * step) / steps - half);
      const key = `${left}:${top}`;
      if (!seen.has(key)) {
        seen.add(key);
        result.push({ left, top });
      }
    }
  }

  return result;
}

const dots = computed(() => dotsOf(selected.value));
/** All the routes are on the map; the picked one is the one in gold, over them, with its docks. */
const otherDots = computed(() =>
  overview.value.routes.filter((route) => route !== selected.value).flatMap(dotsOf),
);

/** The picked route has nothing on the picture: its docks are only in the list below. */
const hasNoPoints = computed(
  () => selected.value !== null && dots.value.length === 0 && placedDocks.value.length === 0,
);

const noteIcons: Record<SeabedNote["kind"], string> = {
  chest: "chest",
  forge: "power",
  deadEnd: "cross",
};

/** The sentence of a note. */
function noteText(note: SeabedNote): string {
  switch (note.kind) {
    case "chest":
      return t("handheld.seabed.note.chest", { item: gameWord(note.item) });
    case "forge":
      return t("handheld.seabed.note.forge", {
        ...note,
        partner: DigimonRepository.getNameById(note.partner),
        smith: gameWord(note.smith),
        from: gameWord(note.from),
        with: gameWord(note.with),
        to: gameWord(note.to),
      });
    case "deadEnd":
      return t("handheld.seabed.note.deadEnd");
  }
}

/** Where a route's warning goes on the map: the middle of its line, or its only dock. */
function warningSpot(route: SeabedRoute): { x: number; y: number } | null {
  const pieces = route.segments;
  const middle = pieces[Math.floor(pieces.length / 2)];
  if (middle !== undefined) {
    return { x: (middle.from.x + middle.to.x) / 2, y: (middle.from.y + middle.to.y) / 2 };
  }

  return route.docks.find(isPlaced) ?? null;
}

/** The routes that have something to warn of, with where the warning goes. */
const warnings = computed(() =>
  overview.value.routes.flatMap((route) => {
    const spot = route.notes.length > 0 ? warningSpot(route) : null;
    return spot === null ? [] : [{ route, x: spot.x, y: spot.y }];
  }),
);

function at(x: number, y: number, half: number): Record<string, string> {
  const spot = place(x, y);
  return { left: `calc(${spot.left} - ${half}px)`, top: `calc(${spot.top} - ${half}px)` };
}
</script>

<template>
  <div class="view">
    <section class="seabed">
      <div ref="stage" class="win stage">
        <MapImage :src="overview.imageUrl" :alt="serverName" @load="onLoad" />

        <span
          v-for="(dot, index) in otherDots"
          :key="`other-${index}`"
          class="dot other"
          :style="{ left: `${dot.left}px`, top: `${dot.top}px` }"
          aria-hidden="true"
        ></span>
        <span
          v-for="(dot, index) in dots"
          :key="index"
          class="dot"
          :style="{ left: `${dot.left}px`, top: `${dot.top}px` }"
          aria-hidden="true"
        ></span>

        <button
          v-for="warning in warnings"
          :key="warning.route.id"
          type="button"
          class="warning"
          :style="at(warning.x, warning.y, 11)"
          :aria-label="t('handheld.seabed.route', { number: warning.route.number })"
          @click="picked = warning.route.id"
        >
          <img :src="iconUrl('alert')" alt="" class="icon" />
        </button>

        <template v-if="selected">
          <button
            v-for="dock in placedDocks"
            :key="dock.key"
            type="button"
            class="dock"
            :style="at(dock.x, dock.y, 16)"
            :disabled="!dock.isOpen"
            :aria-label="dock.name"
            @click="go('seabedRuta', dock.key)"
          >
            <img :src="iconUrl(dock.isOpen ? 'seabed' : 'cross')" alt="" class="icon" />
          </button>
        </template>

        <div class="thin-plate gold asuka">{{ serverName }}</div>
        <button type="button" class="btn back" :aria-label="t('handheld.back')" @click="back()">
          <img :src="iconUrl('back')" alt="" class="icon" />
        </button>
        <div v-if="hasNoPoints && overview.imageUrl" class="thin-plate note">
          {{ t("handheld.seabed.noPoints") }}
        </div>
      </div>

      <div class="routes">
        <button
          v-for="route in overview.routes"
          :key="route.id"
          type="button"
          class="route"
          :class="route === selected ? 'slot-on' : 'slot'"
          :aria-pressed="route === selected"
          @click="picked = route.id"
        >
          {{ t("handheld.seabed.route", { number: route.number }) }}
          <img
            v-if="route.notes.length > 0"
            :src="iconUrl('alert')"
            alt=""
            class="icon route-alert"
          />
        </button>
      </div>
    </section>

    <section class="win scroll docks">
      <template v-if="selected">
        <div v-for="(note, index) in selected.notes" :key="index" class="thin-plate note-row">
          <img :src="iconUrl(noteIcons[note.kind])" alt="" class="icon" />
          <span>{{ noteText(note) }}</span>
        </div>
        <button
          v-for="dock in selected.docks"
          :key="dock.key"
          type="button"
          class="thin dock-row"
          :disabled="!dock.isOpen"
          @click="go('seabedRuta', dock.key)"
        >
          <img :src="iconUrl(dock.isOpen ? 'seabed' : 'cross')" alt="" class="icon" />
          <span class="drop dock-name" :class="{ dim: !dock.isOpen }">{{ dock.name }}</span>
        </button>
      </template>
    </section>
  </div>
</template>

<style scoped>
.seabed {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.stage {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.back {
  position: absolute;
  right: 4px;
  top: 4px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning {
  position: absolute;
  width: 22px;
  height: 22px;
  padding: 0;
  background: none;
  border: 0;
}

.note-row {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  font-size: 10px;
  line-height: 1.4;
}

/* The line of the picked route: gold, with a hard dark outline, on a whole device pixel grid (see `dots`). */
.dot {
  position: absolute;
  width: 4px;
  height: 4px;
  background: #ffd52e;
  box-shadow: 0 0 0 1px #040716;
}

.dot.other {
  background: #7fe8ff;
}

.dock {
  position: absolute;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.asuka {
  position: absolute;
  left: 2px;
  top: 2px;
  font-size: 10px;
}

.note {
  position: absolute;
  left: 50%;
  bottom: 4px;
  transform: translateX(-50%);
  font-size: 10px;
  white-space: nowrap;
}

.routes {
  width: 124px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.route {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 10px;
  white-space: nowrap;
}

.docks {
  height: 96px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: min-content;
  align-content: start;
  gap: 3px;
  overflow-y: auto;
  font-size: 10px;
}

.dock-row {
  min-width: 0;
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 5px;
  text-align: left;
}

.dock-name {
  white-space: nowrap;
  overflow: hidden;
}
</style>
