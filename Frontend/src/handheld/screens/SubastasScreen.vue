<script setup lang="ts">
import { useTexts } from "../texts";
import { placeWord } from "../place-words";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import ScreenHeader from "../components/Header.vue";
import { iconUrl } from "../assets";
import { buildAuctionSheet } from "../auction-sheet";
import { moneyAmount, moneyUnit } from "../game-words";
import { useScreen } from "../router";
import { useGameState } from "@/composables/use-game-state";
import type { AuctionStatus } from "@/types/auction-status.type";

const { t } = useI18n();
const texts = useTexts();
const { go } = useScreen();
const gameState = useGameState();

const sheet = computed(() => buildAuctionSheet(gameState.value, texts));

/** Frame, icon, label and colour of an auction in the history. */
const looks: Record<AuctionStatus, { frame: string; icon: string; label: string; tone: string }> = {
  participated: {
    frame: "good",
    icon: "check",
    label: "handheld.auction.participated",
    tone: "tone-done",
  },
  missed: { frame: "lock", icon: "cross", label: "handheld.auction.missed", tone: "tone-off" },
  available: {
    frame: "slot-on",
    icon: "signpost",
    label: "handheld.auction.now",
    tone: "tone-now",
  },
  notYetOccurred: {
    frame: "lock",
    icon: "locked",
    label: "handheld.auction.upcoming",
    tone: "tone-off",
  },
};
</script>

<template>
  <div class="view">
    <ScreenHeader back="diario">
      <span class="head">
        <img :src="iconUrl('chest')" alt="" class="icon" />
        <span class="gold head-title">{{ t("auction.modalTitle") }}</span>
      </span>
      <i18n-t keypath="handheld.auction.have" tag="span" class="head-bits">
        <template #bits
          ><span class="gold">{{ moneyAmount(t, sheet.bits) }}</span></template
        >
      </i18n-t>
    </ScreenHeader>

    <section v-if="sheet.current" class="plate panel">
      <div class="offer">
        <img
          :src="iconUrl(sheet.current.icon)"
          :alt="t('handheld.auction.equipment')"
          class="big"
        />
        <div class="offer-text">
          <span>{{ t("handheld.auction.nowAt", { place: placeWord(texts, "cargoTower") }) }}</span>
          <span class="gold offer-name">{{ sheet.current.name }}</span>
        </div>
      </div>
      <div class="prices">
        <span class="thin price">
          <span class="dim">{{ t("handheld.auction.bid") }}</span>
          <span class="gold">{{ moneyAmount(t, sheet.current.bid) }}</span>
        </span>
        <span class="thin price">
          <span class="dim">{{ t("handheld.auction.resale") }}</span>
          <span class="ice">{{ moneyAmount(t, sheet.current.resale) }}</span>
        </span>
      </div>
      <span class="closes">{{ sheet.current.closesWhen }}</span>
      <div v-if="sheet.current.hasNotEnoughBits" class="bad note">
        <img :src="iconUrl('alert')" :alt="t('handheld.auction.warning')" class="icon" />
        <span>{{
          t("handheld.auction.notEnoughBits", { bid: moneyAmount(t, sheet.current.bid) })
        }}</span>
      </div>
      <div v-else class="good note">
        <img :src="iconUrl('check')" alt="" class="icon" />
        <span>{{ t("handheld.auction.enoughBits", { money: moneyUnit(t) }) }}</span>
      </div>
    </section>
    <section v-else class="lock panel idle">
      <img :src="iconUrl('locked')" alt="" class="big" />
      <span class="dim idle-title">{{ t("auction.cardIdleTitle") }}</span>
      <span class="dim">{{ t("auction.cardIdleSubtitle") }}</span>
    </section>

    <section class="history" :aria-label="t('auction.historySubtitle')">
      <button
        v-for="row in sheet.rows"
        :key="row.id"
        type="button"
        class="row"
        :class="[looks[row.status].frame, looks[row.status].tone]"
        @click="go('wikiEquipo', row.equipmentId)"
      >
        <img :src="iconUrl(looks[row.status].icon)" alt="" class="icon" />
        <span class="row-text">
          <span class="drop row-name">{{ row.name }}</span>
          <span class="dim row-prices">
            <span>{{ t("handheld.auction.bidShort") }} {{ row.bid }}</span>
            <span>{{ t("handheld.auction.resaleShort") }} {{ row.resale }}</span>
          </span>
          <span class="row-label">{{ t(looks[row.status].label) }}</span>
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

.big {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.head-title {
  font-size: 12px;
}

.head-bits {
  font-size: 10px;
}

.panel {
  height: 190px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.idle {
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 10px;
}

.idle-title {
  font-size: 12px;
}

.offer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.offer-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
}

.offer-name {
  font-size: 16px;
}

.prices {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  font-size: 10px;
}

.price {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.closes {
  font-size: 10px;
  line-height: 1.5;
  text-wrap: balance;
}

.note {
  height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
}

.history {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 3px;
}

.row {
  min-width: 0;
  min-height: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  font-size: 10px;
}

.row-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.row-name {
  font-size: 12px;
  white-space: nowrap;
}

/* "Puja 2700 · Reventa 30000" is 25 letters and the line holds 23: the dot gives way to a gap between the two prices. */
.row-prices {
  display: flex;
  gap: 10px;
  white-space: nowrap;
}

.tone-done .row-name,
.tone-done .row-label {
  color: #b4f7a8;
}

.tone-now .row-name,
.tone-now .row-label {
  color: #ffd52e;
}

.tone-off .row-name,
.tone-off .row-label {
  color: #8fb4e8;
}
</style>
