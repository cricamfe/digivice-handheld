<script setup lang="ts">
import { placeOf, serverOf } from "../map-view";
import { useTexts } from "../texts";
import { placeWord } from "../place-words";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import MapImage from "../components/MapImage.vue";
import { compassUrl, genericNpcUrl, iconUrl, tileUrl } from "../assets";
import {
  buildMobiusGrid,
  buildMobiusLegend,
  buildMobiusZone,
  type MobiusCell,
  type MobiusExit,
} from "../mobius-sheet";
import { useScreen } from "../router";
import { useCover } from "../use-cover";
import { useFollow, type Point } from "../use-follow";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const gameState = useGameState();
const { current, back } = useScreen();

const texts = useTexts();

// The two servers have the same desert with other characters, and a picture each.
const server = computed(() => serverOf(gameState.value));
const grid = computed(() =>
  buildMobiusGrid(
    placeOf(gameState.value.player.mapId),
    gameState.value.player.mapVariant,
    texts,
    server.value,
  ),
);
const legend = buildMobiusLegend(texts);

/** The area picked on the grid; until one is picked, the one the player is in. */
const picked = ref<string | null>(null);
const selected = computed(() => picked.value ?? grid.value.playerLabel);
const zone = computed(() =>
  selected.value === null ? null : buildMobiusZone(selected.value, texts, server.value),
);

const details = ref<HTMLElement | null>(null);
const { onLoad, place, placePixels } = useCover(details, () => zone.value?.imageUrl ?? null);

/** Whether the zone on show is the one the player is in. */
const isPlayerZone = computed(
  () => selected.value !== null && selected.value === grid.value.playerLabel,
);

/**
 * Where the player stands on the picture of their own zone: the game gives pixels of the picture (1152 x 1024, which
 * the pictures of both servers have). The core sends (0, 0) when the game has no player on the field.
 */
const playerSpot = computed((): Point | null => {
  const { positionX, positionY } = gameState.value.player;
  if (!isPlayerZone.value || (positionX === 0 && positionY === 0)) {
    return null;
  }

  const spot = placePixels(positionX, positionY);
  return spot === null ? null : { x: parseFloat(spot.left), y: parseFloat(spot.top) };
});

// Moving to another zone is a new picture of the same size, so the dot starts over there instead of sliding across.
const playerPoint = useFollow(
  () => playerSpot.value,
  () => `${zone.value?.imageUrl}-${grid.value.playerLabel}`,
);

const agentStyle = computed(() => {
  const agent = zone.value?.agent;
  if (!agent) {
    return null;
  }

  const spot = place(agent.x, agent.y);
  // The sprite stands on the point: its feet are at the coordinates, and the picture sits as its frame says.
  const { left, top, width, height } = agent.picture;
  return {
    left: `calc(${spot.left} + ${left}px)`,
    top: `calc(${spot.top} + ${top}px)`,
    width: `${width}px`,
    height: `${height}px`,
  };
});

function pick(cell: MobiusCell): void {
  if (cell.isPlayable) {
    picked.value = cell.label;
  }
}

function follow(exit: MobiusExit): void {
  if (exit.area !== null) {
    picked.value = exit.area;
  }
}

function isSelected(cell: MobiusCell): boolean {
  return cell.isPlayable && cell.label === selected.value;
}
</script>

<template>
  <div class="view">
    <section class="mobius">
      <div class="left">
        <div class="win grid">
          <template v-for="cell in grid.cells" :key="cell.key">
            <component
              :is="cell.isPlayable ? 'button' : 'div'"
              class="cell"
              :type="cell.isPlayable ? 'button' : undefined"
              :aria-label="cell.name"
              :aria-pressed="cell.isPlayable ? isSelected(cell) : undefined"
              @click="pick(cell)"
            >
              <img :src="tileUrl(cell.tile)" alt="" class="tile" />
              <span class="marks">
                <img
                  v-if="cell.isPlayer"
                  :src="iconUrl('place')"
                  :alt="t('handheld.mobius.here')"
                  class="icon"
                />
                <img
                  v-if="cell.note"
                  :src="cell.sprite ?? genericNpcUrl()"
                  :alt="cell.note"
                  class="agent"
                />
              </span>
              <span
                class="outline cell-label"
                :class="{ foot: cell.labelAtFoot, dim: cell.isDim }"
                >{{ cell.label }}</span
              >
              <img v-if="isSelected(cell)" :src="tileUrl('cursor')" alt="" class="tile" />
            </component>
          </template>
        </div>

        <div class="win-soft legend">
          <span v-for="entry in legend" :key="entry.tile" class="legend-entry">
            <img :src="tileUrl(entry.tile)" alt="" class="legend-tile" />{{ entry.label }}
          </span>
        </div>
      </div>

      <div class="right">
        <!-- Opened from the Wiki's guides: there is no tab of the bar that brings the player back. -->
        <button
          v-if="current === 'wikiMobius'"
          type="button"
          class="btn back"
          :aria-label="t('handheld.back')"
          @click="back()"
        >
          <img :src="iconUrl('back')" alt="" class="icon" />
        </button>
        <div class="plate title">
          <img :src="iconUrl('desert')" alt="" class="icon" />
          <span class="gold title-text">{{
            zone
              ? t("handheld.mobius.zone", { zone: zone.label })
              : placeWord(texts, "mobiusDesert")
          }}</span>
        </div>

        <div class="win compass-frame">
          <template v-if="zone">
            <div class="compass">
              <img :src="compassUrl()" alt="" class="dial" />
              <span class="gold dial-label">{{ zone.label }}</span>
            </div>
            <button
              type="button"
              class="exit north"
              :disabled="zone.north.area === null"
              :aria-label="t('handheld.mobius.north', { area: zone.north.label })"
              @click="follow(zone.north)"
            >
              <span class="gold">N</span><span class="drop exit-name">{{ zone.north.label }}</span>
            </button>
            <button
              type="button"
              class="exit south"
              :disabled="zone.south.area === null"
              :aria-label="t('handheld.mobius.south', { area: zone.south.label })"
              @click="follow(zone.south)"
            >
              <span class="gold">S</span><span class="drop exit-name">{{ zone.south.label }}</span>
            </button>
            <button
              type="button"
              class="exit west"
              :disabled="zone.west.area === null"
              :aria-label="t('handheld.mobius.west', { area: zone.west.label })"
              @click="follow(zone.west)"
            >
              <span class="gold">O</span><span class="drop exit-name">{{ zone.west.label }}</span>
            </button>
            <button
              type="button"
              class="exit east"
              :disabled="zone.east.area === null"
              :aria-label="t('handheld.mobius.east', { area: zone.east.label })"
              @click="follow(zone.east)"
            >
              <span class="gold">E</span><span class="drop exit-name">{{ zone.east.label }}</span>
            </button>
          </template>
          <p v-else class="hint dim">{{ t("handheld.mobius.hint") }}</p>
        </div>

        <div ref="details" class="win picture">
          <template v-if="zone">
            <MapImage
              :src="zone.imageUrl"
              :alt="t('handheld.mobius.map', { place: placeWord(texts, 'mobiusDesert') })"
              @load="onLoad"
            />
            <div v-if="agentStyle" class="pinned" :style="agentStyle">
              <img :src="zone.agent?.picture.url" :alt="zone.agent?.note ?? ''" class="pin" />
              <span v-if="zone.agent?.note" class="thin-plate gold pin-note">{{
                zone.agent.note
              }}</span>
            </div>
            <!-- After the character, so the dot is drawn over it. A new picture is a new dot. -->
            <img
              v-if="playerPoint"
              :key="zone.imageUrl ?? ''"
              :src="iconUrl('here')"
              :alt="t('handheld.mobius.here')"
              class="player"
              :style="{
                transform: `translate3d(${playerPoint.x - 11}px, ${playerPoint.y - 11}px, 0)`,
              }"
            />
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mobius {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.left {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

/* Six cells of 54 px fill the 324 px inside the frame, so the grid has no padding. */
.grid {
  height: 340px;
  flex-shrink: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(6, 54px);
  grid-template-rows: repeat(6, 54px);
}

.cell {
  position: relative;
  width: 54px;
  height: 54px;
  overflow: hidden;
}

.tile {
  position: absolute;
  inset: 0;
  width: 54px;
  height: 54px;
}

.marks {
  position: absolute;
  left: 0;
  right: 0;
  top: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 1px;
}

.agent {
  width: 19px;
  height: 34px;
  flex-shrink: 0;
  object-fit: contain;
}

.cell-label {
  position: absolute;
  left: 0;
  right: 0;
  top: 23px;
  text-align: center;
  font-size: 10px;
}

.cell-label.foot {
  top: auto;
  bottom: 4px;
}

.legend {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 6px;
  align-content: center;
  justify-items: start;
  font-size: 10px;
}

/* "Noise Desert S" is 14 letters and the line next to the tile holds 11: the game's name takes two lines, whole words. */
.legend-entry {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 11px;
}

.legend-tile {
  width: 27px;
  height: 27px;
  flex-shrink: 0;
}

.back {
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.title-text {
  font-size: 12px;
  white-space: nowrap;
}

.compass-frame {
  position: relative;
  height: 192px;
  flex-shrink: 0;
}

.compass {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 132px;
  height: 132px;
  margin: -66px 0 0 -66px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dial {
  position: absolute;
  inset: 0;
  width: 132px;
  height: 132px;
}

.dial-label {
  position: relative;
  font-size: 16px;
}

.exit {
  position: absolute;
  display: flex;
}

.exit-name {
  font-size: 12px;
}

.north,
.south {
  left: 50%;
  width: 96px;
  height: 44px;
  margin-left: -48px;
  justify-content: center;
  gap: 8px;
}

.north {
  top: 0;
  align-items: flex-start;
  padding-top: 2px;
}

.south {
  bottom: 0;
  align-items: flex-end;
  padding-bottom: 2px;
}

.west,
.east {
  top: 50%;
  width: 54px;
  height: 56px;
  margin-top: -28px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.west {
  left: 0;
}

.east {
  right: 0;
}

.hint {
  position: absolute;
  inset: 0;
  margin: 0;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1.6;
}

.picture {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* Taps go through to the picture. `useFollow` moves it. */
.player {
  position: absolute;
  left: 0;
  top: 0;
  width: 22px;
  height: 22px;
  pointer-events: none;
}

/* Its size is the picture's, at twice the game's, so the pixels stay square. */
.pinned {
  position: absolute;
}

.pin {
  display: block;
  width: 100%;
  height: 100%;
}

.pin-note {
  position: absolute;
  left: 50%;
  top: 100%;
  transform: translateX(-50%);
  font-size: 10px;
  white-space: nowrap;
}
</style>
