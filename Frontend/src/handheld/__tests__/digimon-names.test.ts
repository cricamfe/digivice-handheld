import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import enemies from "@/database/enemy.json";
import evolutions from "@/database/digievolution/digievolution.json";
import partners from "@/database/digimon.json";
import shops from "@/database/shop/card-shop.json";
import enHandheld from "@/i18n/locales/en-US/handheld.json";
import esHandheld from "@/i18n/locales/es-ES/handheld.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const knownWords = new Set(textKeys.words.map((word) => word.key));

/** Made-up names in the shape of the disc's, for every Digimon name the extractor publishes: no game text. */
const madeUpNames = (prefix: string) =>
  Object.fromEntries(
    textKeys.words
      .filter((word) => word.table === "dignam" || word.key === "talk06:791")
      .map((word) => [word.key, `${prefix} ${word.key}`]),
  );

async function setup(
  byLocale: Record<string, Record<string, string>>,
  locale: "en-US" | "es-ES" | "fr-FR",
) {
  gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}.json`);
  vi.stubGlobal("fetch", async (url: string) => ({
    ok: true,
    json: async () => ({
      messages: {},
      words: byLocale[url.slice("/game/texts/".length, -5)] ?? {},
    }),
  }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage(locale);
  return { i18n, names: await import("../digimon-names") };
}

describe("the names of the Digimon come from the disc's Digimon names", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("keeps no name in the app's tables, only the line of the game's names", () => {
    const rows: Record<string, unknown>[] = [
      ...Object.values(enemies),
      ...Object.values(evolutions),
      ...Object.values(partners),
    ];
    expect(rows.filter((row) => "name" in row)).toEqual([]);
    const lines = [
      ...rows.map((row) => row.nameId as number),
      ...Object.values(shops).flatMap((shop) => ("keeper" in shop ? [shop.keeper.nameId] : [])),
    ];
    expect(lines.filter((line) => !knownWords.has(`dignam:${line}`))).toEqual([]);
  });

  it("names a partner by the line after its id", () => {
    expect(Object.entries(partners).map(([id, raw]) => raw.nameId - Number(id))).toEqual([
      1, 1, 1, 1, 1, 1, 1, 1,
    ]);
  });

  it("reads a name in the language shown, and the English one where the language lacks the line", async () => {
    const { names } = await setup(
      { "en-US": madeUpNames("English"), "es-ES": { "dignam:212": "Spanish 212" } },
      "es-ES",
    );
    expect(names.digimonName(212)).toBe("Spanish 212");
    expect(names.digimonName(88)).toBe("English dignam:88");
  });

  it("follows a name with the app's word for a colour, and a letter as it is", async () => {
    const { names } = await setup(
      { "en-US": madeUpNames("English"), "es-ES": madeUpNames("Spanish") },
      "es-ES",
    );
    expect(names.digimonName(170, "green")).toBe("Spanish dignam:170(Verde)");
    expect(names.digimonName(240, "A")).toBe("Spanish dignam:240(A)");
  });

  it("shows a generic name with the line's number when the extracted text has none", async () => {
    const { names } = await setup({}, "en-US");
    expect(names.digimonName(212)).toBe("Digimon #212");
    expect(names.digimonName(170, "green")).toBe("Digimon #170(Green)");
  });

  it("has a word for every colour of the enemies in every language", () => {
    const colours = [
      ...new Set(
        Object.values(enemies).flatMap((raw) =>
          "variant" in raw && raw.variant.length > 1 ? [raw.variant] : [],
        ),
      ),
    ];
    expect(colours.length).toBeGreaterThan(0);
    for (const messages of [enHandheld, esHandheld, frUi, deUi, itUi]) {
      const words = (messages as { handheld: { digimon: { variant: Record<string, string> } } })
        .handheld.digimon.variant;
      expect(colours.filter((colour) => !words[colour])).toEqual([]);
    }
  });

  it("names the rivals of Duel Island from the disc with each language's rule for the order", async () => {
    const english = { ...madeUpNames("English") };
    const { i18n } = await setup({ "en-US": english, "fr-FR": madeUpNames("French") }, "fr-FR");
    expect(i18n.global.t("duelIsland.divermon2.name")).toBe("French dignam:88 nº2");
    expect(i18n.global.t("duelIsland.kingDivermon.name")).toBe("French talk06:791");
    expect(i18n.global.t("duelIsland.divermon1.name", {}, { locale: "en-US" })).toBe(
      "1º English dignam:88",
    );
  });

  it("leaves a rival of Duel Island without a name when the extracted text lacks its line", async () => {
    const { i18n } = await setup({ "en-US": {} }, "en-US");
    expect(i18n.global.te("duelIsland.divermon1.name")).toBe(false);
  });

  it("refers the rivals of Duel Island to lines the extractor publishes", async () => {
    const { ComposedWords } = await import("../composed-messages");
    expect(ComposedWords.filter((word) => !knownWords.has(word))).toEqual([]);
  });

  it("names the target of a quest's step that is a Digimon from the disc, and nothing without its line", async () => {
    const { QuestTargetWords, questTarget } = await import("../quest-target-words");
    expect(Object.values(QuestTargetWords).filter((word) => !knownWords.has(word))).toEqual([]);
    const { i18n } = await setup({ "en-US": madeUpNames("English") }, "en-US");
    const texts = {
      t: i18n.global.t,
      te: i18n.global.te,
    } as unknown as import("../map-view").Texts;
    expect(questTarget(texts, "mainQuest", 3)).toBe(
      `English ${QuestTargetWords["mainQuest.steps.3"]}`,
    );
    vi.resetModules();
    const bare = await setup({}, "en-US");
    const { questTarget: bareTarget } = await import("../quest-target-words");
    const bareTexts = {
      t: bare.i18n.global.t,
      te: bare.i18n.global.te,
    } as unknown as import("../map-view").Texts;
    expect(bareTarget(bareTexts, "mainQuest", 3)).toBeNull();
  });

  it("writes no Digimon target of a quest in the app's texts, in any language", async () => {
    const { QuestTargetWords } = await import("../quest-target-words");
    const { default: english } = await import("@/i18n/locales/en-US/handheld-locale");
    const quests = await Promise.all(
      ["es-ES", "fr-FR", "de-DE", "it-IT"].map(
        async (locale) => (await import(`@/i18n/locales/${locale}/quests.json`)).default,
      ),
    );
    for (const messages of [english, ...quests]) {
      const left = Object.keys(QuestTargetWords).filter((key) => {
        const [quest, , step] = key.split(".");
        const steps = (
          messages as Record<string, { steps?: Record<string, { locationTarget?: string }> }>
        )[quest!]?.steps;
        return steps?.[step!]?.locationTarget !== undefined;
      });
      expect(left).toEqual([]);
    }
  });

  it("names the characters of the Amaterasu side of the Mobius Desert from the disc", async () => {
    const words = {
      "talk07:848": "Made-up Sagittarius",
      "talk00:24": "Made-up Agent",
      "talk02:721": "Made-up Monk",
    };
    const { i18n } = await setup({ "en-US": words }, "en-US");
    const texts = {
      t: i18n.global.t,
      te: i18n.global.te,
    } as unknown as import("../map-view").Texts;
    const { buildMobiusGrid } = await import("../mobius-sheet");
    const notes = buildMobiusGrid("02C1", null, texts, "amaterasu")
      .cells.flatMap((cell) => (cell.note === null ? [] : [cell.note]))
      .sort();
    expect(notes).toEqual(["Made-up Agent", "Made-up Monk", "Made-up Sagittarius"]);
  });

  it("writes the note of a card that summons a Digimon with the name of its card on the disc", async () => {
    const cards = { "cardnm:81": "Made-up Angel", "cardnm:314": "Made-up Gear" };
    const { i18n } = await setup({ "en-US": cards, "fr-FR": cards }, "fr-FR");
    expect(i18n.global.t("card.1001.note")).toBe("Invoque 2 Made-up Angel");
    expect(i18n.global.t("card.1049.note", {}, { locale: "en-US" })).toBe(
      "Bring Made-up Gear to field",
    );
    expect(i18n.global.te("card.1005.note", "en-US")).toBe(false);
  });
});
