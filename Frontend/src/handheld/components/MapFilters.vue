<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import type { ActorRole } from "../field-actors";
import { ActorRoleOrder, useMapOptions } from "../map-options";
import FloatingPanel, { type PanelAnchor } from "./FloatingPanel.vue";

defineProps<{
  /** The button that opens it, in the panel's own pixels. */
  anchor: PanelAnchor;
  hostWidth: number;
  hostHeight: number;
}>();

const emit = defineEmits<{ close: [] }>();

const { t } = useI18n();
const { exits, objectives, characters, roles } = useMapOptions();

/** How many kinds of character are on, like `3/5`. */
const shownRoles = computed(
  () => `${ActorRoleOrder.filter((role) => roles[role].value).length}/${ActorRoleOrder.length}`,
);

const roleIcons: Record<ActorRole, string> = {
  shop: "shop",
  service: "training",
  battle: "battle",
  story: "journal",
  chest: "chest",
  talk: "walking",
};
</script>

<template>
  <!-- Beside the map, not over all of it: what is switched is seen to go at once. -->
  <FloatingPanel
    :anchor="anchor"
    :host-width="hostWidth"
    :host-height="hostHeight"
    :label="t('handheld.mapFilters.title')"
    :title="t('handheld.mapFilters.title')"
    icon="filter"
    :width="320"
    @close="emit('close')"
  >
    <div class="rows">
      <!-- Lit slot while it is on; the choice stays across restarts. -->
      <button
        type="button"
        class="panel-action row"
        :class="exits ? 'slot-on' : 'slot'"
        :aria-pressed="exits"
        @click="exits = !exits"
      >
        <img :src="iconUrl('exit')" alt="" />
        <span>{{ t("handheld.mapFilters.exits") }}</span>
        <img :src="iconUrl(exits ? 'check' : 'cross')" alt="" />
      </button>
      <button
        type="button"
        class="panel-action row"
        :class="objectives ? 'slot-on' : 'slot'"
        :aria-pressed="objectives"
        @click="objectives = !objectives"
      >
        <img :src="iconUrl('signpost')" alt="" />
        <span>{{ t("handheld.mapFilters.objectives") }}</span>
        <img :src="iconUrl(objectives ? 'check' : 'cross')" alt="" />
      </button>
      <button
        type="button"
        class="panel-action row"
        :class="characters ? 'slot-on' : 'slot'"
        :aria-pressed="characters"
        @click="characters = !characters"
      >
        <img :src="iconUrl('party')" alt="" />
        <span>{{ t("handheld.mapFilters.characters") }}</span>
        <em class="gold count">{{ shownRoles }}</em>
        <img :src="iconUrl(characters ? 'check' : 'cross')" alt="" />
      </button>
      <!-- The kinds of character hang from the row above, tied to it by a line. In the colour their names have on the map;
           they wait while all the characters are off. -->
      <div class="branch" :class="{ resting: !characters }" role="group">
        <div v-for="role in ActorRoleOrder" :key="role" class="twig">
          <button
            type="button"
            class="panel-action row kind"
            :class="characters ? (roles[role].value ? 'slot-on' : 'slot') : 'lock'"
            :disabled="!characters"
            :aria-pressed="roles[role].value"
            @click="roles[role].value = !roles[role].value"
          >
            <img :src="iconUrl(roleIcons[role])" alt="" />
            <span :class="characters ? `role-${role}` : 'dim'">{{
              t(`handheld.mapFilters.role.${role}`)
            }}</span>
            <img :src="iconUrl(roles[role].value ? 'check' : 'cross')" alt="" />
          </button>
        </div>
      </div>
    </div>
  </FloatingPanel>
</template>

<style scoped>
.rows {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.row {
  height: 34px;
}

/* A count, not a word: it takes no more room than it needs, and the name keeps the rest. */
.panel-action > .count {
  flex-shrink: 0;
  font-style: normal;
  font-size: 10px;
}

.branch {
  --twig: #7cc0ff;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.branch.resting {
  --twig: #2c4a86;
}

.twig {
  position: relative;
}

/* The line: it comes down from the row above along all of a kind, and turns into it at its middle. */
.twig::before {
  content: "";
  position: absolute;
  left: -12px;
  top: -4px;
  bottom: 0;
  width: 4px;
  background: var(--twig);
}

.twig:last-child::before {
  bottom: 50%;
}

.twig::after {
  content: "";
  position: absolute;
  left: -12px;
  top: calc(50% - 2px);
  width: 12px;
  height: 4px;
  background: var(--twig);
}

.kind {
  width: 100%;
  height: 30px;
}
</style>
