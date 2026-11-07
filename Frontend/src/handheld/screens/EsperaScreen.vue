<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl, spriteUrl } from "../assets";
import { waitLooks, type WaitKind } from "../connection";
import { useLongPress } from "../use-long-press";

const props = defineProps<{
  /** Why there is nothing else to show. */
  kind: WaitKind;
}>();

const emit = defineEmits<{
  /** The button of the state was pressed (only `inicio` has one). */
  action: [];
  /** The screen was held. It has no nav bar to hold, so this is how the app panel (Ocultar, Salir) is reached. */
  longpress: [];
}>();

const { t } = useI18n();
const look = computed(() => waitLooks[props.kind]);
const key = (name: string) => `handheld.wait.${props.kind}.${name}`;
const steps = computed(() =>
  Array.from({ length: look.value.steps }, (_, index) => ({
    number: `${index + 1}.`,
    text: t(key(`step${index + 1}`)),
  })),
);

const { onPointerDown, onPointerMove, cancel } = useLongPress(() => emit("longpress"));
</script>

<template>
  <div
    class="view espera"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="cancel"
    @pointercancel="cancel"
    @pointerleave="cancel"
    @contextmenu.prevent
  >
    <section class="win card" :data-kind="kind">
      <div class="side">
        <img v-if="spriteUrl(3)" :src="spriteUrl(3)!" alt="" class="agumon" />
        <img :src="iconUrl(look.icon)" alt="" class="mark" />
      </div>
      <div class="body">
        <span class="dim">{{ t(key("kicker")) }}</span>
        <span class="gold title">{{ t(key("title")) }}</span>
        <span class="text">{{ t(key("text")) }}</span>
        <div v-if="steps.length > 0" class="thin steps">
          <span v-for="step in steps" :key="step.number" class="step">
            <span class="gold">{{ step.number }}</span>
            <span>{{ step.text }}</span>
          </span>
        </div>
        <span v-if="look.note" class="text dim">{{ t(key("note")) }}</span>
        <button v-if="look.action" type="button" class="btn-on action" @click="emit('action')">
          {{ t(key("action")) }}
        </button>
      </div>
    </section>

    <div class="status credits">
      <span>{{ t("handheld.wait.credits.author") }}</span>
      <span>{{ t("handheld.wait.credits.port") }}</span>
    </div>
  </div>
</template>

<style scoped>
.espera {
  /* The hold is read from pointer events, so the browser must not start a pan, zoom or selection. */
  touch-action: none;
}

.card {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
}

.side {
  width: 152px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.agumon {
  width: 128px;
  height: 128px;
}

.mark {
  width: 44px;
  height: 44px;
}

.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  font-size: 10px;
}

.title {
  font-size: 16px;
  line-height: 1.4;
}

.text {
  line-height: 1.7;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step {
  display: flex;
  gap: 8px;
  line-height: 1.5;
}

.action {
  height: 52px;
  flex-shrink: 0;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.credits {
  color: #8fb4e8;
}
</style>
