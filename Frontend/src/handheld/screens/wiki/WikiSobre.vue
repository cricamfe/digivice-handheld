<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl } from "../../assets";
import { BoosterIcon } from "../../item-icons";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { breakable, numberOf } from "../../wiki-common";
import { buildBoosterSheet } from "../../wiki-drop-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The booster to show: its number in the database (the card and NPC sheets send a number). */
const boosterId = computed(() => numberOf(param.value));
const sheet = computed(() =>
  boosterId.value === null ? null : buildBoosterSheet(boosterId.value, gameState.value, texts),
);

/** Three columns of sources leave 110 px for a name (11 letters at 10 px); a word longer than 9 letters at 12 px ("KingDivermon") takes two. */
const sourceColumns = computed(() =>
  (sheet.value?.sources ?? []).some((source) =>
    source.name.split(/[\s(]+/).some((word) => word.length > 9),
  )
    ? 2
    : 3,
);
</script>

<template>
  <div v-if="sheet" class="view">
    <WikiHeader
      :icon="BoosterIcon"
      :title="sheet.name"
      :detail="t('handheld.wiki.booster.count', { count: sheet.cards.length })"
    />

    <section class="win scroll cards" :aria-label="t('handheld.wiki.booster.cards')">
      <button
        v-for="card in sheet.cards"
        :key="card.id"
        type="button"
        class="thin card"
        :aria-label="card.name"
        @click="go(card.target.screen, card.target.param)"
      >
        <WikiArt :picture="{ card: card.imageId }" :alt="card.name" :w="40" :h="46" />
      </button>
      <div v-if="sheet.cards.length === 0" class="none">
        <img :src="iconUrl('unknown')" alt="" class="icon" />
        <span>{{ t("handheld.wiki.noData") }}</span>
      </div>
    </section>

    <section class="win sources">
      <span class="gold">{{ t("handheld.wiki.booster.droppedBy") }}</span>
      <div
        v-if="sheet.sources.length > 0"
        class="scroll grid"
        :style="{ gridTemplateColumns: `repeat(${sourceColumns}, minmax(0, 1fr))` }"
      >
        <button
          v-for="source in sheet.sources"
          :key="source.key"
          type="button"
          class="thin source"
          @click="go(source.target.screen, source.target.param)"
        >
          <WikiArt :picture="{ url: source.imageUrl }" icon="unknown" :w="40" :h="68" />
          <span class="drop name">{{ breakable(source.name) }}</span>
        </button>
      </div>
      <div v-else class="none">
        <img :src="iconUrl('unknown')" alt="" class="icon" />
        <span>{{ t("enemy.dropSourcesNone") }}</span>
      </div>
    </section>
  </div>
  <div v-else class="view">
    <WikiHeader icon="unknown" :title="t('handheld.wiki.unknown')" />
    <div class="win empty">
      <img :src="iconUrl('unknown')" alt="" class="mark" />
      <span class="drop">{{ t("handheld.wiki.unknown") }}</span>
    </div>
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.mark {
  width: 44px;
  height: 44px;
}

/* Seven columns of three rows, as the design: a booster with more cards scrolls. */
.cards {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-auto-rows: 82px;
  gap: 3px;
  align-content: start;
}

.card {
  min-width: 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sources {
  height: 124px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
}

.grid {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: minmax(60px, 1fr);
  gap: 3px;
}

.source {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.name {
  min-width: 0;
  font-size: 12px;
  overflow-wrap: anywhere;
  line-height: 1.3;
}

.none {
  grid-column: 1 / -1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
</style>
