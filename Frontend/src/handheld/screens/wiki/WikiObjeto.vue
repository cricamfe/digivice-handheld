<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { moneyUnit } from "../../game-words";
import { numberOf } from "../../wiki-common";
import { buildItemSheet } from "../../wiki-drop-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The object to show: its number in the database (a drop of an enemy sends a number). */
const itemId = computed(() => numberOf(param.value));
const sheet = computed(() =>
  itemId.value === null ? null : buildItemSheet(itemId.value, gameState.value, texts),
);
</script>

<template>
  <div v-if="sheet" class="view">
    <WikiHeader :icon="sheet.icon" :title="sheet.name" :detail="t('handheld.wiki.kind.item')" />

    <section class="win top">
      <div class="slot-on big">
        <img :src="iconUrl(sheet.icon)" alt="" class="art" />
      </div>
      <div class="facts">
        <div class="thin-plate fact">
          <span class="dim">{{ t("enemy.resaleValueLabel") }}</span>
          <span class="amount"
            ><span class="gold figure">{{ sheet.resale }}</span
            >{{ moneyUnit(t) }}</span
          >
        </div>
        <div class="thin fact">
          <span class="dim">{{ t("handheld.wiki.item.inShops") }}</span>
          <span class="amount"
            ><img :src="iconUrl(sheet.soldInStore ? 'check' : 'cross')" alt="" class="icon" />{{
              t(sheet.soldInStore ? "handheld.wiki.item.sold" : "handheld.wiki.item.notSold")
            }}</span
          >
        </div>
      </div>
    </section>

    <section v-if="sheet.note" class="win note">{{ sheet.note }}</section>

    <section class="win sources">
      <span class="gold">{{ t("handheld.wiki.item.droppedBy") }}</span>
      <div v-if="sheet.sources.length > 0" class="scroll grid">
        <button
          v-for="source in sheet.sources"
          :key="source.key"
          type="button"
          class="slot source"
          @click="go(source.target.screen, source.target.param)"
        >
          <WikiArt :picture="{ url: source.imageUrl }" icon="unknown" :w="112" :h="112" show-text />
          <span class="gold name">{{ source.name }}</span>
          <span v-if="source.level !== null">{{
            `${t("handheld.wiki.level", { level: source.level })} · ${t("handheld.bar.hp")} ${source.hp}`
          }}</span>
          <span v-if="source.only" class="dim">{{ source.only }}</span>
          <span v-if="source.place" class="ice place"
            ><img :src="iconUrl('place')" alt="" class="icon" />{{ source.place }}</span
          >
          <span v-else-if="source.level !== null" class="gold place"
            ><img :src="iconUrl('unknown')" alt="" class="icon" />{{
              t("handheld.wiki.enemy.unknownPlace")
            }}</span
          >
        </button>
      </div>
      <div v-else class="none">
        <img :src="iconUrl('unknown')" alt="" class="icon" />
        <span>{{ t("handheld.wiki.item.noSources") }}</span>
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

.top {
  height: 148px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* The slot is 124 px: its frame leaves 108 and the 88 px icon sits inside with room to spare. */
.big {
  width: 124px;
  height: 124px;
  flex-shrink: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.art {
  width: 88px;
  height: 88px;
}

.facts {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
}

.fact {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 10px;
}

.amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.fact .icon {
  align-self: center;
}

.fact .amount:has(.icon) {
  align-items: center;
}

.figure {
  font-size: 16px;
}

.note {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 1.5;
}

.sources {
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
  grid-auto-rows: minmax(204px, 1fr);
  gap: 4px;
}

.source {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
}

.name {
  font-size: 12px;
}

.place {
  display: flex;
  align-items: center;
  gap: 4px;
}

.none {
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
