<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import type { TreeNode } from "../evolution-sheet";
import EvolutionArt from "./EvolutionArt.vue";

const props = defineProps<{
  node: TreeNode;
  selected: boolean;
}>();

/** The tap hands over the node's own box, which is what the panel floats beside. */
const emit = defineEmits<{ pick: [element: HTMLElement] }>();

const { t } = useI18n();

/** The frame says which one is chosen and which ones the Digimon cannot reach yet. */
const frame = computed(() => {
  if (props.selected) {
    return "slot-on";
  }

  return props.node.isUnlocked ? "slot" : "lock";
});

/** At 12 px a name of more than 14 letters would run under the lock, so it is written at 10 px. */
const isLong = computed(() => props.node.name.length > 14);
</script>

<template>
  <button
    type="button"
    class="node"
    :class="frame"
    :aria-pressed="selected"
    :data-node="node.id"
    data-panel-anchor
    @click="emit('pick', $event.currentTarget as HTMLElement)"
  >
    <EvolutionArt
      v-if="node.imageUrl"
      :key="node.imageUrl"
      :src="node.imageUrl"
      :evolution-id="node.id"
      class="art"
      :class="{ locked: !node.isUnlocked }"
    />
    <span v-else class="missing">
      <img :src="iconUrl('unknown')" alt="" class="missing-icon" />
      <span class="dim">{{ t("handheld.evolutions.noArt") }}</span>
    </span>
    <span class="head">
      <span class="outline name" :class="{ small: isLong }">{{ node.name }}</span>
      <img
        :src="iconUrl(node.isUnlocked ? 'unlocked' : 'locked')"
        :alt="t(node.isUnlocked ? 'handheld.evolutions.unlocked' : 'handheld.evolutions.locked')"
        class="lock-icon"
      />
    </span>
    <span class="requirements">
      <span
        v-for="requirement in node.requirements"
        :key="requirement.text"
        class="outline requirement"
        :class="{ met: requirement.isMet }"
        >{{ requirement.text }}</span
      >
    </span>
  </button>
</template>

<style scoped>
.node {
  position: relative;
  width: 216px;
  height: 132px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: 22px minmax(0, 1fr) auto;
  gap: 2px;
  padding: 2px 4px;
  flex-shrink: 0;
  overflow: hidden;
  text-align: left;
}

.art {
  grid-row: 2;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.art.locked {
  /* Darkened, not see-through: nothing of the node's own frame shows through the art. */
  filter: grayscale(0.4) brightness(0.45);
}

.missing {
  grid-row: 2;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 10px;
}

.missing-icon {
  width: 22px;
  height: 22px;
}

.head {
  grid-row: 1;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  line-height: 22px;
}

.name.small {
  font-size: 10px;
  line-height: 22px;
}

.lock-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.requirements {
  grid-row: 3;
  z-index: 1;
  padding: 3px 4px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 10px;
  background: #000a2b;
}

.requirement {
  /* "Imperialdramon-F Nv.40" is 22 letters and the line holds 19: the level goes down to a second line, the name stays whole. */
  white-space: normal;
  line-height: 10px;
  color: #ffffff;
}

.requirement.met {
  color: #77e878;
}
</style>
