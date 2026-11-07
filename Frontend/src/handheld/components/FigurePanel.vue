<script setup lang="ts">
import FloatingPanel, { type PanelAnchor } from "./FloatingPanel.vue";
import PanelRow from "./PanelRow.vue";
import { sentences } from "../format";
import type { FigureDetail } from "../figure-detail";

defineProps<{
  detail: FigureDetail;
  anchor: PanelAnchor;
  hostWidth: number;
  hostHeight: number;
}>();

defineEmits<{ close: [] }>();
</script>

<template>
  <FloatingPanel
    :anchor="anchor"
    :host-width="hostWidth"
    :host-height="hostHeight"
    :width="detail.verdict === undefined ? 300 : 280"
    beside
    :label="detail.label"
    :title="detail.label"
    :icon="detail.key"
    @close="$emit('close')"
  >
    <div v-if="detail.rows.length > 0" class="panel-rows">
      <PanelRow v-for="row in detail.rows" :key="row.label" :label="row.label">
        <span :class="row.tone">{{ row.value }}</span>
      </PanelRow>
    </div>
    <PanelRow
      v-if="detail.verdict !== undefined"
      :icon="detail.verdict ? 'check' : 'cross'"
      :label="detail.meaning"
      :tone="detail.verdict ? 'up' : 'down'"
      wrap
    />
    <template v-else>
      <p v-for="sentence in sentences(detail.meaning)" :key="sentence" class="panel-text">
        {{ sentence }}
      </p>
    </template>
    <p v-if="detail.note" class="panel-text panel-note">{{ detail.note }}</p>
  </FloatingPanel>
</template>
