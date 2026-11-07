<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import PixelKeyboard from "../../components/PixelKeyboard.vue";
import WikiArt from "../../components/WikiArt.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { breakable, wikiQuery, wikiSearchScroll } from "../../wiki-common";
import { useGrowing } from "../../use-growing";
import { searchWiki, type WikiHit } from "../../wiki-list-sheet";

const { t } = useI18n();
const texts = useTexts();
const { go, back } = useScreen();

const hits = computed(() => searchWiki(wikiQuery.value, texts));

/** A single letter matches some 600 entries: they are drawn 30 at a time, as the player scrolls. */
const growing = useGrowing(() => hits.value.length, 30);
const drawn = computed(() => hits.value.slice(0, growing.shown.value));

// A new word starts at the top of the results.
const results = ref<HTMLElement | null>(null);
watch(wikiQuery, () => {
  growing.reset();
  wikiSearchScroll.top = 0;
  if (results.value) {
    results.value.scrollTop = 0;
  }
});

function onScroll(): void {
  if (results.value) {
    wikiSearchScroll.top = results.value.scrollTop;
    growing.onScroll(results.value);
  }
}

/** Height of a result tile with its gap, to know how many tiles a saved scroll needs (two per row). */
const TileHeight = 109;

// Coming back from a sheet: the word is still typed, and the results return to where they were.
onMounted(() => {
  const saved = wikiSearchScroll.top;
  if (saved === 0 || !results.value) {
    return;
  }

  growing.ensure(Math.ceil((saved + results.value.clientHeight) / TileHeight) * 2 + 30);
  nextTick(() => {
    if (results.value) {
      results.value.scrollTop = saved;
    }
  });
});

function open(hit: WikiHit): void {
  go(hit.target.screen, hit.target.param);
}
</script>

<template>
  <div class="view">
    <header class="header">
      <button type="button" class="btn back" :aria-label="t('handheld.back')" @click="back()">
        <img :src="iconUrl('back')" alt="" class="icon" />
      </button>
      <div class="win field">
        <img :src="iconUrl('search')" alt="" class="icon" />
        <span class="gold query">{{ wikiQuery.toUpperCase() }}</span>
        <span class="caret"></span>
      </div>
    </header>

    <section
      ref="results"
      class="scroll results"
      :aria-label="t('handheld.wiki.searchButton')"
      @scroll.passive="onScroll"
    >
      <button v-for="hit in drawn" :key="hit.key" type="button" class="slot hit" @click="open(hit)">
        <WikiArt :picture="hit.picture" :icon="hit.icon" :w="22" :h="22" />
        <span class="text">
          <span class="drop name">{{ breakable(hit.name) }}</span>
          <span class="dim">{{ hit.kind }}</span>
        </span>
      </button>
      <div v-if="hits.length === 0" class="empty">
        <img :src="iconUrl('unknown')" alt="" class="mark" />
        <span class="dim">{{
          wikiQuery === "" ? t("handheld.wiki.typeToSearch") : t("handheld.wiki.noResults")
        }}</span>
      </div>
    </section>

    <PixelKeyboard v-model="wikiQuery" />
  </div>
</template>

<style scoped>
.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.header {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

.back {
  width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.field {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.query {
  min-width: 0;
  white-space: pre;
  overflow: hidden;
}

.caret {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  background: #ffd52e;
}

/* Two columns of two tiles, as the design draws four results; more of them scroll. */
.results {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: 106px;
  gap: 3px;
  align-content: start;
}

.hit {
  min-width: 0;
  min-height: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  text-align: left;
}

.text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.name {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  overflow-wrap: anywhere;
  font-size: 12px;
  line-height: 1.3;
}

.dim {
  white-space: nowrap;
  overflow: hidden;
}

.empty {
  grid-column: 1 / -1;
  height: 106px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 10px;
}

.mark {
  width: 44px;
  height: 44px;
}
</style>
