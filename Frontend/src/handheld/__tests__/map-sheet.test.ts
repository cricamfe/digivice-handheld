import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { StateDTO } from "@/events/dto/state.dto";
import "@/extensions/math.extensions";
import { useGameStore } from "@/stores/use-game-store";
import initialState from "../../../../Android/core/src/test/resources/native/initial-state.json";
import { loadGameAssets } from "../game-assets";
import { buildMapSheet } from "../map-sheet";
import { gameManifest, gameResponse } from "./game-assets.fixture";

const circuitMaps = [
  ["02E8", 865],
  ["02E9", 870],
  ["02EA", 875],
  ["02EB", 880],
  ["02EC", 885],
  ["02ED", 890],
  ["02EE", 895],
] as const;
const otherMaps = [201, 233, 261, 415, 815, 920, 927, 934];
const stages = [...circuitMaps.map(([, stage]) => stage), ...otherMaps];
const assets = Object.fromEntries(stages.map((stage) => [`maps/${stage}`, `maps/${stage}.webp`]));
const texts = { t: () => "Placa de circuito", te: (key: string) => key === "map.02EC" };

describe("Stage-backed maps in the live field", () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    useGameStore().setInitialState(initialState as unknown as StateDTO);
    vi.stubGlobal("fetch", async (path: string) => gameResponse(path, gameManifest(assets)));
    expect(await loadGameAssets()).toBe("ready");
  });
  afterEach(() => vi.unstubAllGlobals());

  it.each(circuitMaps)("uses the localized title and field image for %s", (mapId, stage) => {
    const state = useGameStore().currentState!;
    state.player.mapId = mapId;
    expect(buildMapSheet(state, texts)).toMatchObject({
      placeId: mapId,
      mapId,
      name: "Placa de circuito",
      imageUrl: `/game/maps/${stage}.webp`,
    });
  });

  it.each([
    ["022D", 415],
    ["0278", 233],
    ["027F", 261],
    ["02DE", 815],
  ])("opens the extracted scenery for %s", (mapId, stage) => {
    const state = useGameStore().currentState!;
    state.player.mapId = mapId;
    expect(buildMapSheet(state, texts).imageUrl).toBe(`/game/maps/${stage}.webp`);
  });

  it.each([
    ["0270", 201, 920],
    ["0278", 233, 927],
    ["027F", 261, 934],
  ])("updates the map image for %s when a chapter change arrives", (mapId, main, extra) => {
    const store = useGameStore();
    store.syncPlayer({ mapId });
    expect(buildMapSheet(store.currentState!, texts).imageUrl).toBe(`/game/maps/${main}.webp`);
    store.syncPlayer({ extraChapter: true });
    expect(buildMapSheet(store.currentState!, texts).imageUrl).toBe(`/game/maps/${extra}.webp`);
    store.syncPlayer({ extraChapter: false });
    expect(buildMapSheet(store.currentState!, texts).imageUrl).toBe(`/game/maps/${main}.webp`);
  });

  it("keeps an unknown mode unnamed and without an image", () => {
    const state = useGameStore().currentState!;
    state.player.mapId = "FFFF";
    expect(buildMapSheet(state, texts)).toMatchObject({ name: null, imageUrl: null });
  });
});
