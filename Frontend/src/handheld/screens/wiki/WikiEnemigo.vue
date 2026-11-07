<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import FigurePanel from "../../components/FigurePanel.vue";
import type { PanelAnchor } from "../../components/FloatingPanel.vue";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { moneyUnit } from "../../game-words";
import type { WikiTarget } from "../../wiki-common";
import { usePanelHost } from "../../use-panel-host";
import { buildEnemySheet } from "../../wiki-enemy-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The enemy to show: its id in the database, from a list, the search, a drop or a battle. */
const enemyId = computed(() => (param.value === null ? "" : String(param.value)));
const sheet = computed(() => buildEnemySheet(enemyId.value, gameState.value, texts));

/** Tapping a figure explains it in a panel beside it; tapping the same figure again closes it. */
const panel = ref<{ key: string; anchor: PanelAnchor } | null>(null);
const { host, hostSize, anchorOf } = usePanelHost();
watch(enemyId, () => {
  panel.value = null;
});

function showFigure(key: string, element: unknown): void {
  if (panel.value?.key === key) {
    panel.value = null;
    return;
  }

  const anchor = anchorOf(element);
  panel.value = anchor === null ? null : { key, anchor };
}

const panelDetail = computed(
  () => sheet.value?.figures.find((figure) => figure.key === panel.value?.key)?.detail ?? null,
);

function open(target: WikiTarget): void {
  go(target.screen, target.param);
}

function openPlace(id: string): void {
  go("mapa", id);
}

const hasPlaces = computed(() => (sheet.value?.places.length ?? 0) > 0);
</script>

<template>
  <div v-if="sheet" ref="host" class="view page">
    <WikiHeader
      :icon="sheet.speciesIcon"
      :title="sheet.name"
      :detail="`${sheet.speciesLabel} · ${t('handheld.wiki.level', { level: sheet.level })}`"
      search
    />

    <section class="top">
      <div class="win picture">
        <WikiArt :picture="{ url: sheet.imageUrl }" :alt="sheet.name" :w="134" :h="138" show-text />
      </div>

      <div class="win facts">
        <span class="fact" :class="{ wide: sheet.reward === null }"
          ><span class="dim">{{ t("handheld.bar.hp") }}</span
          ><span>{{ sheet.hp }}</span></span
        >
        <template v-if="sheet.reward">
          <span class="fact"
            ><span class="dim">{{ t("enemy.baseExp") }}</span
            ><span>{{ sheet.reward.exp }}</span></span
          >
          <span class="fact"
            ><span class="dim">{{ moneyUnit(t) }}</span
            ><span>{{ sheet.reward.bits }}</span></span
          >
          <span class="fact"
            ><span class="dim">{{ t("enemy.baseDvexp") }}</span
            ><span>{{ sheet.reward.dvexp }}</span></span
          >
          <div class="gain">
            <span class="dim">{{ t("handheld.wiki.enemy.dvexpGain") }}</span>
            <span class="partners">
              <span v-for="member in sheet.reward.party" :key="member.name" class="gold partner">
                <img v-if="member.sprite" :src="member.sprite" :alt="member.name" class="sprite" />
                <img v-else :src="iconUrl('unknown')" :alt="member.name" class="icon" />
                <template v-if="member.gain !== null">{{ member.gain }}</template>
                <img
                  v-else
                  :src="iconUrl('unknown')"
                  :alt="t('handheld.wiki.noData')"
                  class="icon"
                />
              </span>
            </span>
          </div>
        </template>
        <div v-else class="no-reward">
          <img :src="iconUrl('unknown')" alt="" class="icon" />
          <span>{{ t("handheld.wiki.enemy.noReward") }}</span>
        </div>
        <div class="moves">
          <span class="move"
            ><img :src="iconUrl('strength')" :alt="sheet.attackLabel" class="icon" />
            <template v-if="sheet.attack">{{ sheet.attack }}</template>
            <template v-else>{{ t("handheld.wiki.noData") }}</template>
          </span>
          <span class="move"
            ><img :src="iconUrl('spirit')" :alt="t('enemy.technique')" class="icon" />
            <template v-if="sheet.technique">{{ sheet.technique }}</template>
            <template v-else>{{ t("handheld.wiki.noData") }}</template>
          </span>
        </div>
      </div>
    </section>

    <section class="win figures">
      <button
        v-for="figure in sheet.figures"
        :key="figure.key"
        type="button"
        class="figure"
        :aria-label="figure.label"
        data-panel-anchor
        @click="showFigure(figure.key, $event.currentTarget)"
      >
        <img :src="iconUrl(figure.key)" alt="" class="icon" />
        <span class="drop" :class="figure.tone">{{ figure.text }}</span>
      </button>
    </section>

    <section class="links">
      <div
        v-if="hasPlaces"
        class="column"
        :class="{ many: sheet.places.length > 1, scroll: sheet.places.length > 1 }"
      >
        <button
          v-for="place in sheet.places"
          :key="place.id"
          type="button"
          class="slot link"
          @click="openPlace(place.id)"
        >
          <img :src="iconUrl('place')" :alt="t('handheld.wiki.enemy.place')" class="mark" />
          <span class="label">
            <span v-if="sheet.places.length === 1" class="dim">{{ t("enemy.whereToFind") }}</span>
            <span class="ice name">{{ place.name }}</span>
          </span>
        </button>
      </div>
      <button
        v-else-if="sheet.owner"
        type="button"
        class="slot link"
        @click="open(sheet.owner.target)"
      >
        <img :src="iconUrl('battle')" alt="" class="mark" />
        <span class="label">
          <span class="dim">{{ t("handheld.wiki.enemy.owner") }}</span>
          <span class="ice name">{{ sheet.owner.name }}</span>
        </span>
      </button>
      <div v-else class="slot link">
        <img :src="iconUrl('unknown')" alt="" class="mark" />
        <span class="label">
          <span class="dim">{{ t("enemy.whereToFind") }}</span>
          <span class="gold name">{{ t("handheld.wiki.enemy.unknownPlace") }}</span>
        </span>
      </div>

      <div
        v-if="sheet.drops.length > 0"
        class="column"
        :class="{ many: sheet.drops.length > 1, scroll: sheet.drops.length > 1 }"
      >
        <button
          v-for="drop in sheet.drops"
          :key="drop.key"
          type="button"
          class="slot link"
          @click="open(drop.target)"
        >
          <img :src="iconUrl(drop.icon)" :alt="t('enemy.drop')" class="mark" />
          <span class="label">
            <span v-if="sheet.drops.length === 1" class="dim">{{ t("enemy.drop") }}</span>
            <span class="gold name">{{ drop.name }}</span>
            <span v-if="drop.only" class="dim name">{{ drop.only }}</span>
          </span>
        </button>
      </div>
      <div v-else class="lock link">
        <img :src="iconUrl('cross')" alt="" class="mark" />
        <span class="label">
          <span class="dim">{{ t("enemy.drop") }}</span>
          <span class="name">{{ t("drops.none") }}</span>
        </span>
      </div>
    </section>

    <FigurePanel
      v-if="panel !== null && panelDetail !== null"
      :detail="panelDetail"
      :anchor="panel.anchor"
      :host-width="hostSize.width"
      :host-height="hostSize.height"
      @close="panel = null"
    />
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

.sprite {
  width: 32px;
  height: 32px;
}

.up {
  color: #b4f7a8;
}

.down {
  color: #ffb09a;
}

/* The panel of a figure floats over the screen, so the screen is where it is placed. */
.page {
  position: relative;
}

.top {
  height: 166px;
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}

.picture {
  width: 166px;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.facts {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 10px;
  align-content: space-between;
  font-size: 10px;
}

.fact {
  display: flex;
  justify-content: space-between;
}

.fact.wide {
  grid-column: 1 / -1;
}

.gain {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 2px dashed #144aa8;
  padding-top: 6px;
}

.partners {
  display: flex;
  gap: 8px;
}

.partner {
  display: flex;
  align-items: center;
  gap: 3px;
}

.no-reward {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 2px dashed #144aa8;
  padding-top: 6px;
  line-height: 1.4;
}

.moves {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  line-height: 1.4;
}

.move {
  display: flex;
  align-items: center;
  gap: 5px;
}

.figures {
  height: 148px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-template-rows: repeat(5, minmax(0, 1fr));
  grid-auto-flow: column;
  gap: 0 4px;
  font-size: 12px;
}

.figure {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 5px;
}

.links {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.link {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  text-align: left;
  font-size: 10px;
}

.column {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* Several places or drops: compact rows of 22 px icons, two at a time, the rest by scrolling. */
.column.many {
  overflow-y: auto;
}

.column.many .link {
  flex: none;
  height: 42px;
}

.column.many .mark {
  width: 22px;
  height: 22px;
}

/* A drop that is only in one place has two lines (10 px each) in the 22 px a 42 px row leaves inside its frame. */
.column.many .label {
  gap: 2px;
}

.label {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.name {
  font-size: 10px;
  white-space: nowrap;
  overflow: hidden;
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
