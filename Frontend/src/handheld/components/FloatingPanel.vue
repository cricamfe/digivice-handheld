<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, onUpdated, ref, watch } from "vue";
import { iconUrl } from "../assets";

/** The box of the thing the panel is about, in the host's own pixels. */
export interface PanelAnchor {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

const props = defineProps<{
  anchor: PanelAnchor;
  /** Size of the host the panel floats in (the panel never leaves it). */
  hostWidth: number;
  hostHeight: number;
  label: string;
  /** The header band: the kit icon (22 px) and the title in gold, with a smaller line under it. No title, no band. */
  title?: string;
  subtitle?: string;
  icon?: string;
  /** Kit frame of the band: the thin plate when not given, or the frame of an element (`frame-fire`). */
  frame?: string;
  /** Width of the panel, 330 px when not given. */
  width?: number;
  /** Beside the thing (right, left, below, above; the first that leaves it uncovered) instead of above or below. */
  beside?: boolean;
}>();

const emit = defineEmits<{ close: [] }>();

const Gap = 4;
const DefaultWidth = 330;

const panel = ref<HTMLElement | null>(null);
const position = ref({ top: 0, left: 0 });
const placed = ref(false);

interface Spot {
  top: number;
  left: number;
}

/**
 * Next to the thing it is about and never over it when there is room: above first, else below, else on the side with
 * more room (then it may cover part of it). With `beside`: on its right, else its left, else below, else above; when
 * none leaves it uncovered, the one that covers least. It is measured once drawn, and drawn hidden until then, so it
 * never shows in a wrong place.
 */
function place(): void {
  const element = panel.value;
  if (element === null) {
    return;
  }

  const width = element.offsetWidth;
  const height = element.offsetHeight;
  const { anchor, hostWidth, hostHeight } = props;
  const clampLeft = (value: number) => Math.max(Gap, Math.min(value, hostWidth - width - Gap));
  const clampTop = (value: number) => Math.max(Gap, Math.min(value, hostHeight - height - Gap));
  const centre = (anchor.left + anchor.right) / 2;
  const left = clampLeft(centre - width / 2);

  if (props.beside === true) {
    const spots: Spot[] = [
      { top: clampTop(anchor.top), left: clampLeft(anchor.right + Gap) },
      { top: clampTop(anchor.top), left: clampLeft(anchor.left - Gap - width) },
      { top: clampTop(anchor.bottom + Gap), left },
      { top: clampTop(anchor.top - Gap - height), left },
    ];
    const covered = (spot: Spot) => {
      const across = Math.min(spot.left + width, anchor.right) - Math.max(spot.left, anchor.left);
      const down = Math.min(spot.top + height, anchor.bottom) - Math.max(spot.top, anchor.top);
      return Math.max(0, across) * Math.max(0, down);
    };
    // The first spot of the order wins a tie, so a free right side beats a free left one.
    const best = spots.reduce((chosen, spot) => (covered(spot) < covered(chosen) ? spot : chosen));
    settle(best.top, best.left);
    return;
  }

  const above = anchor.top - Gap - height;
  const below = anchor.bottom + Gap;
  let top: number;
  if (above >= Gap) {
    top = above;
  } else if (below + height <= hostHeight - Gap) {
    top = below;
  } else {
    top = anchor.top - Gap > hostHeight - anchor.bottom - Gap ? Gap : hostHeight - height - Gap;
  }

  settle(Math.max(Gap, top), left);
}

/** Keeps the spot as it is when it did not move: `onUpdated` runs this after every draw, and a new object would draw again. */
function settle(top: number, left: number): void {
  if (position.value.top !== top || position.value.left !== left) {
    position.value = { top, left };
  }
  placed.value = true;
}

/** A tap outside the panel closes it; the thing it is about toggles it by itself (it carries `data-panel-anchor`). */
function onOutside(event: PointerEvent): void {
  const target = event.target;
  if (!(target instanceof Node)) {
    return;
  }

  const anchored = target instanceof Element && target.closest("[data-panel-anchor]") !== null;
  if (!anchored && panel.value !== null && !panel.value.contains(target)) {
    emit("close");
  }
}

function onKey(event: KeyboardEvent): void {
  if (event.key === "Escape") {
    emit("close");
  }
}

onMounted(() => {
  void nextTick(place);
  document.addEventListener("pointerdown", onOutside, true);
  document.addEventListener("keydown", onKey);
});

onUpdated(place);
watch(
  () => props.anchor,
  () => void nextTick(place),
);

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onOutside, true);
  document.removeEventListener("keydown", onKey);
});
</script>

<template>
  <div
    ref="panel"
    class="win floating"
    role="dialog"
    :aria-label="label"
    :style="{
      top: `${position.top}px`,
      left: `${position.left}px`,
      width: `${Math.min(width ?? DefaultWidth, hostWidth - Gap * 2)}px`,
      maxHeight: `${hostHeight - Gap * 2}px`,
      visibility: placed ? 'visible' : 'hidden',
    }"
  >
    <slot name="head">
      <div v-if="title !== undefined" class="panel-head" :class="frame ?? 'thin-plate'">
        <img v-if="icon" :src="iconUrl(icon)" alt="" class="panel-icon" />
        <span class="panel-head-text">
          <span class="gold panel-title">{{ title }}</span>
          <span v-if="subtitle" class="panel-sub">{{ subtitle }}</span>
        </span>
      </div>
    </slot>
    <div class="scroll panel-body"><slot /></div>
    <div v-if="$slots.footer" class="panel-foot"><slot name="footer" /></div>
  </div>
</template>

<style scoped>
/* A kit frame with its opaque fill, drawn over the screen without a backdrop and without a transition. */
.floating {
  position: absolute;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
  line-height: 1.4;
}
</style>
