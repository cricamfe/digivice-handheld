<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import WikiArt from "../../components/WikiArt.vue";
import WikiHeader from "../../components/WikiHeader.vue";
import { iconUrl, spriteUrl } from "../../assets";
import { useScreen } from "../../router";
import { useTexts } from "../../texts";
import { numberOf } from "../../wiki-common";
import { buildEquipmentSheet } from "../../wiki-drop-sheet";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const texts = useTexts();
const { go, param } = useScreen();
const gameState = useGameState();

/** The equipment to show: its number in the database (the auction rows send a number). */
const equipmentId = computed(() => numberOf(param.value));
const sheet = computed(() =>
  equipmentId.value === null
    ? null
    : buildEquipmentSheet(equipmentId.value, gameState.value, texts),
);
</script>

<template>
  <div v-if="sheet" class="view">
    <WikiHeader :icon="sheet.icon" :title="sheet.name" :detail="sheet.typeLabel" />

    <section class="win note">
      <span v-if="sheet.note">{{ sheet.note }}</span>
      <span v-else class="no-note">
        <img :src="iconUrl('unknown')" alt="" class="icon" />{{
          t("handheld.wiki.equipment.noNote")
        }}
      </span>
    </section>

    <section
      class="attributes"
      :style="{
        gridTemplateColumns: `repeat(${Math.max(1, sheet.attributes.length)}, minmax(0, 1fr))`,
      }"
    >
      <div v-for="attribute in sheet.attributes" :key="attribute.key" class="slot attribute">
        <img :src="iconUrl(attribute.key)" alt="" class="icon" />
        <span class="dim">{{ attribute.label }}</span>
        <span :class="attribute.isPenalty ? 'down' : 'gold'" class="value">{{
          attribute.value
        }}</span>
      </div>
      <div v-if="sheet.attributes.length === 0" class="slot attribute">
        <img :src="iconUrl('unknown')" alt="" class="icon" />
        <span class="dim">{{ t("handheld.wiki.equipment.noAttributes") }}</span>
      </div>
    </section>

    <section class="win equip">
      <span class="gold">{{
        t(sheet.forAll ? "handheld.wiki.equipment.equipAll" : "handheld.wiki.equipment.equip")
      }}</span>
      <div class="partners">
        <span v-for="partner in sheet.equipable" :key="partner.id" class="partner">
          <img
            v-if="spriteUrl(partner.id)"
            :src="spriteUrl(partner.id)!"
            :alt="partner.name"
            class="sprite"
          />
          <img v-else :src="iconUrl('unknown')" alt="" class="icon" />
          {{ partner.name }}
        </span>
        <span v-if="sheet.equipable.length === 0" class="partner">
          <img :src="iconUrl('unknown')" alt="" class="mark" />
          {{ t("handheld.wiki.equipment.nobody") }}
        </span>
      </div>
    </section>

    <section class="sources scroll">
      <button
        v-for="source in sheet.sources"
        :key="source.key"
        type="button"
        class="slot source"
        @click="go(source.target.screen, source.target.param)"
      >
        <WikiArt :picture="{ url: source.imageUrl }" icon="unknown" :w="44" :h="44" />
        <span class="label">
          <span class="gold name">{{ source.name }}</span>
          <span class="dim">{{
            [
              t("enemy.drop"),
              source.level === null ? null : t("handheld.wiki.level", { level: source.level }),
              source.isBoss ? t("handheld.wiki.boss") : null,
              source.only,
            ]
              .filter((part) => part !== null)
              .join(" · ")
          }}</span>
        </span>
      </button>
      <div v-if="sheet.sources.length === 0" class="lock source">
        <img :src="iconUrl('cross')" alt="" class="icon" />
        <span>{{ t("enemy.droppedByNone") }}</span>
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

.down {
  color: #ffb09a;
}

.note {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  font-size: 10px;
  line-height: 1.5;
}

.no-note {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attributes {
  height: 92px;
  flex-shrink: 0;
  display: grid;
  gap: 3px;
}

.attribute {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 10px;
}

.value {
  font-size: 12px;
}

.equip {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
}

/* Eight names of seven letters are 560 px at 10 px, in a line of 572: they do not fit in one row, so the partners go in
   two rows of four, each with its 32 px sprite beside the name (the design's 64 px portraits above it did not fit). */
.partners {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-content: center;
  gap: 10px 6px;
}

.partner {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sprite {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.sources {
  height: 70px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.source {
  height: 70px;
  flex-shrink: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  font-size: 10px;
}

.label {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.label .dim {
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
