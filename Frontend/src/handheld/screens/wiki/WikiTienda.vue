<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { moneyAmount } from "../../game-words";
import { buildShopSheet } from "../../wiki-card-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The shop to show: its id in the database, from the map, a card or the search. */
const shopId = computed(() => (param.value === null ? "" : String(param.value)));
const sheet = computed(() => buildShopSheet(shopId.value, gameState.value, texts));

/** When the stock on offer is sold, in words. */
const windowText = computed(() => {
  const window = sheet.value?.window;
  if (window === undefined) {
    return "";
  }

  if (window.kind === "until") {
    return t("handheld.wiki.shop.untilStep", { step: window.step });
  }

  return window.kind === "from"
    ? t("handheld.wiki.shop.fromStep", { step: window.step })
    : t("handheld.wiki.shop.fromStart");
});
</script>

<template>
  <div v-if="sheet" class="view">
    <WikiHeader
      icon="shop"
      :title="sheet.name"
      :detail="t('handheld.wiki.shop.count', { count: sheet.stock.length })"
    />

    <section class="win keeper">
      <WikiArt
        :picture="{ url: sheet.imageUrl }"
        :alt="sheet.keeper ?? sheet.name"
        :w="62"
        :h="80"
        show-text
      />
      <span class="text">
        <span v-if="sheet.keeper">{{
          t("handheld.wiki.shop.keeper", { name: sheet.keeper })
        }}</span>
        <span v-else class="unknown-keeper">
          <img :src="iconUrl('unknown')" alt="" class="icon" />{{ t("handheld.wiki.noData") }}
        </span>
        <span class="dim">{{ windowText }}</span>
      </span>
      <button
        v-if="sheet.place"
        type="button"
        class="thin place"
        @click="go('mapa', sheet.place.id)"
      >
        <img :src="iconUrl('place')" alt="" class="icon" />
        <span class="ice place-name">{{ sheet.place.name }}</span>
      </button>
    </section>

    <section class="scroll stock" :aria-label="t('handheld.wiki.shop.stock')">
      <button
        v-for="entry in sheet.stock"
        :key="entry.card.id"
        type="button"
        class="slot card"
        @click="go(entry.card.target.screen, entry.card.target.param)"
      >
        <WikiArt :picture="{ card: entry.card.imageId }" :alt="entry.card.name" :w="80" :h="92" />
        <span class="drop name">{{ entry.card.name }}</span>
        <span class="gold">{{ moneyAmount(t, entry.price, "cardPrice") }}</span>
      </button>
      <div v-if="sheet.stock.length === 0" class="none">
        <img :src="iconUrl('unknown')" alt="" class="mark" />
        <span>{{ t("handheld.wiki.noData") }}</span>
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

.keeper {
  height: 96px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 10px;
}

.text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
  line-height: 1.4;
}

.unknown-keeper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.place {
  width: 170px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.place-name {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
}

.stock {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: minmax(140px, 1fr);
  gap: 3px;
  align-content: start;
}

.card {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 10px;
}

.name {
  max-width: 100%;
  text-align: center;
  overflow-wrap: anywhere;
}

.none {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
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
