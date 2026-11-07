<script setup lang="ts">
import { useTexts } from "../texts";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import MapImage from "../components/MapImage.vue";
import ScreenHeader from "../components/Header.vue";
import { iconUrl } from "../assets";
import { MainQuestParam } from "../map-sheet";
import {
  buildMissionSheet,
  buildMissionSlides,
  defaultChapterIndex,
  defaultStepNumber,
  findQuest,
  type StepState,
} from "../mission-sheet";
import { useScreen } from "../router";
import { useCover } from "../use-cover";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const { param } = useScreen();
const gameState = useGameState();
const texts = useTexts();

/** The quest to show: the id the Diario sent, or the main quest (the map strip sends "mainQuest"). */
const questId = computed(() => (typeof param.value === "string" ? param.value : MainQuestParam));
const quest = computed(() => findQuest(gameState.value, questId.value));

// `null` means "the default": the chapter the player is in, the step the player is on.
const pickedChapter = ref<number | null>(null);
const pickedStep = ref<string | null>(null);
const pickedSlide = ref<number | null>(null);

const chapterIndex = computed(() =>
  quest.value === null ? -1 : (pickedChapter.value ?? defaultChapterIndex(quest.value)),
);
const sheet = computed(() =>
  quest.value === null ? null : buildMissionSheet(quest.value, chapterIndex.value, texts),
);
const selected = computed(() => {
  const steps = sheet.value?.steps ?? [];
  const picked = steps.find((step) => step.number === pickedStep.value);
  return picked ?? steps.find((step) => step.number === defaultStepNumber(steps)) ?? null;
});

const slides = computed(() =>
  quest.value === null || selected.value === null
    ? []
    : buildMissionSlides(quest.value, selected.value.number, texts),
);
/** The target comes last, so it is the one shown until the player flips through the others. */
const slideIndex = computed(() => pickedSlide.value ?? slides.value.length - 1);
const slide = computed(() => slides.value[slideIndex.value] ?? null);

watch(selected, (step, previous) => {
  if (step?.number !== previous?.number) {
    pickedSlide.value = null;
  }
});

const hasPrevious = computed(() => chapterIndex.value > 0);
const hasNext = computed(
  () => sheet.value !== null && chapterIndex.value < sheet.value.chapters.length - 1,
);

function moveChapter(offset: number): void {
  pickedChapter.value = chapterIndex.value + offset;
  pickedStep.value = null;
  pickedSlide.value = null;
  nextTick(scrollToSelected);
}

function nextSlide(): void {
  pickedSlide.value = (slideIndex.value + 1) % slides.value.length;
}

const list = ref<HTMLElement | null>(null);

/** Centres the chosen step in the list, so the player lands on it instead of the top. */
function scrollToSelected(): void {
  const row = list.value?.querySelector<HTMLElement>("[aria-current='step']");
  if (list.value && row) {
    list.value.scrollTop = row.offsetTop - (list.value.clientHeight - row.offsetHeight) / 2;
  }
}

onMounted(scrollToSelected);

const stage = ref<HTMLElement | null>(null);
const { onLoad, place, area } = useCover(stage, () => slide.value?.imageUrl ?? null);

/** The design draws the target pin with its tip on the point: a 44 px icon, 22 px left and 40 px up. */
const pinStyle = computed(() => {
  const pin = slide.value?.pin;
  if (!pin) {
    return null;
  }

  const spot = place(pin.x, pin.y);
  return { left: `calc(${spot.left} - 22px)`, top: `calc(${spot.top} - 40px)` };
});

/** Pixels a label takes besides its letters: the 4 px frame and the 6 px margin on each side. */
const LabelFrame = 20;
const LabelHeight = 22;
/** Room kept between the label and the edge of the picture. */
const LabelEdge = 4;

/**
 * The label sits under the pin (over it near the bottom), centred on it and pushed back inside the picture when the
 * pin is near an edge. The font is monospaced, so its width is the letters times 10 px.
 */
const labelStyle = computed(() => {
  const pin = slide.value?.pin;
  const label = slide.value?.label;
  if (!pin) {
    return null;
  }

  const spot = place(pin.x, pin.y);
  const top = pin.y > 80 ? `calc(${spot.top} - 66px)` : `calc(${spot.top} + 6px)`;
  const box = area.value;
  if (box === null || !label) {
    return { top, left: spot.left, transform: "translateX(-50%)" };
  }

  const width = label.length * 10 + LabelFrame;
  const centre = parseFloat(spot.left);
  const left = Math.min(Math.max(centre - width / 2, LabelEdge), box.width - width - LabelEdge);
  const y = parseFloat(spot.top) + (pin.y > 80 ? -66 : 6);
  const clampedTop = Math.min(Math.max(y, LabelEdge), box.height - LabelHeight - LabelEdge);
  return { top: `${clampedTop}px`, left: `${left}px` };
});

const stateIcons: Record<StepState, string> = {
  done: "check",
  current: "signpost",
  pending: "locked",
};

const stateLabels: Record<StepState, string> = {
  done: "handheld.mission.stepDone",
  current: "handheld.mission.stepCurrent",
  pending: "handheld.mission.stepPending",
};
</script>

<template>
  <div v-if="sheet" class="view">
    <ScreenHeader back="diario" :title="sheet.title" :detail="sheet.progress">
      <template v-if="sheet.chapters.length > 1" #action>
        <button
          type="button"
          class="chapter-button"
          :class="hasPrevious ? 'btn' : 'lock'"
          :disabled="!hasPrevious"
          :aria-label="t('handheld.mission.previousChapter')"
          @click="moveChapter(-1)"
        >
          <img :src="iconUrl('back')" alt="" class="icon" />{{ t("handheld.mission.chapterShort") }}
        </button>
        <button
          type="button"
          class="chapter-button"
          :class="hasNext ? 'btn' : 'lock'"
          :disabled="!hasNext"
          :aria-label="t('handheld.mission.nextChapter')"
          @click="moveChapter(1)"
        >
          {{ t("handheld.mission.chapterShort")
          }}<img :src="iconUrl('arrow')" alt="" class="icon" />
        </button>
      </template>
    </ScreenHeader>

    <section class="body">
      <div ref="list" class="win scroll steps">
        <button
          v-for="step in sheet.steps"
          :key="step.number"
          type="button"
          class="step"
          :class="[step.number === selected?.number ? 'thin-on' : 'thin', `state-${step.state}`]"
          :aria-current="step.number === selected?.number ? 'step' : undefined"
          @click="pickedStep = step.number"
        >
          <img
            :src="iconUrl(stateIcons[step.state])"
            :alt="t(stateLabels[step.state])"
            class="icon"
          />
          <span class="gold step-number">{{ step.number }}</span>
          <span class="step-target">{{ step.target }}</span>
        </button>
        <span v-if="sheet.steps.length === 0" class="dim">{{ t("journal.noSteps") }}</span>
      </div>

      <div ref="stage" class="win picture">
        <MapImage
          :src="slide?.imageUrl ?? null"
          :alt="slide?.place ?? t('handheld.mission.noMap')"
          :detail="slide === null ? t('handheld.mission.noMap') : (slide.place ?? undefined)"
          @load="onLoad"
        />
        <img
          v-if="pinStyle"
          :src="iconUrl('place')"
          :alt="t('handheld.mission.target')"
          class="pin"
          :style="pinStyle"
        />
        <span
          v-if="labelStyle && slide?.label"
          class="thin-plate gold pin-label"
          :style="labelStyle"
          >{{ slide.label }}</span
        >
        <div v-if="slide && slide.pin === null" class="win-soft no-pin">
          {{ t("handheld.mission.noPin") }}
        </div>
        <div v-if="slide?.place" class="thin-plate gold place">
          {{ slide.place }}
          <span v-if="slides.length > 1" class="ice">{{ slideIndex + 1 }}/{{ slides.length }}</span>
        </div>
        <button
          v-if="slides.length > 1"
          type="button"
          class="btn next-map"
          :aria-label="t('handheld.mission.nextMap')"
          @click="nextSlide"
        >
          <img :src="iconUrl('arrow')" alt="" class="icon" />
        </button>
      </div>
    </section>

    <div v-if="selected" class="win description">
      <img
        :src="iconUrl(stateIcons[selected.state])"
        :alt="t(stateLabels[selected.state])"
        class="icon"
      />
      <span
        ><span class="gold">[{{ selected.number }}] </span>{{ selected.description }}</span
      >
    </div>
  </div>
  <div v-else class="view">
    <ScreenHeader back="diario" :title="t('handheld.mission.unknown')" />
    <div class="win empty">
      <img :src="iconUrl('unknown')" alt="" class="empty-icon" />
      <span class="drop">{{ t("handheld.mission.unknown") }}</span>
    </div>
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

/* Labelled "Cap." so the pair cannot be mistaken for the back button at the other end. */
.chapter-button {
  width: 88px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 10px;
}

.body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.steps {
  position: relative;
  /* 272 px: "MasterTyrannomon" is one word of 16 letters and the line of a step holds 16 at this width. */
  width: 272px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.step {
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  text-align: left;
  font-size: 10px;
}

.step-number {
  flex-shrink: 0;
}

.step-target {
  min-width: 0;
  /* Two lines of 11 px fit the 24 px inside the row; a name with spaces takes the second one instead of being cut. */
  line-height: 11px;
  overflow-wrap: break-word;
}

.state-done .step-target {
  color: #77e878;
}

.state-pending .step-target {
  color: #8fb4e8;
}

.picture {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.pin {
  position: absolute;
  width: 44px;
  height: 44px;
}

.pin-label {
  position: absolute;
  font-size: 10px;
  white-space: nowrap;
}

.no-pin {
  position: absolute;
  left: 12px;
  right: 12px;
  top: 45%;
  text-align: center;
  font-size: 10px;
  line-height: 1.5;
}

.place {
  position: absolute;
  left: 2px;
  top: 2px;
  font-size: 10px;
}

.next-map {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.description {
  height: 58px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  line-height: 1.5;
}

.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
}

.empty-icon {
  width: 44px;
  height: 44px;
}
</style>
