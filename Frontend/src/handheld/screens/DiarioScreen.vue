<script setup lang="ts">
import { useTexts } from "../texts";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import {
  buildJournalSheet,
  selectedTab,
  type CardTone,
  type JournalTabKey,
} from "../journal-sheet";
import { questKindIcon } from "../quest-offers";
import { useScreen } from "../router";
import { useGameState } from "@/composables/use-game-state";
import { useGameStore } from "@/stores/use-game-store";

const { t } = useI18n();
const texts = useTexts();
const { go } = useScreen();
const gameState = useGameState();
const store = useGameStore();

const sheet = computed(() => buildJournalSheet(gameState.value, texts, store.questOffers));
const tab = computed(() => sheet.value.tabs.find((entry) => entry.key === selectedTab.value));

const tabIcons: Record<JournalTabKey, string> = {
  underWay: "signpost",
  available: "alert",
  done: "check",
};

const cardFrames: Record<CardTone, string> = {
  done: "good",
  fresh: "plate",
};
</script>

<template>
  <div class="view">
    <button v-if="sheet.main" type="button" class="plate main" @click="go('mision', sheet.main.id)">
      <img :src="iconUrl('signpost')" alt="" class="main-icon" />
      <span class="main-text">
        <span class="gold main-title">{{ sheet.main.title }}</span>
        <span class="main-step"
          ><span v-if="sheet.main.step" class="gold">[{{ sheet.main.step }}] </span
          >{{ sheet.main.text }}</span
        >
      </span>
    </button>

    <button
      type="button"
      class="auction"
      :class="sheet.hasAuction ? 'slot-on' : 'lock'"
      @click="go('subastas')"
    >
      <img :src="iconUrl(sheet.hasAuction ? 'signpost' : 'locked')" alt="" class="icon" />
      <template v-if="sheet.hasAuction">
        <span class="gold">{{ t("auction.cardActiveTitle") }}</span>
        <span class="ice auction-note">{{ t("auction.cardActiveSubtitle") }}</span>
      </template>
      <template v-else>
        <span class="dim">{{ t("auction.cardIdleTitle") }}</span>
        <span class="dim auction-note">{{ t("auction.cardIdleSubtitle") }}</span>
      </template>
    </button>

    <div class="tabs">
      <button
        v-for="entry in sheet.tabs"
        :key="entry.key"
        type="button"
        class="tab"
        :class="entry.key === selectedTab ? 'slot-on' : 'slot'"
        :aria-pressed="entry.key === selectedTab"
        @click="selectedTab = entry.key"
      >
        <img :src="iconUrl(tabIcons[entry.key])" alt="" class="icon" />
        <span class="drop tab-label">{{ entry.label }}</span>
        <span class="gold tab-count">{{ entry.count }}</span>
      </button>
    </div>

    <section class="scroll cards" :aria-label="t('handheld.journal.quests')">
      <p v-if="tab?.cards.length === 0" class="dim none">
        {{ t(`handheld.journal.none.${selectedTab}`) }}
      </p>
      <button
        v-for="card in tab?.cards ?? []"
        :key="card.id"
        type="button"
        class="card"
        :class="[cardFrames[card.tone], `tone-${card.tone}`]"
        @click="go('mision', card.id)"
      >
        <img :src="questKindIcon(card.kind)" alt="" class="icon" />
        <span class="card-text">
          <span class="drop card-title">{{ card.title }}</span>
          <span class="card-line"
            ><span v-if="card.step" class="gold">[{{ card.step }}] </span>{{ card.text }}</span
          >
        </span>
      </button>
    </section>
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

/* The chapter strip is a plate: 68 px with a 44 px signpost and two lines of the current step. */
.main {
  height: 68px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.main-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.main-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.main-title {
  font-size: 12px;
}

.main-step {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  font-size: 10px;
  line-height: 1.5;
  text-wrap: balance;
}

.auction {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  font-size: 10px;
}

.auction-note {
  margin-left: auto;
}

.tabs {
  height: 52px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 3px;
}

.tab {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 6px;
}

.tab-label {
  font-size: 12px;
}

.tab-count {
  margin-left: auto;
  font-size: 10px;
}

.cards {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}

/* 64 px: the title and two lines of the step (the longest, 79 letters, takes two of the 54 a line holds). */
.card {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.card-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.card-line {
  font-size: 10px;
  line-height: 11px;
}

.tone-done .card-title {
  color: #b4f7a8;
}

.tone-done .card-line {
  color: #77e878;
}

.tone-fresh .card-title {
  color: #ffd52e;
}

.none {
  padding: 16px;
  font-size: 12px;
  text-align: center;
}
</style>
