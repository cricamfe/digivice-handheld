<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl, portraitUrl } from "../assets";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";
import type { DigimonStatus } from "@/types/digimon-status.type";

const props = defineProps<{
  /** Id of the Digimon, as the game numbers it. */
  digimon: number;
  status: DigimonStatus;
  selected?: boolean;
  /** HP fill like `89.6%`. Only the Digimon that are not selected show it. */
  hp?: string | null;
  /** MP fill like `96.7%`, drawn under the HP bar (the map screens). */
  mp?: string | null;
  label?: string;
  /** Write the Digimon's name under the portrait. */
  showName?: boolean;
  /** Its level, above the portrait. */
  level?: number | null;
}>();

const emit = defineEmits<{ pick: [button: HTMLElement] }>();

const { t } = useI18n();
const name = computed(() => ProfilePresenter.getName(props.digimon));
const portrait = computed(() => portraitUrl(props.digimon));
</script>

<template>
  <button
    type="button"
    class="portrait-slot"
    :class="[selected ? 'slot-on' : 'slot', { 'with-mp': mp, 'with-level': level != null }]"
    :aria-label="label ?? name"
    :aria-current="selected ? 'true' : undefined"
    @click="emit('pick', $event.currentTarget as HTMLElement)"
  >
    <span v-if="level != null" class="drop slot-level">{{
      t("handheld.party.slotLevel", { level })
    }}</span>
    <span class="portrait-frame" :class="`portrait-${status}`">
      <img v-if="portrait" :src="portrait" alt="" class="portrait" />
      <img v-else :src="iconUrl('unknown')" alt="" class="missing" />
    </span>
    <span v-if="showName" class="drop slot-name">{{ name }}</span>
    <span v-if="hp" class="bar hp-track"
      ><span class="hp hp-fill" :style="{ width: hp }"></span
    ></span>
    <span v-if="mp" class="bar hp-track"
      ><span class="mp hp-fill" :style="{ width: mp }"></span
    ></span>
  </button>
</template>

<style scoped>
.portrait-slot {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* With two bars the design pulls the portrait and the bars closer. */
.with-mp {
  gap: 4px;
}

.with-level {
  justify-content: flex-start;
  gap: 4px;
  padding-top: 5px;
}

.portrait-frame {
  --portrait-edge: #7fe8ff;
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #00103a;
  background-image:
    linear-gradient(to right, #000e3f30 1px, transparent 1px),
    linear-gradient(to bottom, #000e3f30 1px, transparent 1px);
  background-size: 4px 4px;
}

/* The extracted 32 px image stays a whole 2× nearest-neighbour image. Frame, grid and condition
   belong to the UI, so all four states use the same image from the player's disc. */
.portrait-frame::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  box-shadow: inset 0 0 0 2px var(--portrait-edge);
}

.portrait-injured {
  --portrait-edge: #ffd52e;
  background-color: #262606;
}

.portrait-debuffed {
  --portrait-edge: #ffb09a;
  background-color: #26061a;
}

.portrait-ko {
  --portrait-edge: #8fb4e8;
  background-color: #0c1024;
}

.portrait-ko .portrait {
  filter: grayscale(1);
}

.portrait {
  width: 64px;
  height: 64px;
  image-rendering: pixelated;
}

.missing {
  width: 44px;
  height: 44px;
}

.slot-level {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 1;
}

.slot-name {
  font-size: 10px;
  white-space: nowrap;
}

.hp-track {
  display: block;
  width: 100%;
  height: 12px;
}

.hp-fill {
  display: block;
  height: 100%;
}
</style>
