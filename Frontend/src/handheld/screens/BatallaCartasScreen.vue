<script setup lang="ts">
import { useTexts } from "../texts";
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { cardUrl, genericNpcUrl, iconUrl } from "../assets";
import { buildCardBattleSheet } from "../card-battle-sheet";
import { useScreen } from "../router";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go } = useScreen();
const gameState = useGameState();

const sheet = computed(() => buildCardBattleSheet(gameState.value, texts));

// The art of the cards loads on demand. An ID is in the map once its URL has answered, with null
// when the game files have no such picture.
const art = ref(new Map<number, string | null>());

// The folder is the same for as long as the card IDs are, however often the game state updates.
const cardIds = computed(() => [
  ...new Set(sheet.value.deck.flatMap((card) => (card.imageId === null ? [] : [card.imageId]))),
]);

watch(
  () => cardIds.value.join("|"),
  async (key) => {
    const ids = cardIds.value;
    const loaded = await Promise.all(ids.map(async (id) => [id, await cardUrl(id)] as const));
    if (key === cardIds.value.join("|")) {
      art.value = new Map(loaded);
    }
  },
  { immediate: true },
);

/** Art of a card: its URL, null when it has none, undefined while it is still loading. */
function artOf(imageId: number | null): string | null | undefined {
  return imageId === null ? null : art.value.get(imageId);
}

const title = computed(() =>
  sheet.value.rival === null
    ? t("handheld.cardBattle.unknownRival")
    : t("handheld.cardBattle.versus", { name: sheet.value.rival }),
);

/** The folder line: its name and level, whichever the game files know. */
const folderLine = computed(() => {
  const parts = [
    sheet.value.folderName,
    sheet.value.folderLevel === null
      ? null
      : t("handheld.cardBattle.level", { level: sheet.value.folderLevel }),
  ];
  return parts.filter((part) => part !== null).join(" · ");
});

const placeLine = computed(() => {
  const parts = [
    sheet.value.place,
    sheet.value.charisma === null
      ? null
      : t("handheld.cardBattle.charisma", { range: sheet.value.charisma }),
  ];
  return parts.filter((part) => part !== null).join(" · ");
});

// Inside the frame there are 94 px. The sprite is drawn at the largest whole multiple of its size
// that fits, which keeps the pixels square.
const SpriteRoom = 94;
const tamerSize = ref<{ width: string; height: string } | undefined>(undefined);

function onTamerLoad(event: Event): void {
  const image = event.target as HTMLImageElement;
  if (image.naturalWidth > 0) {
    const scale = Math.max(1, Math.floor(SpriteRoom / image.naturalHeight));
    tamerSize.value = {
      width: `${image.naturalWidth * scale}px`,
      height: `${image.naturalHeight * scale}px`,
    };
  }
}

// A prize name has about 17 letters of room at 12 px; a longer one is written at 8 px instead of cut.
const PrizeChars = 17;

const boosterLine = computed(() => sheet.value.booster?.name ?? t("handheld.cardBattle.noPrize"));

const rivalLine = computed(() =>
  sheet.value.rival === null
    ? t("handheld.cardBattle.noRival")
    : t("handheld.cardBattle.sheet", { name: sheet.value.rival }),
);

/** The rival's own sprite, or the generic one when the game files have none. */
const rivalImage = computed(() => sheet.value.tamerImageUrl ?? genericNpcUrl());

function openRival(): void {
  if (sheet.value.npcId !== null) {
    go("wikiNpc", sheet.value.npcId);
  }
}
</script>

<template>
  <div class="view">
    <section class="win rival">
      <img v-if="sheet.backdropUrl" :src="sheet.backdropUrl" alt="" class="backdrop" />
      <button
        type="button"
        class="tamer"
        :disabled="sheet.npcId === null"
        :aria-label="t('handheld.cardBattle.sheet', { name: sheet.rival ?? '' })"
        @click="openRival"
      >
        <img :src="rivalImage" alt="" class="tamer-image" :style="tamerSize" @load="onTamerLoad" />
      </button>
      <div class="win-soft rival-text">
        <span class="gold rival-title">{{ title }}</span>
        <span v-if="folderLine">{{ folderLine }}</span>
        <span v-if="placeLine" class="dim">{{ placeLine }}</span>
      </div>
    </section>

    <section
      class="win deck scroll"
      :class="{ fit: sheet.deck.length <= 18 }"
      :aria-label="t('handheld.cardBattle.deck')"
    >
      <button
        v-for="card in sheet.deck"
        :key="card.key"
        type="button"
        class="thin card"
        :aria-label="card.name"
        @click="go('wikiCarta', card.cardId)"
      >
        <img v-if="artOf(card.imageId)" :src="artOf(card.imageId)!" alt="" class="card-art" />
        <img
          v-else-if="artOf(card.imageId) === null"
          :src="iconUrl('unknown')"
          alt=""
          class="card-missing"
        />
        <span v-else class="card-art"></span>
        <span class="gold">x{{ card.quantity }}</span>
      </button>
      <div v-if="sheet.deck.length === 0" class="deck-empty">
        <img :src="iconUrl('unknown')" alt="" class="card-missing" />
        <span class="dim">{{ t("handheld.cardBattle.noDeck") }}</span>
      </div>
    </section>

    <section class="prizes">
      <button
        type="button"
        class="slot prize"
        :disabled="sheet.booster === null"
        @click="sheet.booster && go('wikiSobre', sheet.booster.id)"
      >
        <img :src="iconUrl('card')" alt="" class="prize-icon" />
        <span class="prize-text">
          <span class="dim">{{ t("handheld.cardBattle.prize") }}</span>
          <span class="gold prize-name" :class="{ long: boosterLine.length > PrizeChars }">{{
            boosterLine
          }}</span>
        </span>
      </button>
      <button type="button" class="slot prize" :disabled="sheet.npcId === null" @click="openRival">
        <span class="prize-icon prize-rival">
          <img :src="rivalImage" alt="" />
        </span>
        <span class="prize-text">
          <span class="dim">{{ t("handheld.cardBattle.rival") }}</span>
          <span class="ice prize-name" :class="{ long: rivalLine.length > PrizeChars }">{{
            rivalLine
          }}</span>
        </span>
      </button>
    </section>
  </div>
</template>

<style scoped>
.rival {
  position: relative;
  height: 122px;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* The picture is a photograph-sized render: smoothing it keeps it from turning into noise. */
  image-rendering: auto;
}

.tamer {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
}

.tamer-image {
  display: block;
}

.rival-text {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 82px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  font-size: 10px;
}

.rival-title {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.rival-text span {
  overflow: hidden;
  white-space: nowrap;
}

.deck {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-auto-rows: 78px;
  gap: 3px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Up to eighteen cards fill the frame exactly as the design draws them. */
.deck.fit {
  grid-template-rows: repeat(3, minmax(0, 1fr));
  overflow: hidden;
}

.card {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 10px;
}

.card-art {
  width: 40px;
  height: 46px;
}

.card-missing {
  width: 22px;
  height: 22px;
}

.deck-empty {
  grid-column: 1 / -1;
  grid-row: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.prizes {
  height: 62px;
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}

.prize {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  text-align: left;
}

.prize-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

/* The sprite keeps its own size, centred on the 44 px the icon of the other prize takes. */
.prize-rival {
  display: flex;
  align-items: center;
  justify-content: center;
}

.prize-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.prize-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.prize-name.long {
  font-size: 10px;
}
</style>
