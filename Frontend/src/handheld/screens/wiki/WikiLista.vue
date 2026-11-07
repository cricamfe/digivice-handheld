<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import ScreenHeader from "../../components/Header.vue";
import WikiArt from "../../components/WikiArt.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import {
  breakable,
  categoryIcons,
  categoryOf,
  openSearch,
  wikiCategories,
  wikiCategory,
  wikiScroll,
  type WikiCategory,
} from "../../wiki-common";
import { useGrowing } from "../../use-growing";
import { buildWikiList, npcCategoryArt, type WikiRow } from "../../wiki-list-sheet";

const { t } = useI18n();
const texts = useTexts();
const { go, param, replaceParam } = useScreen();

// The index and the tabs say which category to show; coming back from a sheet keeps the last one.
const requested = categoryOf(param.value);
if (requested !== null) {
  wikiCategory.value = requested;
}

const rows = computed(() => buildWikiList(wikiCategory.value, texts));

/** Rows drawn at once: 30 rows of two fill the screen several times over. */
const RowsPerPiece = 60;
/** Height of a row with its gap, to know how many rows a saved scroll needs. */
const RowHeight = 65;
const growing = useGrowing(() => rows.value.length, RowsPerPiece);
const drawn = computed(() => rows.value.slice(0, growing.shown.value));

const list = ref<HTMLElement | null>(null);

function restoreScroll(): void {
  if (!list.value) {
    return;
  }

  const saved = wikiScroll[wikiCategory.value] ?? 0;
  // Draws enough rows for the saved scroll to exist, then goes there.
  growing.ensure(Math.ceil((saved + list.value.clientHeight) / RowHeight) * 2 + RowsPerPiece);
  nextTick(() => {
    if (list.value) {
      list.value.scrollTop = saved;
    }
  });
}

function pick(category: WikiCategory): void {
  wikiCategory.value = category;
  replaceParam(category);
  growing.reset();
  nextTick(restoreScroll);
}

function remember(): void {
  if (list.value) {
    wikiScroll[wikiCategory.value] = list.value.scrollTop;
    growing.onScroll(list.value);
  }
}

function open(row: WikiRow): void {
  remember();
  go(row.target.screen, row.target.param);
}

function onSearch(): void {
  remember();
  openSearch();
  go("wikiBuscar");
}

onMounted(restoreScroll);
</script>

<template>
  <div class="view">
    <ScreenHeader back="wikiIndice">
      <span class="head">
        <span class="gold head-title">{{ t(`handheld.wiki.category.${wikiCategory}`) }}</span>
        <span class="head-count">{{ t("handheld.wiki.total", { count: rows.length }) }}</span>
      </span>
      <template #action>
        <button
          type="button"
          class="btn head-search"
          :aria-label="t('handheld.wiki.searchButton')"
          @click="onSearch"
        >
          <img :src="iconUrl('search')" alt="" class="icon" />
        </button>
      </template>
    </ScreenHeader>

    <section class="tabs" :aria-label="t('handheld.wiki.categories')">
      <button
        v-for="category in wikiCategories"
        :key="category"
        type="button"
        class="tab"
        :class="category === wikiCategory ? 'slot-on' : 'slot'"
        :aria-current="category === wikiCategory ? 'true' : undefined"
        @click="pick(category)"
      >
        <img v-if="category === 'npcs'" :src="npcCategoryArt()" alt="" class="tab-npc" />
        <img v-else :src="iconUrl(categoryIcons[category])" alt="" class="icon" />
        {{ t(`handheld.wiki.category.${category}`) }}
      </button>
    </section>

    <section ref="list" class="win scroll rows" @scroll.passive="remember">
      <button v-for="row in drawn" :key="row.key" type="button" class="thin row" @click="open(row)">
        <span class="art">
          <WikiArt :picture="row.picture" :icon="row.icon" :w="row.box.w" :h="row.box.h" />
        </span>
        <span class="text">
          <span class="drop name">{{ breakable(row.name) }}</span>
          <span class="dim sub">{{ row.sub }}</span>
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

.head {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head-title {
  font-size: 12px;
}

.head-count {
  font-size: 10px;
}

.head-search {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tabs {
  height: 58px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 3px;
  font-size: 10px;
}

.tab {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* The generic person of the game, 22 px tall like the other tab icons. */
.tab-npc {
  height: 22px;
  width: auto;
  flex-shrink: 0;
}

.rows {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  /* Two columns, not the design's three: a name has 210 px of line here, where three columns left 110 and "Armageddemon"
     (12 letters), "MetalSeadramon" or "Imperialdramon-F" did not fit in one. */
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: 62px;
  gap: 3px;
  align-content: start;
}

.row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  text-align: left;
  font-size: 10px;
}

.art {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

/* Long names take two lines instead of being cut, so "Saving at Seiryu Tower" can still be read. */
.name {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  overflow-wrap: anywhere;
  line-height: 1.4;
}

.sub {
  white-space: nowrap;
  overflow: hidden;
}
</style>
