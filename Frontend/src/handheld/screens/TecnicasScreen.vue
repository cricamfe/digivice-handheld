<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { createLogger } from "@/events/logger";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import { iconUrl } from "../assets";
import { buildTechniqueSheet, tryBuildEvolutionTree } from "../evolution-sheet";
import { useTexts } from "../texts";
import { useScreen } from "../router";
import { useGameState } from "@/composables/use-game-state";
import { PartyPresenter } from "@/presenters/party/party.presenter";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";

const { t } = useI18n();
const texts = useTexts();
const logger = createLogger("TecnicasScreen", "#e67e22");
const { go, param } = useScreen();
const gameState = useGameState();

/** What the screen was asked for: the partner's slot and the digievolution (Equipo, Evoluciones). */
const asked = computed(() => {
  const value = param.value;
  if (typeof value !== "object" || value === null) {
    return null;
  }

  const { digimonSlot, digievolutionId } = value;
  return typeof digimonSlot === "number" && typeof digievolutionId === "number"
    ? { digimonSlot, digievolutionId }
    : null;
});

const slot = computed(() =>
  PartyPresenter.getFilledSlots(gameState.value.party.slots).find(
    (entry) => entry.index === asked.value?.digimonSlot,
  ),
);

const sheet = computed(() => {
  const current = slot.value;
  if (asked.value === null || current === undefined) {
    return null;
  }

  // An id the tables do not have is a plain "unknown digievolution"; any other failure is a bug.
  const id = asked.value.digievolutionId;
  if (!DigievolutionRepository.getAllDigievolutions().some((entry) => entry.id === id)) {
    return null;
  }

  const tree = tryBuildEvolutionTree(current.digimon!, current.digimonId!, t);
  if (tree === null) {
    return null;
  }

  try {
    return buildTechniqueSheet(tree, current.digimon!, current.digimonId!, id, texts);
  } catch (error) {
    logger.error(`The techniques of digievolution ${id} could not be built.`, error);
    return null;
  }
});

const partnerName = computed(() =>
  slot.value === undefined ? "" : ProfilePresenter.getName(slot.value.digimonId!),
);

const levelLine = computed(() => {
  const level = sheet.value?.level;
  return level === null || level === undefined
    ? t("handheld.techniques.notObtained", { name: partnerName.value })
    : t("handheld.techniques.level", { level, name: partnerName.value });
});

const nextLine = computed(() => {
  const next = sheet.value?.next ?? [];
  return next.length === 0
    ? t("handheld.techniques.last")
    : t("handheld.techniques.next", { names: next.join(", ") });
});

function back(): void {
  go("evoluciones", asked.value);
}
</script>

<template>
  <div class="view">
    <header class="win head">
      <img v-if="sheet?.imageUrl" :src="sheet.imageUrl" alt="" class="head-art" />
      <img v-else :src="iconUrl('unknown')" alt="" class="head-missing" />
      <button
        type="button"
        class="btn back"
        :aria-label="t('handheld.techniques.backToTree')"
        @click="back"
      >
        <img :src="iconUrl('back')" alt="" class="icon" />
      </button>
      <div class="head-text">
        <template v-if="sheet">
          <span class="gold head-name">{{ sheet.name }}</span>
          <span class="drop">{{ levelLine }}</span>
          <span class="ice drop follows">{{ nextLine }}</span>
        </template>
        <span v-else class="drop head-name">{{ t("handheld.techniques.unknown") }}</span>
      </div>
    </header>

    <section class="list" :aria-label="t('digievolution.techniques')">
      <article
        v-for="technique in sheet?.techniques ?? []"
        :key="technique.id"
        class="technique"
        :class="technique.isUnlocked ? 'slot' : 'lock'"
      >
        <span class="cell">
          <img :src="iconUrl(technique.typeIcon)" :alt="technique.typeLabel" class="icon" />
        </span>
        <span class="info">
          <span class="drop name">{{ technique.name }}</span>
          <span class="dim line">{{ technique.description }}</span>
        </span>
        <span class="cell">
          <img :src="iconUrl(technique.elementIcon)" :alt="technique.elementLabel" class="icon" />
        </span>
        <span class="cell cell-text ice">
          <span class="dim">{{ t("handheld.bar.mp") }}</span>
          <span>{{ technique.mp }}</span>
        </span>
        <span class="cell cell-text gold">
          <img :src="iconUrl('learn')" :alt="t('handheld.techniques.learn')" class="icon" />
          {{ technique.learnAt }}
        </span>
        <span class="cell cell-text gold">
          <template v-if="technique.isSignature">
            <img
              :src="iconUrl('signature')"
              :alt="t('handheld.techniques.signatureAlt')"
              class="icon"
            />
            {{ t("handheld.techniques.signature") }}
          </template>
          <template v-else-if="technique.loadAt !== null">
            <img :src="iconUrl('load')" :alt="t('handheld.techniques.load')" class="icon" />
            {{ technique.loadAt }}
          </template>
        </span>
      </article>
      <p v-if="sheet && sheet.techniques.length === 0" class="win none">
        {{ t("digievolution.noTechData") }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.head {
  position: relative;
  height: 90px;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* The art is an illustration: smooth, as in the design, cropped to the face. */
.head-art {
  position: absolute;
  right: 0;
  top: 0;
  width: 40%;
  height: 220%;
  object-fit: cover;
  object-position: center 15%;
  image-rendering: auto;
}

.head-missing {
  position: absolute;
  right: 12px;
  top: 12px;
  width: 44px;
  height: 44px;
}

.back {
  position: relative;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* The list of what follows can be long (up to seven names), so it wraps before the art. */
.head-text {
  position: relative;
  max-width: 58%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.follows {
  line-height: 12px;
}

.head-name {
  font-size: 16px;
}

.list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}

/* Rows share the height like in the design, but never grow past a six-row list's row. */
.technique {
  flex: 1 0 56px;
  max-height: 62px;
  min-height: 0;
  display: grid;
  /* The last column holds "Firma" (5 letters, 50 px). */
  grid-template-columns: 30px minmax(0, 1fr) 46px 46px 46px 58px;
  align-items: center;
  column-gap: 2px;
}

.technique.lock {
  /* The design fades locked rows; a darker row says the same without a see-through box. */
  filter: brightness(0.75);
}

.cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.cell-text {
  flex-direction: column;
  gap: 3px;
  font-size: 10px;
}

.info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding-left: 4px;
}

.name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.line {
  font-size: 10px;
  line-height: 11px;
  /* A description of 36 letters does not fit the 34 of the line: it takes a second one, which a 62 px row holds. */
  white-space: normal;
}

.none {
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 62px;
}
</style>
