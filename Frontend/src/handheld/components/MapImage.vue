<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";

defineProps<{
  /** Picture of the place, or null when the game files have none. */
  src: string | null;
  alt: string;
  /** Small text under the question mark, like the id of the map. */
  detail?: string;
  /**
   * Draw all of it instead of filling the frame. Only matters until the layer it sits in has the picture's own size (when
   * the picture has not loaded yet, or its size is not known): the layer is then the whole frame.
   */
  whole?: boolean;
  /** Decode it together with the frame it is first drawn in, not after: for a picture that was decoded beforehand. */
  sync?: boolean;
}>();

const emit = defineEmits<{ load: [event: Event] }>();

const { t } = useI18n();
</script>

<template>
  <img
    v-if="src"
    :src="src"
    :alt="alt"
    class="map-image"
    :class="{ whole }"
    draggable="false"
    :decoding="sync ? 'sync' : undefined"
    @load="emit('load', $event)"
  />
  <div v-else class="map-unknown">
    <img :src="iconUrl('unknown')" alt="" class="map-unknown-icon" />
    <span class="drop map-unknown-text">{{ t("handheld.map.unknown") }}</span>
    <span v-if="detail" class="dim">{{ detail }}</span>
  </div>
</template>

<style scoped>
/* Fills the layer it sits in, which must be `position: absolute` or `relative`: the layer has the picture's proportions. */
.map-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.map-image.whole {
  object-fit: contain;
}

.map-unknown {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.map-unknown-icon {
  width: 44px;
  height: 44px;
}

.map-unknown-text {
  font-size: 12px;
}
</style>
