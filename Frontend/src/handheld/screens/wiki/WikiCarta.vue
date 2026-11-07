<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { moneyAmount } from "../../game-words";
import type { WikiTarget } from "../../wiki-common";
import { buildCardSheet } from "../../wiki-card-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The card to show: its id in the database (a string, or a number from a caller that has one). */
const cardId = computed(() => (param.value === null ? "" : String(param.value)));
const sheet = computed(() => buildCardSheet(cardId.value, gameState.value, texts));

function open(target: WikiTarget): void {
  go(target.screen, target.param);
}
</script>

<template>
  <div v-if="sheet" class="view">
    <WikiHeader
      icon="card"
      :title="sheet.card.name"
      :detail="t('handheld.wiki.card.number', { id: sheet.card.id })"
    />

    <section class="top">
      <div class="win picture">
        <WikiArt
          :picture="{ card: sheet.card.imageId }"
          :alt="sheet.card.name"
          :w="120"
          :h="138"
          show-text
        />
      </div>
      <div class="win facts" :class="{ action: sheet.isAction }">
        <span class="fact"
          ><span class="dim">{{
            t(sheet.isAction ? "handheld.wiki.card.type" : "handheld.wiki.card.color")
          }}</span
          ><span>{{ sheet.typeLabel }}</span></span
        >
        <template v-if="sheet.points">
          <span class="fact"
            ><span class="dim">AP</span><span class="gold">{{ sheet.points.ap }}</span></span
          >
          <span class="fact"
            ><span class="dim">HP</span><span class="gold">{{ sheet.points.hp }}</span></span
          >
        </template>
        <p v-else-if="sheet.note" class="note">{{ sheet.note }}</p>
        <div v-else class="no-note">
          <img :src="iconUrl('unknown')" alt="" class="icon" />
          <span class="dim">{{ t("handheld.wiki.card.noNote") }}</span>
        </div>
      </div>
    </section>

    <section class="win boosters">
      <span class="gold">{{ t("handheld.wiki.card.inBoosters") }}</span>
      <div v-if="sheet.boosters.length > 0" class="scroll grid">
        <button
          v-for="booster in sheet.boosters"
          :key="booster.id"
          type="button"
          class="thin booster"
          @click="open(booster.target)"
        >
          <img :src="iconUrl('card')" alt="" class="icon" />
          <span class="drop name">{{ booster.name }}</span>
        </button>
      </div>
      <div v-else class="none">
        <img :src="iconUrl('unknown')" alt="" class="icon" />
        <span>{{ t("enemy.obtainedFromNone") }}</span>
      </div>
    </section>

    <section
      v-if="sheet.shops.length > 0"
      class="shops"
      :class="{ many: sheet.shops.length > 1, scroll: sheet.shops.length > 2 }"
    >
      <button
        v-for="shop in sheet.shops"
        :key="shop.id"
        type="button"
        class="slot shop"
        @click="open(shop.target)"
      >
        <img :src="iconUrl('shop')" :alt="t('enemy.purchasableAt')" class="mark" />
        <span class="label">
          <span v-if="sheet.shops.length === 1" class="dim">{{ t("enemy.purchasableAt") }}</span>
          <span class="ice shop-name">{{ shop.name }}</span>
        </span>
        <span v-if="shop.price !== null" class="gold price">{{
          moneyAmount(t, shop.price, "cardPrice")
        }}</span>
        <span v-else class="price"
          ><img :src="iconUrl('unknown')" :alt="t('handheld.wiki.noData')" class="icon"
        /></span>
      </button>
    </section>
    <section v-else class="lock shop none-shop">
      <img :src="iconUrl('cross')" alt="" class="mark" />
      <span>{{ t("enemy.purchasableAtNone") }}</span>
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
  flex-shrink: 0;
}

.top {
  height: 170px;
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}

.picture {
  width: 170px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.facts {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  font-size: 12px;
}

.fact {
  display: flex;
  justify-content: space-between;
}

/* An action card has no AP or HP: its note takes their place. */
.note {
  margin: 0;
  font-size: 10px;
  line-height: 1.6;
}

.no-note {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
}

.boosters {
  flex: 1;
  min-height: 0;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: minmax(40px, 1fr);
  gap: 3px;
  align-content: start;
}

.booster {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.none {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.shops {
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* Several shops: compact rows with a 22 px icon, two at a time, so the boosters keep their room. */
.shops.many {
  max-height: 84px;
}

.many .shop {
  height: 42px;
}

.many .mark {
  width: 22px;
  height: 22px;
}

.shop {
  height: 62px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  font-size: 10px;
}

.none-shop {
  justify-content: center;
}

.label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.shop-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.price {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
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
