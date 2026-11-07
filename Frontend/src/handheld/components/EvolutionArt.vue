<script setup lang="ts">
import { computed, ref } from "vue";
import { evolutionPortraits } from "../evolution-portraits";

const props = defineProps<{ src: string; evolutionId: number }>();

const mask = ref<HTMLCanvasElement | null>(null);
const framing = computed(() => {
  const portrait = evolutionPortraits[props.evolutionId];
  if (portrait === undefined) {
    return undefined;
  }

  const [x, y, height] = portrait;
  return {
    "--head-x": `-${x}%`,
    "--head-y": `-${y}%`,
    // Keep a tight bust crop and leave room below the head for the shoulders.
    "--model-height": `${8000 / height}%`,
  };
});

function drawShadow(event: Event): void {
  const image = event.currentTarget as HTMLImageElement;
  const canvas = mask.value;
  if (canvas === null || image.naturalWidth === 0 || image.naturalHeight === 0) {
    return;
  }

  const sourceSide = Math.max(image.naturalWidth, image.naturalHeight);
  const displayedSide = Math.max(image.offsetWidth, image.offsetHeight);
  // Keep the shadow's pixels about 2 px wide as the bust magnification varies.
  const maskSide = framing.value ? Math.min(sourceSide, Math.round(displayedSide / 2)) : 128;
  const scale = maskSide / sourceSide;
  canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
  canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
  const context = canvas.getContext("2d");
  if (context === null) {
    return;
  }

  context.drawImage(image, 0, 0, canvas.width, canvas.height);
  const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
  for (let offset = 0; offset < pixels.data.length; offset += 4) {
    pixels.data[offset] = 2;
    pixels.data[offset + 1] = 5;
    pixels.data[offset + 2] = 18;
    pixels.data[offset + 3] = pixels.data[offset + 3]! >= 64 ? 255 : 0;
  }
  context.putImageData(pixels, 0, 0);
}
</script>

<template>
  <span class="evolution-art">
    <span class="model" :class="{ framed: framing }" :style="framing">
      <canvas ref="mask" class="shadow" width="1" height="1" aria-hidden="true" />
      <img :src="src" alt="" @load="drawShadow" />
    </span>
  </span>
</template>

<style scoped>
.evolution-art {
  position: relative;
  display: block;
}

.evolution-art::before {
  content: "";
  position: absolute;
  inset: -16px 0 0;
  background:
    repeating-conic-gradient(#88b1ef15 0% 25%, transparent 0% 50%) 0 0 / 4px 4px,
    #315b9a26;
  mask-image: radial-gradient(ellipse 48% 67% at 50% 43%, #000, transparent);
  pointer-events: none;
}

.model {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  transform: translateY(-48px) scale(3);
  transform-origin: center top;
}

.model.framed {
  position: absolute;
  top: 22%;
  left: 50%;
  width: max-content;
  height: var(--model-height);
  transform: translate(var(--head-x), var(--head-y));
}

.shadow,
img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center top;
}

.shadow {
  position: absolute;
  inset: 0;
  image-rendering: pixelated;
  transform: translate(1px, 1px);
  opacity: 0.68;
  pointer-events: none;
}

img {
  position: relative;
  display: block;
  image-rendering: auto;
}

.framed img {
  width: auto;
  max-width: none;
}

.framed .shadow {
  object-fit: fill;
  transform: translate(3px, 3px);
}
</style>
