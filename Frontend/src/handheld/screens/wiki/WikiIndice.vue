<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import {
  breakable,
  categoryIcons,
  enemyTarget,
  openSearch,
  wikiCategory,
  type WikiCategory,
} from "../../wiki-common";
import { buildWikiIndex, npcCategoryArt } from "../../wiki-list-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go } = useScreen();
const gameState = useGameState();

const sheet = computed(() => buildWikiIndex(gameState.value, texts));

const place = computed(
  () => sheet.value.here.place?.toUpperCase() ?? t("handheld.map.unknown").toUpperCase(),
);

function openCategory(category: WikiCategory): void {
  wikiCategory.value = category;
  go("wikiLista", category);
}

function onSearch(): void {
  openSearch();
  go("wikiBuscar");
}

function openEnemy(id: string): void {
  const target = enemyTarget(id);
  go(target.screen, target.param);
}
</script>

<template>
  <div class="view">
    <button type="button" class="win search" @click="onSearch">
      <img :src="iconUrl('search')" alt="" class="icon" />
      <span class="dim search-text">{{ t("handheld.wiki.search") }}</span>
    </button>

    <section class="cats" :aria-label="t('handheld.wiki.categories')">
      <button
        v-for="card in sheet.categories"
        :key="card.category"
        type="button"
        class="slot cat"
        @click="openCategory(card.category)"
      >
        <img v-if="card.category === 'npcs'" :src="npcCategoryArt()" alt="" class="npc-art" />
        <img v-else :src="iconUrl(categoryIcons[card.category])" alt="" class="cat-icon" />
        <span class="drop cat-label">{{ t(`handheld.wiki.category.${card.category}`) }}</span>
        <span class="gold">{{ card.count }}</span>
      </button>
    </section>

    <section class="win here">
      <span class="gold">{{ t("handheld.wiki.onThisMap", { name: place }) }}</span>
      <div v-if="sheet.here.enemies.length > 0" class="scroll enemies">
        <button
          v-for="enemy in sheet.here.enemies"
          :key="enemy.id"
          type="button"
          class="enemy"
          :class="enemy.isBoss ? 'thin-plate' : 'thin'"
          @click="openEnemy(enemy.id)"
        >
          <WikiArt :picture="enemy.picture" icon="boss" :w="44" :h="44" />
          <span class="enemy-name" :class="{ gold: enemy.isBoss }">{{
            breakable(enemy.name)
          }}</span>
        </button>
      </div>
      <div v-else class="safe">
        <img :src="iconUrl('check')" alt="" class="icon" />
        <span>{{ t("enemy.noEnemiesOnMap") }}</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.search {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.search-text {
  font-size: 12px;
}

.cats {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 4px;
}

.cat {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.cat-icon {
  width: 44px;
  height: 44px;
}

/* The generic person of the game at twice its size (19 x 34), as the design's sprite. */
.npc-art {
  width: 38px;
  height: 68px;
}

.cat-label {
  font-size: 12px;
}

.here {
  height: 138px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.enemies {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: 98px;
  gap: 4px;
}

.enemy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  font-size: 10px;
}

/* A name with a bracket breaks before it, so "Kokatorimon(Brown)" (18 letters) does not run into the frame. */
.enemy-name {
  max-width: 100%;
  line-height: 1.3;
  text-align: center;
}

.safe {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
