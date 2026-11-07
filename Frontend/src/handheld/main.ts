import { createApp } from "vue";
import { createPinia } from "pinia";

import App from "./App.vue";
import LauncherApp from "./LauncherApp.vue";
import "@/extensions/math.extensions";

import i18n from "@/i18n/handheld";
import { initializeGameEventHandlers } from "@/events/game-event-handlers";
import { bridgeService, installBridge, signalReady } from "./bridge.service";
import { startLanguage } from "./language";
import { loadGameAssets } from "./game-assets";
import { loadBattleCatalog } from "./game-techniques";

/**
 * The same page serves three windows. The overlay on the lower screen loads it as it is. On the upper screen the
 * launcher activity loads it with `?launcher` for the first-run permission page, and the setup activity with
 * `?launcher&setup` for the preparation wizard; neither needs game events.
 */
const isLauncher = new URLSearchParams(window.location.search).has("launcher");

void start();

async function start(): Promise<void> {
  if (!isLauncher) {
    const assetState = await loadGameAssets();
    if (assetState !== "ready") {
      console.warn(`[Digivice] Game assets: ${assetState}`);
    } else if (!(await loadBattleCatalog())) {
      console.warn("[Digivice] Battle catalog unavailable");
    }
  }
  await startLanguage().catch(console.error);
  if (isLauncher) {
    createApp(LauncherApp).use(i18n).mount("#app");
  } else {
    startOverlay();
  }
}

function startOverlay(): void {
  const app = createApp(App);

  app.use(createPinia());
  app.use(i18n);

  initializeGameEventHandlers(bridgeService);

  // The receiver exists before readiness is signalled, so no native event can be lost.
  installBridge();

  app.mount("#app");

  bridgeService.emit("HubConnectionStatusChanged", { isConnected: true });

  signalReady();

  if (import.meta.env.DEV) {
    const query = new URLSearchParams(window.location.search);
    const mock = query.get("mock");
    if (mock) {
      const mockCase = query.get("case");
      const screen = query.get("screen");
      const screenParam = query.get("param");
      const recover = query.get("recover");
      import("./mock.service").then(
        async ({ applyMockConnection, loadMockSnapshot, openMockScreen }) => {
          await loadMockSnapshot(mock, mockCase);
          if (screen) {
            openMockScreen(screen, screenParam);
          }
          applyMockConnection(mockCase, recover === null ? null : Number(recover));
        },
      );
    }
  }
}
