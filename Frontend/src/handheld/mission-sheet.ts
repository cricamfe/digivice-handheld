import { createLogger } from "@/events/logger";
import type { State } from "@/models";
import { QuestModalPresenter } from "@/presenters/journal/quest-modal.presenter";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";
import type { StepViewModel } from "@/viewmodels/quest/step.viewmodel";
import { locationName, type Texts } from "./map-view";
import { questText } from "./quest-words";
import { questTarget } from "./quest-target-words";

const logger = createLogger("MissionSheet", "#e67e22");

export type StepState = "done" | "current" | "pending";

export interface MissionStep {
  number: string;
  /** Who or what the step is about ("Datamon", "Trooper"). */
  target: string;
  description: string;
  state: StepState;
}

export interface MissionChapter {
  number: number;
}

/** One picture of the step's map with the place the step points to. */
export interface MissionSlide {
  key: string;
  /** Null when the game files have no picture of the place. */
  imageUrl: string | null;
  /** Name of the place drawn. */
  place: string | null;
  /** Position of the pin as a percentage of the picture, or null when the step has none here. */
  pin: { x: number; y: number } | null;
  /** What the pin points to. */
  label: string | null;
}

export interface MissionSheet {
  questId: string;
  /** The chapter, or the title of the quest when it has no chapters. */
  title: string;
  /** Steps done out of the steps shown, like `7 / 27`. */
  progress: string;
  chapters: MissionChapter[];
  /** Index in `chapters` of the one on show, or -1 when the quest has no chapters. */
  chapterIndex: number;
  steps: MissionStep[];
}

function stepState(quest: QuestViewModel, step: StepViewModel): StepState {
  if (step.isDone) {
    return "done";
  }

  return quest.currentStep?.number === step.number ? "current" : "pending";
}

/**
 * The quest the screen was asked for, or null when the journal has no such quest (the presenter
 * answers null for an unknown id). Any other failure is logged before the screen degrades.
 */
export function findQuest(state: State, questId: string): QuestViewModel | null {
  try {
    return QuestModalPresenter.getQuestViewModel(state.journal, questId, state.party);
  } catch (error) {
    logger.error(`Quest ${questId} could not be built.`, error);
    return null;
  }
}

/**
 * The chapter the player is in, or the last one once the quest is over (the first when the quest has
 * not started), as an index of `quest.chapters`. -1 when there are no chapters.
 */
export function defaultChapterIndex(quest: QuestViewModel): number {
  if (quest.chapters.length === 0) {
    return -1;
  }

  const current = quest.chapters.findIndex((chapter) => chapter.isCurrent);
  return current === -1 ? quest.chapters.length - 1 : current;
}

/** The step to open: the one the player is on, else the last one of the list. */
export function defaultStepNumber(steps: MissionStep[]): string | null {
  return (
    (steps.find((step) => step.state === "current") ?? steps[steps.length - 1])?.number ?? null
  );
}

export function buildMissionSheet(
  quest: QuestViewModel,
  chapterIndex: number,
  texts: Texts,
): MissionSheet {
  const chapter = chapterIndex === -1 ? null : (quest.chapters[chapterIndex] ?? null);
  const steps = chapter === null ? quest.steps : chapter.steps;
  const done = steps.filter((step) => step.isDone).length;

  return {
    questId: quest.id,
    title:
      chapter === null
        ? (questText(texts, `${quest.id}.title`) ?? quest.id)
        : texts.t("journal.chapter", { number: chapter.number }),
    progress: `${done} / ${steps.length}`,
    chapters: quest.chapters.map((entry) => ({ number: entry.number })),
    chapterIndex: chapter === null ? -1 : chapterIndex,
    steps: steps.map((step) => ({
      number: step.number,
      target: questTarget(texts, quest.id, step.number) ?? step.number,
      description: questText(texts, `${quest.id}.steps.${step.number}.description`) ?? "",
      state: stepState(quest, step),
    })),
  };
}

/**
 * The maps that show where a step is: the world map first, then the place itself (and the places
 * one crosses on the way in, as the original modal does). The last one is the target.
 */
export function buildMissionSlides(
  quest: QuestViewModel,
  stepNumber: string,
  texts: Texts,
): MissionSlide[] {
  const step = quest.steps.find((entry) => entry.number === stepNumber);
  if (step === undefined || !step.location) {
    return [];
  }

  const world = QuestModalPresenter.getWorldMapLocations(step);
  const local = QuestModalPresenter.getLocalMapLocations(step, quest.id);
  // The local slides follow `innerLocation` and end with the step's own place.
  const placeIds = [...step.innerLocation.map((inner) => inner.location), step.location];

  const slides: MissionSlide[] = [];
  for (const slide of world) {
    const pin = slide.pins[0];
    slides.push({
      key: "world",
      imageUrl: slide.imageUrl,
      place: texts.t("handheld.mission.world"),
      pin: pin ? { x: pin.coordinates.x, y: pin.coordinates.y } : null,
      label: pin?.label ? (questText(texts, pin.label) ?? null) : null,
    });
  }

  local.forEach((slide, index) => {
    const pin = slide.pins[0];
    slides.push({
      key: `local-${index}`,
      imageUrl: slide.imageUrl,
      place: locationName(texts, placeIds[index] ?? step.location),
      pin: pin ? { x: pin.coordinates.x, y: pin.coordinates.y } : null,
      label: pin?.label ? (questText(texts, pin.label) ?? null) : null,
    });
  });

  return slides;
}
