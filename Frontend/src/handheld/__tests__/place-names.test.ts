import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import enFallbacks from "@/i18n/locales/en-US/handheld-fallbacks.json";
import esUi from "@/i18n/locales/es-ES/ui.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const knownWords = new Set(textKeys.words.map((word) => word.key));

/** Made-up words in the shape of the disc's, for every place reference the extractor publishes: no game text. */
const placeWords = Object.fromEntries(
  textKeys.words
    .filter((word) => word.table === "stname" || word.table === "starea")
    .map((word) => [word.key, `Place ${word.key}`]),
);

async function setup(words: Record<string, string> = placeWords) {
  gameAssetUrl.mockImplementation((kind: string, locale: string) => `/game/${kind}/${locale}.json`);
  vi.stubGlobal("fetch", async () => ({ ok: true, json: async () => ({ messages: {}, words }) }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage("en-US");
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  return {
    texts,
    view: await import("../map-view"),
    places: await import("../place-words"),
    docks: await import("../seabed-docks"),
    copies: await import("../server-copies"),
  };
}

describe("the names of places come from the disc's place names", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("refers only to lines the extractor publishes", async () => {
    const { view, places, docks, copies } = await setup();
    const references = [
      ...Object.values(view.SectorWords),
      ...Object.values(view.SavingWords),
      ...Object.values(places.ScreenPlaces),
      ...Object.values(docks.SeabedDockPlaces).map((dock) => dock.word),
      ...Object.values(copies.ServerCopyData).map((copy) => copy.word),
    ];
    expect(references.filter((reference) => !knownWords.has(reference))).toEqual([]);
  });

  it("names the sectors of the world map by the game's sector names", async () => {
    const { texts, view } = await setup();
    expect(view.locationName(texts, "0001")).toBe("Place starea:12");
    expect(view.locationName(texts, "0004")).toBe("Place starea:15");
  });

  it("names a map the app's texts once named by hand by the game's own line", async () => {
    const { texts, view } = await setup();
    expect(view.locationName(texts, "024E")).toBe("Place stname:75");
    expect(view.locationName(texts, "020A")).toBe("Place stname:8");
    expect(view.locationName(texts, "0257")).toBe("Place stname:84");
  });

  it("says where the player saves with the place of the game", async () => {
    const { texts, view } = await setup();
    expect(view.locationName(texts, "0C01")).toBe("Saving at Place stname:8");
    expect(view.locationName(texts, "0C13")).toBe("Saving at Place stname:88");
  });

  it("leaves a saving scene or a sector unnamed when the extracted text lacks the line", async () => {
    const { texts, view } = await setup({});
    expect(view.locationName(texts, "0C01")).toBeNull();
    expect(view.locationName(texts, "0001")).toBeNull();
  });

  it("names the places of the screens' titles from the disc, and a generic word without their line", async () => {
    const named = await setup();
    expect(named.places.placeWord(named.texts, "mobiusDesert")).toBe("Place stname:85");
    expect(
      named.texts.t("handheld.map.seabed", {
        place: named.places.placeWord(named.texts, "seabed"),
      }),
    ).toBe("Place stname:133 Routes");
    vi.resetModules();
    const bare = await setup({});
    expect(bare.places.placeWord(bare.texts, "mobiusDesert")).toBe("Desert");
    expect(bare.places.placeWord(bare.texts, "cargoTower")).toBe("the tower");
  });

  it("writes no place of the game in the app's own map names, in any language", () => {
    const sectorsAndSaves = [
      "0001",
      "0002",
      "0003",
      "0004",
      "0C01",
      "0C02",
      "0C03",
      "0C04",
      "0C12",
      "0C13",
      "0C14",
    ];
    const named = ["020A", "020C", "024E", "024F", "0250", "0251", "0257"];
    for (const messages of [enFallbacks, esUi, frUi, deUi, itUi]) {
      const maps = (messages as { map?: Record<string, string> }).map ?? {};
      expect([...sectorsAndSaves, ...named].filter((id) => id in maps)).toEqual([]);
    }
  });
});
