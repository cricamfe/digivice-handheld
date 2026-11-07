<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import PortraitSlot from "./PortraitSlot.vue";
import { getVitals, percentText } from "../party-view";
import type { DigimonSlot } from "@/models";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";

const props = defineProps<{
  slots: DigimonSlot[];
  /** `index` of the selected slot. */
  selected: number | null;
  mapId: string;
  /** Draw the MP bar under the HP bar (the map screens do). */
  showMp?: boolean;
  /** Write each Digimon's name under its portrait. */
  showNames?: boolean;
  /** Write each Digimon's level above its portrait. */
  showLevels?: boolean;
  /** Keep the HP bar of the selected Digimon too (the battle shows every one's). */
  keepHp?: boolean;
  /** What a tap on a portrait does, for the screen reader: the keys of "unselected" and "selected". */
  labelKeys?: [string, string];
  /** The portraits anchor a floating panel: a tap on one while its panel is open toggles it, not closes it first. */
  anchored?: boolean;
}>();

/** `button` is the portrait that was tapped, for a panel that floats next to it. */
const emit = defineEmits<{ pick: [index: number, button: HTMLElement] }>();

const { t } = useI18n();

const entries = computed(() =>
  props.slots.map((slot) => {
    const vitals = getVitals(slot.digimon!, props.mapId);
    const isSelected = slot.index === props.selected;
    const name = ProfilePresenter.getName(slot.digimonId!);

    return {
      index: slot.index,
      digimonId: slot.digimonId!,
      status: vitals.status,
      isSelected,
      level: props.showLevels ? slot.digimon!.level : null,
      hp: isSelected && !props.keepHp ? null : percentText(vitals.hp.current, vitals.hp.max),
      mp: props.showMp ? percentText(vitals.mp.current, vitals.mp.max) : null,
      label: t(
        (props.labelKeys ?? ["handheld.party.view", "handheld.party.viewState"])[
          isSelected ? 1 : 0
        ],
        { name },
      ),
    };
  }),
);
</script>

<template>
  <div class="party-column">
    <PortraitSlot
      v-for="entry in entries"
      :key="entry.index"
      :digimon="entry.digimonId"
      :status="entry.status"
      :selected="entry.isSelected"
      :hp="entry.hp"
      :mp="entry.mp"
      :label="entry.label"
      :show-name="showNames"
      :level="entry.level"
      :data-panel-anchor="anchored ? '' : undefined"
      @pick="emit('pick', entry.index, $event)"
    />
  </div>
</template>

<style scoped>
.party-column {
  width: 96px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
