<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { cardUrl, iconUrl } from "../assets";
import type { WikiPicture } from "../wiki-common";

const props = defineProps<{
  /** The picture to draw. Without one, the kit icon `icon` is drawn instead. */
  picture?: WikiPicture | null;
  /** Kit icon for when there is no picture of the thing. */
  icon?: string;
  /** Size of the slot, in CSS pixels. The art keeps its proportions inside it. */
  w: number;
  h: number;
  alt?: string;
  /** Under the question mark of a missing picture, say that it is missing (for slots that fit text). */
  showText?: boolean;
}>();

const { t } = useI18n();

/** Pictures this wide or more are illustrations, drawn smooth; narrower ones are pixel art. */
const IllustrationWidth = 128;

/** `undefined` while a card loads, `null` when there is no picture. */
const source = ref<string | null | undefined>(undefined);
const natural = ref<{ width: number; height: number } | null>(null);
let token = 0;

// Watches a string that names the picture, not the object or an array of its parts: a screen builds
// `{ url }` again at every render, and a picture that did not change must not be reloaded.
const pictureKey = computed(() => {
  const picture = props.picture;
  if (picture === null || picture === undefined) {
    return "none";
  }

  return picture.card !== undefined ? `card:${picture.card}` : `url:${picture.url ?? ""}`;
});

watch(
  pictureKey,
  async () => {
    const picture = props.picture;
    const current = ++token;

    if (picture === null || picture === undefined) {
      natural.value = null;
      source.value = null;
      return;
    }

    if (picture.card !== undefined) {
      natural.value = null;
      source.value = undefined;
      const url = picture.card === null ? null : await cardUrl(picture.card);
      if (current === token) {
        source.value = url;
      }
      return;
    }

    const url = picture.url ?? null;
    if (url !== source.value) {
      natural.value = null;
      source.value = url;
    }
  },
  { immediate: true },
);

function onLoad(event: Event): void {
  const image = event.target as HTMLImageElement;
  natural.value = { width: image.naturalWidth, height: image.naturalHeight };
}

/** Pixel art is scaled by whole numbers so its pixels stay square; an illustration is fitted. */
const shown = computed(() => {
  if (natural.value === null) {
    return null;
  }

  const { width, height } = natural.value;
  const fit = Math.min(props.w / width, props.h / height);
  const isPixelArt = width < IllustrationWidth;
  const scale = isPixelArt && fit >= 1 ? Math.floor(fit) : fit;

  return {
    width: Math.max(1, Math.floor(width * scale)),
    height: Math.max(1, Math.floor(height * scale)),
    isPixelArt,
  };
});

const isIcon = computed(() => props.picture === null || props.picture === undefined);
const iconName = computed(() => props.picture?.icon ?? props.icon ?? "unknown");
/** A question mark of 44 px where there is room, else 22 px. */
const markSize = computed(() => (Math.min(props.w, props.h) >= 64 ? 44 : 22));
const hasText = computed(() => props.showText === true && props.w >= 88 && props.h >= 88);
</script>

<template>
  <span class="wiki-art" :style="{ width: `${w}px`, height: `${h}px` }">
    <img
      v-if="isIcon || picture?.icon !== undefined"
      :src="iconUrl(iconName)"
      :alt="alt ?? ''"
      class="art-icon"
      :style="{ width: `${Math.min(w, h)}px`, height: `${Math.min(w, h)}px` }"
    />
    <template v-else-if="source">
      <img
        :src="source"
        :alt="alt ?? ''"
        class="art-image"
        :class="{ smooth: shown !== null && !shown.isPixelArt, hidden: shown === null }"
        :style="
          shown === null
            ? { width: `${w}px`, height: `${h}px` }
            : { width: `${shown.width}px`, height: `${shown.height}px` }
        "
        @load="onLoad"
        @error="source = null"
      />
    </template>
    <span v-else-if="source === null" class="art-missing" :title="t('handheld.wiki.noPicture')">
      <img
        :src="iconUrl('unknown')"
        :alt="t('handheld.wiki.noPicture')"
        :style="{ width: `${markSize}px`, height: `${markSize}px` }"
      />
      <span v-if="hasText" class="dim">{{ t("handheld.wiki.noPicture") }}</span>
    </span>
  </span>
</template>

<style scoped>
.wiki-art {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.art-icon,
.art-image {
  flex-shrink: 0;
}

.art-image.smooth {
  image-rendering: auto;
}

.art-image.hidden {
  visibility: hidden;
}

.art-missing {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 10px;
  text-align: center;
}
</style>
