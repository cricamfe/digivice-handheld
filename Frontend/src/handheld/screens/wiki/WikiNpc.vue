<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import { iconUrl } from "../../assets";
import { useScreen } from "../../router";
import { itemWord, moneyUnit, NpcItemWords } from "../../game-words";
import { useTexts } from "../../texts";
import { breakable } from "../../wiki-common";
import {
  buildNpcCardBattle,
  buildNpcDigimonBattle,
  buildNpcSheet,
  type NpcOption,
} from "../../wiki-npc-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, back, param } = useScreen();
const gameState = useGameState();

/** The NPC to show: its id, or `{ npc, option }` when the caller knows which battle to open. */
const request = computed(() => {
  const value = param.value;
  if (typeof value === "string" || typeof value === "number") {
    return { npc: String(value), option: null };
  }

  if (value !== null && typeof value === "object") {
    return {
      npc: String(value.npc ?? ""),
      option: typeof value.option === "string" ? value.option : null,
    };
  }

  return { npc: "", option: null };
});

const sheet = computed(() =>
  buildNpcSheet(request.value.npc, request.value.option, gameState.value, texts),
);

/** Whether a word of the name is wider than the line at 16 px (10 letters). */
const isLongName = computed(
  () => sheet.value?.name.split(/\s+/).some((word) => word.length > 10) ?? false,
);

const picked = ref<string | null>(null);
const member = ref(0);

// A different NPC starts from the battle the sheet picks for it.
watch(
  request,
  () => {
    picked.value = null;
    member.value = 0;
  },
  { immediate: true },
);

const selected = computed(
  () =>
    sheet.value?.options.find((option) => option.id === (picked.value ?? sheet.value?.selected)) ??
    null,
);

function pick(option: NpcOption): void {
  picked.value = option.id;
  member.value = 0;
}

const cardBattle = computed(() =>
  selected.value?.kind === "card" && selected.value.state !== "locked"
    ? buildNpcCardBattle(request.value.npc, selected.value.id, texts)
    : null,
);

const digimonBattle = computed(() =>
  selected.value?.kind === "digimon" && selected.value.state !== "locked"
    ? buildNpcDigimonBattle(request.value.npc, selected.value.id)
    : null,
);

const current = computed(() => digimonBattle.value?.members[member.value] ?? null);

function nextMember(): void {
  const count = digimonBattle.value?.members.length ?? 0;
  member.value = count === 0 ? 0 : (member.value + 1) % count;
}

/** Frame of a battle button: lit when chosen, locked or done as its state says. */
function frame(option: NpcOption): string {
  if (option.id === selected.value?.id) {
    return "slot-on";
  }

  return { open: "slot", done: "good", locked: "lock" }[option.state];
}

function iconOf(option: NpcOption): string {
  if (option.state === "locked") {
    return "locked";
  }

  return option.state === "done" ? "check" : option.kind === "card" ? "card" : "battle";
}
</script>

<template>
  <div v-if="sheet" class="view">
    <section class="body">
      <aside class="win side">
        <button type="button" class="btn back" :aria-label="t('handheld.back')" @click="back()">
          <img :src="iconUrl('back')" alt="" class="icon" />
        </button>
        <WikiArt :picture="{ url: sheet.imageUrl }" :alt="sheet.name" :w="60" :h="102" />
        <span class="gold name" :class="{ long: isLongName }">{{ sheet.name }}</span>
        <span class="dim">{{ sheet.kindLabel }}</span>
        <button
          v-if="sheet.place"
          type="button"
          class="thin place"
          @click="go('mapa', sheet.place.id)"
        >
          <img :src="iconUrl('place')" alt="" class="icon" />
          <span class="ice place-name">{{ sheet.place.name }}</span>
        </button>
        <span v-if="sheet.needsFolderBag" class="dim hint">{{
          t("handheld.wiki.npc.folderBag", { item: itemWord(texts, NpcItemWords.folderBag) })
        }}</span>
      </aside>

      <div class="main">
        <div class="scroll options">
          <button
            v-for="option in sheet.options"
            :key="option.id"
            type="button"
            class="option"
            :class="frame(option)"
            :aria-current="option.id === selected?.id ? 'true' : undefined"
            @click="pick(option)"
          >
            <img :src="iconUrl(iconOf(option))" alt="" class="icon" />
            <span class="option-text">
              <span>{{ t(`npc.battle.${option.kind}`) }}</span>
              <span class="gold">{{ option.range }}</span>
            </span>
          </button>
          <div v-if="sheet.options.length === 0" class="lock option">
            <img :src="iconUrl('unknown')" alt="" class="icon" />
            <span>{{ t("handheld.wiki.npc.noBattles") }}</span>
          </div>
        </div>

        <div class="win detail">
          <template v-if="selected && selected.state === 'locked'">
            <div class="locked">
              <img :src="iconUrl('locked')" alt="" class="mark" />
              <span class="dim">{{
                selected.isClosed ? t("handheld.wiki.npc.closed") : t("handheld.wiki.npc.need")
              }}</span>
              <span v-if="selected.need" class="gold need">{{ selected.need }}</span>
              <span v-else class="gold need">{{ t("handheld.wiki.noData") }}</span>
            </div>
          </template>

          <template v-else-if="cardBattle">
            <span class="gold">{{ cardBattle.title }}</span>
            <div class="scroll folder">
              <button
                v-for="entry in cardBattle.cards"
                :key="entry.card.id"
                type="button"
                class="folder-card"
                :aria-label="entry.card.name"
                @click="go(entry.card.target.screen, entry.card.target.param)"
              >
                <WikiArt
                  :picture="{ card: entry.card.imageId }"
                  :alt="entry.card.name"
                  :w="40"
                  :h="46"
                />
                <span class="gold">x{{ entry.quantity }}</span>
              </button>
            </div>
            <button
              v-if="cardBattle.prize"
              type="button"
              class="thin-plate prize"
              @click="go(cardBattle.prize.target.screen, cardBattle.prize.target.param)"
            >
              <span class="dim">{{ t("handheld.wiki.npc.prize") }}</span>
              <span class="gold">{{ cardBattle.prize.name }}</span>
            </button>
            <div v-else class="thin-plate prize">
              <span class="dim">{{ t("handheld.wiki.npc.prize") }}</span>
              <span>{{ t("handheld.wiki.npc.noPrize") }}</span>
            </div>
          </template>

          <template v-else-if="digimonBattle">
            <span class="gold">{{ t("handheld.wiki.npc.team") }}</span>
            <div v-if="current" class="member-row">
              <button
                type="button"
                class="thin member"
                @click="go(current.target.screen, current.target.param)"
              >
                <WikiArt :picture="{ url: current.imageUrl }" icon="unknown" :w="88" :h="88" />
                <span class="member-text">
                  <span class="drop member-name">{{ breakable(current.name) }}</span>
                  <span class="dim">{{
                    `${t("handheld.wiki.level", { level: current.level })} · ${t("handheld.bar.hp")} ${current.hp}`
                  }}</span>
                </span>
              </button>
              <button
                v-if="digimonBattle.members.length > 1"
                type="button"
                class="btn next"
                :aria-label="t('handheld.wiki.npc.nextMember')"
                @click="nextMember"
              >
                <img :src="iconUrl('arrow')" alt="" class="icon" />
                <span>{{ member + 1 }}/{{ digimonBattle.members.length }}</span>
              </button>
            </div>
            <div v-else class="none">
              <img :src="iconUrl('unknown')" alt="" class="icon" />
              <span>{{ t("handheld.wiki.noData") }}</span>
            </div>
            <div class="rewards">
              <span class="thin-plate reward"
                ><span class="dim">{{ t("enemy.baseExp") }}</span
                ><span class="gold">{{ digimonBattle.exp }}</span></span
              >
              <span class="thin-plate reward"
                ><span class="dim">{{ t("enemy.baseDvexp") }}</span
                ><span class="gold">{{ digimonBattle.dvexp }}</span></span
              >
              <span class="thin-plate reward"
                ><span class="dim">{{ moneyUnit(t) }}</span
                ><span class="gold">{{ digimonBattle.bits }}</span></span
              >
            </div>
          </template>

          <div v-else class="locked">
            <img :src="iconUrl('unknown')" alt="" class="mark" />
            <span class="dim">{{ t("handheld.wiki.noData") }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
  <div v-else class="view">
    <div class="win empty">
      <button type="button" class="btn back" :aria-label="t('handheld.back')" @click="back()">
        <img :src="iconUrl('back')" alt="" class="icon" />
      </button>
      <div class="empty-body">
        <img :src="iconUrl('unknown')" alt="" class="mark" />
        <span class="drop">{{ t("handheld.wiki.unknown") }}</span>
      </div>
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

.body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

/* 200 px: the line of the place holds 12 letters, which "Headquarters" needs (it was 176 px with room for 9). */
.side {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
}

.back {
  align-self: flex-start;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.name {
  max-width: 100%;
  font-size: 16px;
  text-align: center;
  overflow-wrap: anywhere;
}

/* A name with a word over 10 letters ("KingDivermon") is written at 12 px, where the 168 px of the line hold 14. */
.name.long {
  font-size: 12px;
}

.place {
  align-self: stretch;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.place-name {
  min-width: 0;
  overflow-wrap: anywhere;
  line-height: 1.4;
}

.hint {
  line-height: 1.6;
  text-align: center;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.options {
  max-height: 116px;
  flex-shrink: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: 55px;
  gap: 3px;
  font-size: 10px;
}

.option {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  text-align: left;
}

.option-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
}

.folder {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-auto-rows: 100px;
  gap: 2px;
  align-content: start;
}

.folder-card {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.prize {
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.member-row {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 3px;
}

.member {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.member-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-name {
  font-size: 12px;
  line-height: 1.3;
}

.next {
  width: 60px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.rewards {
  height: 56px;
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 3px;
}

.reward {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.locked,
.none {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

.need {
  line-height: 1.6;
}

.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.empty-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
