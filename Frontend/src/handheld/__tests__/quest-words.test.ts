import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import enQuests from "@/i18n/locales/en-US/handheld-quests.json";
import esQuests from "@/i18n/locales/es-ES/quests.json";
import frQuests from "@/i18n/locales/fr-FR/quests.json";
import deQuests from "@/i18n/locales/de-DE/quests.json";
import itQuests from "@/i18n/locales/it-IT/quests.json";
import enHandheld from "@/i18n/locales/en-US/handheld.json";
import esHandheld from "@/i18n/locales/es-ES/handheld.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";
import { WordKinds } from "../game-words";
import { QuestWords } from "../quest-words";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const knownWords = new Set(textKeys.words.map((word) => word.key));

/** Made-up words in the shape of the disc's, for every line the quest texts name: no game text. */
const madeUpWords = (prefix: string) =>
  Object.fromEntries(
    Object.values(QuestWords)
      .flatMap((slots) => Object.values(slots))
      .map((reference) => [reference, `${prefix} ${reference}`]),
  );

function leaves(tree: object, path: string[] = []): [string, string][] {
  return Object.entries(tree).flatMap(([key, value]) =>
    typeof value === "string" ? [[[...path, key].join("."), value]] : leaves(value, [...path, key]),
  );
}

const questFiles = {
  "en-US": enQuests,
  "es-ES": esQuests,
  "fr-FR": frQuests,
  "de-DE": deQuests,
  "it-IT": itQuests,
} as const;

async function setup(words: Record<string, string>, locale: keyof typeof questFiles = "en-US") {
  gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}.json`);
  vi.stubGlobal("fetch", async () => ({ ok: true, json: async () => ({ messages: {}, words }) }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage(locale);
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  return { texts, quests: await import("../quest-words") };
}

describe("the quest texts name the game's places, Digimon and people by the disc's lines", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("refers only to lines the extractor publishes", () => {
    const references = Object.values(QuestWords).flatMap((slots) => Object.values(slots));
    expect(references.length).toBeGreaterThan(300);
    expect(references.filter((reference) => !knownWords.has(reference))).toEqual([]);
  });

  it("has a line for every placeholder of every language, and a text for every reference", () => {
    for (const [locale, file] of Object.entries(questFiles)) {
      for (const [key, text] of leaves(file)) {
        const slots = QuestWords[key] ?? {};
        const placeholders = [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]);
        expect(
          placeholders.filter((slot) => slot === undefined || !(slot in slots)),
          `${locale} ${key}`,
        ).toEqual([]);
      }
    }
    const english = new Map(leaves(enQuests));
    for (const [key, slots] of Object.entries(QuestWords)) {
      const text = english.get(key);
      expect(text, key).toBeDefined();
      expect(
        Object.keys(slots).filter((slot) => !text!.includes(`{${slot}}`)),
        key,
      ).toEqual([]);
    }
  });

  it("keeps the same texts in every language, and only those the handheld shows", () => {
    const keys = leaves(enQuests)
      .map(([key]) => key)
      .sort();
    expect(
      keys.every((key) =>
        /^\w+\.(title|description|steps\.\d+\.(description|locationTarget))$/.test(key),
      ),
    ).toBe(true);
    for (const file of Object.values(questFiles)) {
      expect(
        leaves(file)
          .map(([key]) => key)
          .sort(),
      ).toEqual(keys);
    }
  });

  it("fills a step with the names of the extracted text", async () => {
    const { texts, quests } = await setup(madeUpWords("Name"));
    expect(quests.questText(texts, "driAgentAgumon.steps.2.description")).toBe(
      "Defeat Name dignam:23 in battle to receive Name dignam:4's DDNA",
    );
    expect(quests.questText(texts, "driAgentAgumon.title")).toBe("Name dignam:4 (Name talk06:690)");
    expect(quests.questText(texts, "driAgentAgumon.steps.1.locationTarget")).toBe(
      "Name talk06:690",
    );
  });

  it("fills them in the language shown, with its own sentence", async () => {
    const { texts, quests } = await setup(madeUpWords("Nom"), "de-DE");
    expect(quests.questText(texts, "driAgentAgumon.steps.3.description")).toBe(
      "Bring die DNA von Nom dignam:4 zu Nom talk06:690",
    );
  });

  it("names the target of a step the same way", async () => {
    const { texts } = await setup(madeUpWords("Name"));
    const { questTarget } = await import("../quest-target-words");
    expect(questTarget(texts, "driAgentAgumon", 1)).toBe("Name talk06:690");
    expect(questTarget(texts, "mainQuest", 6)).toBe("Gondola");
  });

  it("names the A.o.A's people, Kenny, Nick and the places the guides named otherwise from the disc", async () => {
    const { texts, quests } = await setup(madeUpWords("Name"));
    expect(quests.questText(texts, "mainQuest.steps.67.description")).toBe(
      "Defeat the Name talk01:299 blocking you from entering Name stname:23",
    );
    expect(quests.questText(texts, "mainQuest.steps.115.description")).toBe(
      "Defeat every Name talk03:439 blocking Name stname:17",
    );
    expect(quests.questText(texts, "mainQuest.steps.64.locationTarget")).toBe(
      "Name talk01:366 & Name talk01:367",
    );
    expect(quests.questText(texts, "mainQuest.steps.94.locationTarget")).toBe("Name talk07:339");
    expect(quests.questText(texts, "mainQuest.steps.16.description")).toBe(
      "Talk to Name talk01:4 in training at the Name stname:53",
    );
    expect(quests.questText(texts, "mainQuest.steps.102.description")).toBe(
      "Ask the Name talk02:779 in the Name stname:106 to send you to Name stname:112 in the Real World",
    );
    expect(quests.questText(texts, "mainQuest.steps.88.description")).toBe(
      "Confront the Name dmg200/24:8 inside the Name stname:22",
    );
    expect(quests.questText(texts, "mainQuest.steps.129.locationTarget")).toBe("Name dmg800/9:1");
    expect(quests.questText(texts, "mainQuest.steps.119.description")).toBe(
      "Go through the door that opens by itself in the Name stname:21 and defeat every Name dmg300/16:1 in the Name stname:22",
    );
  });

  it("writes no guide's name for them in any language", () => {
    const invented =
      /Trooper|Soldier|Soldado|Militaire|Soldat|Attacker|Kenny|Nicky|Tranquil Inn|Military Base|Base Militar|Militärbasis|Cuartel General|Hauptquartier|Game Master|Maître du Jeu|Royal Guard|Guardia Real|Garde royal/;
    for (const [locale, file] of Object.entries(questFiles)) {
      const found = leaves(file).filter(
        ([key, text]) => /^(quests\.)?mainQuest\./.test(key) && invented.test(text),
      );
      expect(found, locale).toEqual([]);
    }
  });

  it("uses the app's own word for a name the extracted text lacks", async () => {
    const { texts, quests } = await setup({});
    expect(quests.questText(texts, "driAgentAgumon.steps.3.description")).toBe(
      "Deliver Digimon's DNA to Someone",
    );
    expect(quests.questText(texts, "mainQuest.steps.11.description")).toBe(
      "Return to the Place with the Item",
    );
    expect(quests.questText(texts, "mainQuest.title")).toBe("Main Quest");
    expect(quests.questText(texts, "noSuchQuest.title")).toBeNull();
  });

  it("has the generic word of every kind in every language", () => {
    for (const ui of [enHandheld, esHandheld, frUi, deUi, itUi]) {
      expect(Object.keys(ui.handheld.word).sort()).toEqual([...WordKinds].sort());
    }
  });
});
