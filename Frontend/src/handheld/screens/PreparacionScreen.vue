<script setup lang="ts">
import DiscIcon from "../components/DiscIcon.vue";
// The first-run preparation wizard on the upper screen. SetupActivity pushes its state; the page only draws it.
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import digiviceUrl from "../assets/ui/digivice.svg?url";
import { setupView, type SetupAction, type SetupPage } from "../setup-view";

const props = defineProps<{ page: SetupPage }>();
const emit = defineEmits<{ action: [action: SetupAction] }>();

const { t } = useI18n();
const view = computed(() => setupView(props.page, t));

/** The Digi-Egg on the Digivice screen: it cracks in four steps and hatches when everything is ready. */
const eggRows: [number, number][] = [
  [23, 25],
  [22, 26],
  [21, 27],
  [21, 27],
  [21, 27],
  [21, 27],
  [22, 26],
  [23, 25],
];
const cracks: [number, number][][] = [
  [
    [24, 25],
    [25, 26],
  ],
  [
    [23, 27],
    [24, 28],
    [26, 26],
  ],
  [
    [22, 27],
    [26, 28],
    [25, 29],
    [27, 27],
  ],
  [
    [21, 28],
    [23, 29],
    [27, 29],
    [24, 30],
  ],
];
const crackCells = computed(() => cracks.slice(0, view.value.egg + 1).flat());

/** The panel is drawn at 640 x 360 and scaled by whole halves to the screen, like a game. */
const scale = ref(1);
function fit(): void {
  scale.value = Math.max(
    1,
    Math.floor(Math.min(window.innerWidth / 640, window.innerHeight / 360) * 2) / 2,
  );
}
onMounted(() => {
  fit();
  window.addEventListener("resize", fit);
});
onBeforeUnmount(() => window.removeEventListener("resize", fit));

/**
 * The bar of the game's player registration (src/menus/player_name/confirm.c): its counter goes up one point a frame
 * and the bar is a violet-to-magenta quad scaled to the count, so the gradient always spans what is filled. Here the
 * count runs after the real progress the same way, one point a frame, and the bar flashes once on reaching 100.
 */
const shown = ref(0);
const flash = ref(false);
let frame: number | null = null;
function tick(): void {
  frame = null;
  const target = view.value.meter?.mode === "value" ? (view.value.meter.percent ?? 0) : 0;
  if (shown.value === target) {
    return;
  }
  shown.value += shown.value < target ? 1 : -Math.min(shown.value - target, 5);
  if (shown.value === 100) {
    flash.value = true;
    window.setTimeout(() => (flash.value = false), 200);
  }
  frame = requestAnimationFrame(tick);
}
watch(
  () => (view.value.meter?.mode === "value" ? view.value.meter.percent : null),
  () => {
    frame ??= requestAnimationFrame(tick);
  },
  { immediate: true },
);
onBeforeUnmount(() => {
  if (frame !== null) cancelAnimationFrame(frame);
});

function press(): void {
  if (!view.value.button.disabled) emit("action", view.value.button.action);
}
</script>

<template>
  <div class="backdrop">
    <div class="stage" :style="{ transform: `translate(-50%, -50%) scale(${scale})` }">
      <!-- Background: circuit traces and a perspective grid floor. -->
      <div class="floor" aria-hidden="true"></div>
      <svg class="circuits" viewBox="0 0 640 360" aria-hidden="true" shape-rendering="crispEdges">
        <path d="M0 64 H50 L66 28 H190 L214 6" class="trace" />
        <rect x="170" y="24" width="7" height="7" class="node" />
        <rect x="22" y="44" width="5" height="5" class="dot" />
        <rect x="30" y="44" width="5" height="5" class="dot" />
        <rect x="38" y="44" width="5" height="5" class="dot" />
        <path d="M640 300 H608 L596 316 H540" class="trace" />
        <rect x="632" y="296" width="7" height="7" class="node" />
        <rect x="608" y="138" width="5" height="5" class="dot" />
        <rect x="616" y="138" width="5" height="5" class="dot" />
      </svg>

      <section class="panel">
        <svg class="bezel" viewBox="0 0 480 280" preserveAspectRatio="none" aria-hidden="true">
          <path
            class="bezel-outer"
            d="M10 4 H92 L98 10 H382 L388 4 H470 L476 10 V270 L470 276 H10 L4 270 V10 Z"
          />
          <path class="bezel-light" d="M12 7 H90 L96 13 H384 L390 7 H468" />
          <path class="bezel-inner" d="M14 16 H466 V264 H14 Z" />
          <path class="bezel-glow" d="M16 18 H464" />
          <circle cx="12" cy="12" r="3" class="bolt" />
          <circle cx="468" cy="12" r="3" class="bolt" />
          <circle cx="12" cy="268" r="3" class="bolt" />
          <circle cx="468" cy="268" r="3" class="bolt" />
          <path
            class="bolt-cross"
            d="M10.5 12 H13.5 M466.5 12 H469.5 M10.5 268 H13.5 M466.5 268 H469.5"
          />
        </svg>

        <div class="icon-tile">
          <svg
            viewBox="11.5 8.5 31 34.5"
            shape-rendering="crispEdges"
            class="device"
            aria-hidden="true"
          >
            <image :href="digiviceUrl" x="11" y="8" width="32" height="36" />
            <rect x="19" y="22" width="11" height="11" fill="#9ce26a" />
            <template v-if="!view.finished">
              <rect
                v-for="([from, to], row) in eggRows"
                :key="`e${row}`"
                :x="from"
                :y="24 + row"
                :width="to - from + 1"
                height="1"
                fill="#1c4a2a"
              />
              <rect
                v-for="([x, y], i) in crackCells"
                :key="`c${i}`"
                :x="x"
                :y="y"
                width="1"
                height="1"
                fill="#c9f59a"
              />
            </template>
            <template v-else>
              <rect x="21" y="28" width="2" height="2" fill="#1c4a2a" />
              <rect x="23" y="30" width="2" height="2" fill="#1c4a2a" />
              <rect x="25" y="28" width="2" height="2" fill="#1c4a2a" />
              <rect x="27" y="26" width="2" height="2" fill="#1c4a2a" />
            </template>
          </svg>
        </div>
        <div class="tab"><span>Digivice</span></div>
        <span class="stripes" aria-hidden="true"><i /><i /><i /></span>
        <span class="tab-rule" aria-hidden="true"></span>
        <h1 class="title">{{ view.title }}</h1>
        <span class="title-rule" aria-hidden="true"></span>

        <div class="source">
          <span class="source-left">
            <DiscIcon class="disc" />
            <span class="source-name">{{ view.file ?? "ISO / BIN / CHD" }}</span>
          </span>
          <span class="source-right">SLES-03936</span>
        </div>

        <p v-if="view.note" class="note" :class="{ warning: view.warning }" role="status">
          <span class="chevrons" aria-hidden="true"><i /><i /></span>
          <span class="note-text">{{ view.note }}</span>
        </p>

        <template v-if="view.step">
          <div class="step" role="status">
            <span class="chevrons" :class="{ still: !view.meter?.moving }" aria-hidden="true"
              ><i /><i
            /></span>
            <span class="step-label">{{ view.step.text }}</span>
            <span class="step-count">{{ view.step.count }}</span>
          </div>
          <span class="step-rule" aria-hidden="true"></span>
        </template>

        <div v-if="view.meter" class="meter">
          <div class="track">
            <div class="lane">
              <div v-if="view.meter.mode === 'sweep'" class="fill sweep" aria-hidden="true"></div>
              <div
                v-else-if="view.meter.mode === 'value'"
                class="fill"
                :class="{ full: flash }"
                :style="{ width: `${shown}%` }"
              ></div>
            </div>
          </div>
          <div class="percent">{{ view.meter.mode === "value" ? shown : "--" }} %</div>
        </div>

        <button type="button" class="action" :disabled="view.button.disabled" @click="press">
          <i class="corner tl" /><i class="corner tr" /><i class="corner bl" /><i
            class="corner br"
          />
          <span v-if="view.button.icon === 'pause'" class="pause" aria-hidden="true"
            ><i /><i
          /></span>
          <DiscIcon v-else-if="view.button.icon === 'disc'" class="arrow" />
          <img v-else :src="iconUrl(view.button.icon)" alt="" class="arrow" />
          {{ view.button.label }}
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 40%, #0e1c6e 0%, #070f48 55%, #040a2e 100%);
  font-family: "Press Start 2P", monospace;
  color: #ffffff;
}

.stage {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 640px;
  height: 360px;
  transform-origin: center;
  image-rendering: pixelated;
}

.floor {
  position: absolute;
  left: -160px;
  right: -160px;
  bottom: -40px;
  height: 170px;
  transform: perspective(220px) rotateX(58deg);
  transform-origin: top;
  background-image:
    linear-gradient(#1a3a9a 1px, transparent 1px),
    linear-gradient(90deg, #1a3a9a 1px, transparent 1px);
  background-size: 28px 28px;
  -webkit-mask-image: linear-gradient(transparent, #000 45%);
  mask-image: linear-gradient(transparent, #000 45%);
  opacity: 0.8;
}

.circuits {
  position: absolute;
  inset: 0;
  width: 640px;
  height: 360px;
}

.trace {
  fill: none;
  stroke: #1d4bc0;
  stroke-width: 2;
}

.node {
  fill: #2c7fe8;
  animation: pulse 2.4s steps(2) infinite;
}

.dot {
  fill: #2448b0;
}

.panel {
  position: absolute;
  left: 80px;
  top: 40px;
  width: 480px;
  height: 280px;
}

.bezel {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.bezel-outer {
  fill: #8c96cc;
  stroke: #2c3478;
  stroke-width: 2;
}

.bezel-inner {
  fill: #08124c;
  stroke: #3a54c0;
  stroke-width: 2;
}

.bezel-light {
  fill: none;
  stroke: #d8ddf8;
  stroke-width: 1;
}

.bezel-glow {
  stroke: #1d3caa;
  stroke-width: 2;
}

.bolt-cross {
  stroke: #2c3478;
  stroke-width: 1;
}

.bolt {
  fill: #5d669e;
  stroke: #d7dcf5;
  stroke-width: 1;
}

.icon-tile {
  position: absolute;
  left: 24px;
  top: 24px;
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #c9d3f2;
  border: 2px solid #8c96cc;
  border-radius: 6px;
  box-shadow: 0 2px 0 #2c3478;
}

.device {
  width: 48px;
  height: 54px;
}

.tab {
  position: absolute;
  left: 92px;
  top: 24px;
  height: 18px;
  padding: 0 26px 0 18px;
  display: flex;
  align-items: center;
  background: #1c3fae;
  clip-path: polygon(8px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.stripes {
  position: absolute;
  left: 220px;
  top: 26px;
  display: flex;
  gap: 3px;
}

.stripes i {
  width: 4px;
  height: 14px;
  background: #8d3cf0;
  transform: skewX(-30deg);
}

.tab-rule {
  position: absolute;
  left: 240px;
  right: 22px;
  top: 41px;
  height: 1px;
  background: #2c4ac0;
}

.title {
  position: absolute;
  left: 96px;
  top: 52px;
  margin: 0;
  font-size: 16px;
  font-weight: normal;
  color: #ffe833;
  text-shadow: 2px 2px 0 #3a2c00;
}

.title-rule {
  position: absolute;
  left: 96px;
  right: 22px;
  top: 78px;
  height: 2px;
  background: linear-gradient(90deg, #3fe6ff, #9b4dff);
}

.source {
  position: absolute;
  left: 24px;
  right: 22px;
  top: 92px;
  height: 22px;
  display: flex;
  font-size: 10px;
}

.source-left,
.source-right {
  display: flex;
  align-items: center;
  border: 1px solid #3a54c0;
  background: #0c1a5e;
}

.source-left {
  flex: 1;
  min-width: 0;
  gap: 8px;
  padding-left: 10px;
  color: #c8d2ff;
  clip-path: polygon(6px 0, 100% 0, calc(100% - 10px) 100%, 0 100%);
}

.source-right {
  padding: 0 10px 0 16px;
  margin-left: -6px;
  color: #6ff0ff;
  clip-path: polygon(10px 0, 100% 0, 100% 100%, 0 100%);
}

.source-name {
  min-width: 0;
  overflow: hidden;
  padding-right: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.disc {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  margin: -2px 0;
}

.step {
  position: absolute;
  left: 26px;
  right: 22px;
  top: 126px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 10px;
}

.chevrons {
  display: flex;
  flex-shrink: 0;
}

.step-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* Choosing a file or a problem: the step line wraps into the room of the bar. */
.note {
  position: absolute;
  left: 26px;
  right: 22px;
  top: 124px;
  display: flex;
  gap: 10px;
  margin: 0;
  font-size: 10px;
  line-height: 16px;
  color: #c8d2ff;
}

.note .chevrons {
  height: 16px;
  align-items: center;
}

.note-text {
  flex: 1;
  min-width: 0;
}

.warning {
  color: #ffb09a;
}

.warning .chevrons i {
  border-left-color: #ff7a52;
}

.step-count {
  color: #6ff0ff;
}

.chevrons i {
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 6px solid #3fe6ff;
  animation: blink 1s steps(2) infinite;
}

.chevrons i + i {
  animation-delay: 0.25s;
}

.chevrons.still i {
  animation: none;
}

.step-rule {
  position: absolute;
  left: 26px;
  right: 22px;
  top: 144px;
  height: 1px;
  background: #2c4ac0;
}

.meter {
  position: absolute;
  left: 52px;
  right: 40px;
  top: 158px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.track {
  flex: 1;
  height: 24px;
  padding: 4px;
  box-sizing: border-box;
  border: 2px solid #c6ccee;
  border-radius: 6px;
  background: #050a30;
  box-shadow: inset 0 0 0 2px #2c3478;
}

.lane {
  position: relative;
  height: 100%;
  overflow: hidden;
}

.fill {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: 2px;
  /* The game's own colours, left and right of its Gouraud quad. */
  background: linear-gradient(90deg, #7f32f2, #d12fde);
}

.fill.full {
  filter: brightness(1.6);
}

/* The file is being checked: a block of the bar sweeps across until the work has a size. */
.sweep {
  position: absolute;
  width: 30%;
  animation: sweep 1.6s steps(16) infinite;
  transition: none;
}

.percent {
  width: 84px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #6c78c8;
  border-radius: 6px;
  background: #0a1450;
  color: #6ff0ff;
  font-size: 16px;
  text-shadow: 2px 2px 0 #0a2a6a;
}

.action {
  position: absolute;
  left: 28px;
  right: 26px;
  top: 200px;
  height: 40px;
  display: flex;
  align-items: center;
  gap: 18px;
  padding-left: 36px;
  border: 2px solid #6fd0ff;
  background: #10309a;
  color: #ffffff;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.action:active:not(:disabled) {
  background: #1c3fae;
}

.action:disabled {
  border-color: #3a54c0;
  color: #8fb4e8;
  cursor: default;
}

.action:disabled .corner {
  border-color: #8a7420;
}

.action:disabled .pause i {
  background: #4f78a8;
}

.corner {
  position: absolute;
  width: 9px;
  height: 9px;
  border-color: #ffd52e;
  border-style: solid;
  border-width: 0;
}

.tl {
  left: 3px;
  top: 3px;
  border-left-width: 3px;
  border-top-width: 3px;
}
.tr {
  right: 3px;
  top: 3px;
  border-right-width: 3px;
  border-top-width: 3px;
}
.bl {
  left: 3px;
  bottom: 3px;
  border-left-width: 3px;
  border-bottom-width: 3px;
}
.br {
  right: 3px;
  bottom: 3px;
  border-right-width: 3px;
  border-bottom-width: 3px;
}

.pause {
  display: flex;
  gap: 4px;
}

.pause i {
  width: 7px;
  height: 20px;
  background: #7fe8ff;
  box-shadow: 1px 1px 0 #0a2a6a;
}

.arrow {
  width: 22px;
  height: 22px;
}

@keyframes pulse {
  50% {
    fill: #6ff0ff;
  }
}

@keyframes blink {
  50% {
    opacity: 0.25;
  }
}

@keyframes sweep {
  from {
    left: -30%;
  }
  to {
    left: 100%;
  }
}
</style>
