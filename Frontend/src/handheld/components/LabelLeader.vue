<script setup lang="ts">
import { computed } from "vue";
import { toDevice } from "../use-cover";

const props = defineProps<{
  /** The 2 px squares of the line (top-left corners), relative to the point it hangs from; see `leaderDots`. */
  dots: readonly { x: number; y: number }[];
  /** The colour of the plate or letters of the name it leads to. */
  color: string;
}>();

/** A dot is 2 px, and one pixel of dark under it keeps it legible over the picture, as the names' outline does. */
const Dot = 2;

function path(offset: number): string {
  const size = toDevice(Dot);
  return props.dots
    .map(
      (dot) => `M${toDevice(dot.x + offset)} ${toDevice(dot.y + offset)}h${size}v${size}h${-size}z`,
    )
    .join("");
}

const shadow = computed(() => path(1));
const lit = computed(() => path(0));
</script>

<template>
  <svg class="leader" aria-hidden="true" shape-rendering="crispEdges">
    <path :d="shadow" fill="#040716" />
    <path :d="lit" :fill="color" />
  </svg>
</template>

<style scoped>
/* From the point it hangs from; the dots lie anywhere around it, so the box is nothing and nothing is cut. */
.leader {
  position: absolute;
  left: 0;
  top: 0;
  width: 1px;
  height: 1px;
  overflow: visible;
  pointer-events: none;
}
</style>
