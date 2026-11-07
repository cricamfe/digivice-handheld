<script setup lang="ts">
import { computed } from "vue";
import { leaderDots } from "../actor-labels";
import { toDevice } from "../use-cover";
import LabelLeader from "./LabelLeader.vue";
import { useFollow, type Point } from "../use-follow";

const props = defineProps<{
  /** Where its feet are, in pixels of the layer of the map; null while the point is outside the picture. */
  spot: Point | null;
  /** The picture changes (another place): it starts on its spot instead of gliding there. */
  snap: string;
  src: string;
  /** Where the picture sits against the point, and its size, in pixels: `[x, y, width, height]`. */
  frame: readonly [number, number, number, number];
  name: string | null;
  /** What it is for: the colour of its name. */
  role: string;
  /** How far the name moves from under the feet so it does not lie over another name or character (see `nudgeLabels`). */
  labelShift?: { x: number; y: number } | null;
  /** The icon over its head when it offers a quest. */
  badge?: string | null;
  /** Taps open something; otherwise taps go through to what is below. */
  tappable: boolean;
}>();

const emit = defineEmits<{ open: [] }>();

/** The readings come with the full cycle, every 500 ms. */
const DelayMs = 600;
/** There are many, and a walking character on a side of the map is not worth redrawing it as often as the player. */
const FramesPerSecond = 15;
const point = useFollow(
  () => props.spot,
  () => props.snap,
  DelayMs,
  FramesPerSecond,
);

/** The line that ties the name to the character when the name was moved away from it (nothing when it sits next to it). */
const leader = computed(() =>
  props.name !== null && props.labelShift
    ? leaderDots({ key: "", x: 0, y: 0, frame: props.frame, name: props.name }, props.labelShift)
    : null,
);
</script>

<template>
  <component
    :is="tappable ? 'button' : 'div'"
    v-if="point"
    :type="tappable ? 'button' : undefined"
    class="actor"
    :class="{ tappable }"
    :aria-label="tappable && name !== null ? name : undefined"
    :style="{
      // Whole device pixels: a sprite between two of them is resampled, and shimmers as it moves.
      transform: `translate3d(${toDevice(point.x)}px, ${toDevice(point.y)}px, 0)`,
      zIndex: Math.round(point.y),
    }"
    @click="tappable ? emit('open') : undefined"
  >
    <img
      :src="src"
      alt=""
      class="actor-sprite"
      :style="{
        left: `${frame[0]}px`,
        top: `${frame[1]}px`,
        width: `${frame[2]}px`,
        height: `${frame[3]}px`,
      }"
    />
    <span v-if="badge" class="actor-badge thin-plate" :style="{ top: `${frame[1] - 34}px` }">
      <img :src="badge" alt="" />
    </span>
    <LabelLeader v-if="leader" :dots="leader" color="#7fe8ff" />
    <span
      v-if="name !== null"
      class="actor-label"
      :class="`role-${role}`"
      :style="
        labelShift
          ? { transform: `translate(calc(-50% + ${labelShift.x}px), ${labelShift.y}px)` }
          : undefined
      "
      >{{ name }}</span
    >
  </component>
</template>

<style scoped>
/* The box is the point itself: the picture and the label hang from it. */
.actor {
  position: absolute;
  left: 0;
  top: 0;
  width: 0;
  height: 0;
  padding: 0;
  pointer-events: none;
}

.actor.tappable {
  pointer-events: auto;
}

.actor-sprite {
  position: absolute;
  display: block;
  max-width: none;
}

/* A plate over the head, so its icon reads over any part of the picture. */
.actor-badge {
  position: absolute;
  left: 0;
  transform: translateX(-50%);
  display: block;
  line-height: 0;
}

.actor-badge img {
  width: 22px;
  height: 22px;
  max-width: none;
}

/* Plain letters with a hard outline, not a plate: a dozen plates over the picture would hide it. */
.actor-label {
  position: absolute;
  left: 0;
  top: 2px;
  transform: translateX(-50%);
  font-size: 10px;
  line-height: 1;
  white-space: nowrap;
  color: #7fe8ff;
  text-shadow:
    1px 0 0 #000e3f,
    -1px 0 0 #000e3f,
    0 1px 0 #000e3f,
    0 -1px 0 #000e3f,
    1px 1px 0 #000e3f,
    -1px -1px 0 #000e3f,
    1px -1px 0 #000e3f,
    -1px 1px 0 #000e3f;
}
</style>
