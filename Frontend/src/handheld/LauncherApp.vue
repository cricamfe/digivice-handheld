<script setup lang="ts">
import "./kit.css";
import { shallowRef } from "vue";
import EsperaScreen from "./screens/EsperaScreen.vue";
import PreparacionScreen from "./screens/PreparacionScreen.vue";
import { createLogger } from "@/events/logger";
import { language, setLanguage } from "./language";
import { matchLocale, parseSetupPage, type SetupAction, type SetupPage } from "./setup-view";

const logger = createLogger("Launcher", "#e67e22");

/**
 * `?launcher&setup` is the preparation wizard of SetupActivity: the activity pushes `SetupState` through
 * `window.__digivice.receive` once the page says it is ready. Without `setup` it is the first-run permission page.
 */
const query = new URLSearchParams(window.location.search);
const isSetup = query.has("setup");
const setupPage = shallowRef<SetupPage | null>(null);

function showSetup(page: SetupPage): void {
  setupPage.value = page;
  const locale = matchLocale(page.language);
  if (locale !== null && locale !== language.value) {
    void setLanguage(locale).catch((err) => logger.error("Setup language failed.", err));
  }
}

if (isSetup) {
  window.__digivice = {
    receive: (eventType, payloadJson) => {
      if (eventType !== "SetupState") return;
      const page = parseSetupPage(payloadJson);
      if (page === null) {
        logger.error("Unreadable SetupState.", payloadJson);
      } else {
        showSetup(page);
      }
    },
  };
  const fixture = query.get("setup");
  if (import.meta.env.DEV && fixture) {
    // Development only: `?launcher&setup=<state>` draws a sample state without the activity.
    void import("./setup-fixtures").then(({ setupFixture }) =>
      showSetup(setupFixture(fixture, query)),
    );
  } else {
    try {
      window.DigiviceNative?.onReady?.();
    } catch (err) {
      logger.error("DigiviceNative.onReady failed.", err);
    }
  }
}

function runSetup(action: SetupAction): void {
  try {
    window.DigiviceNative?.setupAction?.(action);
  } catch (err) {
    logger.error("DigiviceNative.setupAction failed.", err);
  }
}

/**
 * Asks the launcher activity to open the system screen where the overlay permission is granted. In a plain browser
 * (and in the overlay) there is no `openSettings`, so nothing happens.
 */
function openSettings(): void {
  try {
    window.DigiviceNative?.openSettings?.();
  } catch (err) {
    logger.error("DigiviceNative.openSettings failed.", err);
  }
}
</script>

<template>
  <template v-if="isSetup">
    <PreparacionScreen v-if="setupPage" :page="setupPage" @action="runSetup" />
    <div v-else class="stage" />
  </template>
  <!-- The upper screen is not the lower one: the page keeps its 620 x 540 shape when it fits and shrinks when it does not. -->
  <div v-else class="stage">
    <div class="screen sized">
      <EsperaScreen kind="inicio" @action="openSettings" />
    </div>
  </div>
</template>

<style scoped>
.stage {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #000030;
}

.sized {
  width: min(620px, 100vw);
  height: min(540px, 100vh);
}
</style>
