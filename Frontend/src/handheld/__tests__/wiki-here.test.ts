import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import type { StateDTO } from "@/events/dto/state.dto";
import "@/extensions/math.extensions";
import { useGameStore } from "@/stores/use-game-store";
import initialState from "../../../../Android/core/src/test/resources/native/initial-state.json";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

/** Made-up words in the shape of the disc's, for every place reference the extractor publishes: no game text. */
const placeWords = Object.fromEntries(
  textKeys.words
    .filter((word) => word.table === "stname" || word.table === "starea")
    .map((word) => [word.key, `Place ${word.key}`]),
);

async function setup() {
  gameAssetUrl.mockImplementation((kind: string, locale: string) => `/game/${kind}/${locale}.json`);
  vi.stubGlobal("fetch", async () => ({
    ok: true,
    json: async () => ({ messages: {}, words: placeWords }),
  }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage("en-US");
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  const { buildWikiIndex } = await import("../wiki-list-sheet");
  const { mapName } = await import("../map-view");
  setActivePinia(createPinia());
  const store = useGameStore();
  store.setInitialState(initialState as unknown as StateDTO);
  return { texts, buildWikiIndex, mapName, state: store.currentState! };
}

describe("the Wiki index names the place the player is in", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("names Amaterasu City by its own name, not by the Asuka City it copies", async () => {
    const { texts, buildWikiIndex, mapName, state } = await setup();
    // 0274 is Amaterasu City, the second server's copy of Asuka City (0205).
    state.player.mapId = "0274";
    const place = buildWikiIndex(state, texts).here.place;
    expect(place).toBe("Place stname:118");
    expect(place).toBe(mapName(texts, "0274"));
    expect(place).not.toBe(mapName(texts, "0205"));
  });

  it("names the place a battle started in, also on the second server", async () => {
    const { texts, buildWikiIndex, state } = await setup();
    state.player.previousMapId = "0274";
    state.player.mapId = "0600";
    expect(buildWikiIndex(state, texts).here.place).toBe("Place stname:118");
  });

  it("names a map of the first server as before", async () => {
    const { texts, buildWikiIndex, mapName, state } = await setup();
    state.player.mapId = "0205";
    expect(buildWikiIndex(state, texts).here.place).toBe(mapName(texts, "0205"));
  });
});
