import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import tamers from "@/database/npc/tamer.json";
import npcs from "@/database/npc/npc.json";
import asukaDesert from "@/database/mobius-desert/mobius-desert-areas.json";
import enNpcs from "@/i18n/locales/en-US/npcs.json";
import esUi from "@/i18n/locales/es-ES/ui.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";
import { ComposedWords } from "../composed-messages";
import { AuctionWords } from "../game-words";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const knownWords = new Set(textKeys.words.map((word) => word.key));

const asukaNotes = asukaDesert.areas.flatMap((row) =>
  row.flatMap((area) => ("note" in area && area.note !== undefined ? [area.note] : [])),
);

/** Made-up speakers in the shape of the disc's, for every talk line these names use: no game text. */
const madeUpWords = Object.fromEntries(
  [
    ...ComposedWords,
    ...asukaNotes,
    ...Object.values(AuctionWords).flatMap((slots) => Object.values(slots)),
  ].map((reference) => [reference, `Name ${reference}`]),
);

async function setup(words: Record<string, string>) {
  gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}.json`);
  vi.stubGlobal("fetch", async () => ({ ok: true, json: async () => ({ messages: {}, words }) }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage("en-US");
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  return { texts, view: await import("../map-view"), words: await import("../game-words") };
}

describe("the tamers, NPCs and people the app names come from the speakers of the disc's talk", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("refers only to lines the extractor publishes", () => {
    expect(
      [...ComposedWords, ...asukaNotes].filter((reference) => !knownWords.has(reference)),
    ).toEqual([]);
  });

  it("keeps no name in the tables: tamers and NPCs are drawn as their field character", () => {
    const rows: Record<string, unknown>[] = [...Object.values(tamers), ...Object.values(npcs)];
    expect(rows.filter((row) => "imageName" in row)).toEqual([]);
    expect(
      rows.every((row) => typeof row.fieldActor === "string" && /^\d+-[0-7]$/.test(row.fieldActor)),
    ).toBe(true);
    expect(asukaNotes).toEqual(["talk02:829", "talk02:837", "talk02:845"]);
  });

  it("names a tamer, a leader and the scene of Dr. Kadomatsu from the disc", async () => {
    const { texts, view } = await setup(madeUpWords);
    expect(view.gameText(texts, "tamers.lucia.name")).toBe("Name talk06:70");
    expect(view.gameText(texts, "npcs.seiryuLeader.name")).toBe("Name talk01:6");
    expect(view.locationName(texts, "0D00")).toBe("Name talk05:4 / Name talk01:76");
  });

  it("numbers the troopers and soldiers of the Administration Center after their speaker", async () => {
    const { texts, view } = await setup(madeUpWords);
    expect(view.gameText(texts, "npcs.trooper1AdminCenterB1F.name")).toBe("1º Name talk01:299");
    expect(view.gameText(texts, "npcs.optionalTrooper2AdminFloor2.name")).toBe(
      "15º Name talk01:299",
    );
    expect(view.gameText(texts, "npcs.optionalSoldier1AdminFloor2.name")).toBe(
      "2º Name talk03:444",
    );
    expect(view.gameText(texts, "npcs.generalAdminFloor2.name")).toBe("Name talk01:301");
    expect(view.gameText(texts, "npcs.aoaAttacker.name")).toBe("Name talk03:439");
    expect(view.gameText(texts, "npcs.gameMasterHeadquarters.name")).toBe("Name dmg200/24:8");
  });

  it("numbers them with each language's rule", async () => {
    gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}.json`);
    vi.stubGlobal("fetch", async () => ({
      ok: true,
      json: async () => ({ messages: {}, words: madeUpWords }),
    }));
    const { setLanguage } = await import("../language");
    const { default: i18n } = await import("@/i18n/handheld");
    await setLanguage("fr-FR");
    expect(i18n.global.t("npcs.trooper2AdminCenterB1F.name")).toBe("Name talk01:299 nº2");
  });

  it("names the DRI agents of the Asuka side of the Mobius Desert from the disc", async () => {
    const { texts } = await setup(madeUpWords);
    const { buildMobiusGrid } = await import("../mobius-sheet");
    const notes = buildMobiusGrid("0258", null, texts, "asuka")
      .cells.flatMap((cell) => (cell.note === null ? [] : [cell.note]))
      .sort();
    expect(notes).toEqual(["Name talk02:829", "Name talk02:837", "Name talk02:845"]);
  });

  it("fills the people of the auctions' closing conditions", async () => {
    const { texts, words } = await setup(madeUpWords);
    expect(
      texts.t(
        "handheld.auction.closesWhen.yinYangWand",
        words.fillWords(texts, AuctionWords.yinYangWand!),
      ),
    ).toBe("Closes on challenging the real Name talk01:7 (step 48) or on taking part.");
  });

  it("names nobody when the extracted text lacks their line", async () => {
    const { texts, view } = await setup({});
    expect(view.gameText(texts, "tamers.lucia.name")).toBeNull();
    expect(view.gameText(texts, "npcs.keith.name")).toBeNull();
    expect(view.gameText(texts, "npcs.trooper1AdminFloor1.name")).toBeNull();
    expect(view.locationName(texts, "0D00")).toBeNull();
  });

  it("writes none of those names in the handheld's own texts", () => {
    expect(Object.keys(enNpcs.npcs)).not.toContain("keith");
    expect(Object.keys(enNpcs.npcs)).toEqual(["guardsHeadquarters"]);
    for (const ui of [esUi, frUi, deUi, itUi]) {
      expect("tamers" in ui).toBe(false);
      expect(ui.npcs).not.toHaveProperty("seiryuLeader");
      expect(ui.npcs).not.toHaveProperty("trooper1AdminCenterB1F");
      expect(ui.map).not.toHaveProperty("0D00");
    }
  });
});
