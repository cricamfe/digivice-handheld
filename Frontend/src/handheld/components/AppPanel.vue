<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import AboutPanel from "./AboutPanel.vue";
import { chooseLanguage, language, Languages, type Locale } from "../language";
import { createLogger } from "@/events/logger";

const emit = defineEmits<{ close: [] }>();

const { t } = useI18n();
const logger = createLogger("AppPanel", "#e67e22");

/** Calls the native side. In a plain browser there is no `DigiviceNative`, so nothing happens. */
function callNative(action: "hide" | "exit"): void {
  try {
    window.DigiviceNative?.[action]?.();
  } catch (err) {
    logger.error(`DigiviceNative.${action} failed.`, err);
  }
}

function hide(): void {
  emit("close");
  callNative("hide");
}

function exit(): void {
  emit("close");
  callNative("exit");
}

/** Whether the panel shows the languages instead of its actions. */
const picking = ref(false);
/** Whether the panel shows the version, the licences and where the source code is. */
const about = ref(false);
const languageName = (locale: Locale): string =>
  Languages.find((entry) => entry.locale === locale)?.name ?? locale;

async function pick(locale: Locale): Promise<void> {
  await chooseLanguage(locale);
  picking.value = false;
}

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape") {
    emit("close");
  }
}

onMounted(() => window.addEventListener("keydown", onKeyDown));
onBeforeUnmount(() => window.removeEventListener("keydown", onKeyDown));
</script>

<template>
  <div class="backdrop" @click.self="emit('close')">
    <AboutPanel v-if="about" @back="about = false" />
    <div
      v-else
      class="win panel-menu"
      role="dialog"
      aria-modal="true"
      :aria-label="t('handheld.panel.title')"
    >
      <div class="thin-plate panel-head">
        <img :src="iconUrl('party')" alt="" class="panel-icon" />
        <span class="gold panel-title">{{ t("handheld.panel.title") }}</span>
      </div>
      <template v-if="picking">
        <button
          v-for="entry in Languages"
          :key="entry.locale"
          type="button"
          class="panel-action"
          :class="entry.locale === language ? 'btn-on' : 'btn'"
          :aria-pressed="entry.locale === language"
          @click="pick(entry.locale)"
        >
          <img :src="iconUrl(entry.locale === language ? 'check' : 'journal')" alt="" />
          <span>{{ entry.name }}</span>
        </button>
      </template>
      <template v-else>
        <button type="button" class="btn panel-action" @click="picking = true">
          <img :src="iconUrl('journal')" alt="" />
          <span>{{ languageName(language) }}</span>
        </button>
        <button type="button" class="btn panel-action" @click="hide">
          <img :src="iconUrl('back')" alt="" />
          <span>{{ t("handheld.panel.hide") }}</span>
        </button>
        <button type="button" class="btn panel-action" @click="exit">
          <img :src="iconUrl('escape')" alt="" />
          <span>{{ t("handheld.panel.exit") }}</span>
        </button>
        <button type="button" class="btn panel-action" @click="about = true">
          <img :src="iconUrl('signature')" alt="" />
          <span>{{ t("handheld.panel.about") }}</span>
        </button>
        <button type="button" class="btn-on panel-action" @click="emit('close')">
          <img :src="iconUrl('cross')" alt="" />
          <span>{{ t("handheld.panel.cancel") }}</span>
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  left: 0;
  top: 0;
  width: 620px;
  height: 540px;
  display: flex;
  align-items: center;
  justify-content: center;
  /* Flat and opaque: the panel stands on plain colour, nothing is blended with the screen below. */
  background-color: #000030;
  font-family: "Press Start 2P", monospace;
  font-size: 12px;
  color: #ffffff;
}
</style>
