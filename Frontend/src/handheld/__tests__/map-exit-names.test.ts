import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

/** Made-up texts in the shape of the disc's: the messages by path and the words by reference, nothing of the game. */
const mapMessages = Object.fromEntries(
  textKeys.messages
    .filter((entry) => entry.path[0] === "map")
    .map((entry) => [entry.path[1], `Map ${entry.path[1]}`]),
);
const words = Object.fromEntries(
  textKeys.words
    .filter((word) => word.table === "stname")
    .map((word) => [word.key, `Place ${word.key}`]),
);

async function setup() {
  gameAssetUrl.mockImplementation((kind: string, locale: string) => `/game/${kind}/${locale}.json`);
  vi.stubGlobal("fetch", async () => ({
    ok: true,
    json: async () => ({
      messages: { map: { ...mapMessages, "02E1": "Seabed", "02EC": "Circuit" } },
      words,
    }),
  }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage("en-US");
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  return {
    texts,
    view: await import("../map-view"),
    exits: await import("../map-exits"),
    copies: await import("../server-copies"),
  };
}

describe("the label of an exit is the name of the map it leads to", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("numbers an exit to a Seabed map", async () => {
    const { texts, view, exits } = await setup();
    const toSeabed = Object.values(exits.MapExits)
      .flat()
      .filter((exit) => exit.to === "02E1");
    expect(toSeabed.length).toBeGreaterThan(0);
    expect(view.mapName(texts, "02E1")).toBe("Seabed 1");
    expect(view.mapName(texts, "02E5")).toBe("Map 02E5 5");
  });

  it("names an exit to a Circuit Board sector with the title all of them share", async () => {
    const { texts, view } = await setup();
    expect(view.mapName(texts, "02EB")).toBe("Circuit");
  });

  it("names an exit to a copy for the second server with the copy's own name", async () => {
    const { texts, view, exits, copies } = await setup();
    const toCopy = Object.values(exits.MapExits)
      .flat()
      .find((exit) => copies.ServerCopyData[exit.to] !== undefined);
    expect(toCopy).toBeDefined();
    const copy = copies.ServerCopyData[toCopy!.to]!;
    expect(view.mapName(texts, toCopy!.to)).toBe(`Place ${copy.word}`);
    expect(view.mapName(texts, toCopy!.to)).not.toBe(
      view.mapName(texts, view.originalOf(toCopy!.to)),
    );
  });

  it("names a map the messages leave out by its reference to the place names", async () => {
    const { texts, view, exits } = await setup();
    expect(view.mapName(texts, "0261")).toBe(`Place ${exits.PlaceWords["0261"]}`);
  });

  it("leaves no exit without a name", async () => {
    const { texts, view, exits } = await setup();
    const unnamed = new Set<string>();
    for (const exit of Object.values(exits.MapExits).flat()) {
      if (view.mapName(texts, exit.to) === null) unnamed.add(exit.to);
    }
    expect([...unnamed]).toEqual([]);
  });

  it("names nothing until the disc's texts are read", async () => {
    gameAssetUrl.mockReturnValue(null);
    const view = await import("../map-view");
    const { default: i18n } = await import("@/i18n/handheld");
    const texts = {
      t: i18n.global.t,
      te: i18n.global.te,
    } as unknown as import("../map-view").Texts;
    expect(view.mapName(texts, "0270")).toBeNull();
    expect(view.mapName(texts, "0261")).toBeNull();
  });
});
