<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useGameState } from "@/composables/use-game-state";
import { FooterPresenter } from "@/presenters/footer/footer.presenter";
import { moneyUnit } from "../game-words";

const { t } = useI18n();
const gameState = useGameState();

const bits = computed(() => gameState.value.player.bits);
const charisma = computed(() => FooterPresenter.getPartyCharisma(gameState.value.party));
const level = computed(() => FooterPresenter.getPartyLevel(gameState.value.party));
</script>

<template>
  <div class="thin status">
    <span
      ><span class="dim label">{{ moneyUnit(t) }}</span
      >{{ bits }}</span
    >
    <span
      ><span class="dim label">{{ t("handheld.status.charisma") }}</span
      >{{ charisma }}</span
    >
    <span
      ><span class="dim label">{{ t("handheld.status.level") }}</span
      >{{ level }}</span
    >
  </div>
</template>

<style scoped>
/* One space of the pixel font (10px) between the label and its figure. */
.label {
  margin-right: 10px;
}
</style>
