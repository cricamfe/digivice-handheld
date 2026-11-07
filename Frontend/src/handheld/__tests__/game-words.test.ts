import { beforeEach, describe, expect, it, vi } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import enTechniques from "@/i18n/locales/en-US/handheld-enemy-techniques.json";
import enRegularAttacks from "@/i18n/locales/en-US/enemy/enemy-regular-attack.json";
import enemies from "@/database/enemy.json";
import enHandheld from "@/i18n/locales/en-US/handheld.json";
import esUi from "@/i18n/locales/es-ES/ui.json";
import esHandheld from "@/i18n/locales/es-ES/handheld.json";
import esField from "@/i18n/locales/es-ES/field.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";
import { ComposedWords, NamedPaths } from "../composed-messages";
import enDigimon from "@/i18n/locales/en-US/digimon.json";
import enLocale from "@/i18n/locales/en-US/handheld-locale";
import esDigimon from "@/i18n/locales/es-ES/digimon.json";
import esLocale from "@/i18n/locales/es-ES/index";
import {
  AuctionWords,
  EnemyTechniqueWords,
  EquipmentKindWords,
  EquipmentSlotWords,
  MoneyWords,
  NpcItemWords,
} from "../game-words";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const knownWords = new Set(textKeys.words.map((word) => word.key));

const references = [
  ...Object.values(EnemyTechniqueWords).flatMap((slots) => Object.values(slots)),
  ...Object.values(AuctionWords).flatMap((slots) => Object.values(slots)),
  ...Object.values(NpcItemWords),
  ...Object.values(EquipmentSlotWords),
  ...Object.values(EquipmentKindWords),
  ...Object.values(MoneyWords),
  ...ComposedWords,
];

/** Made-up words in the shape of the disc's, for every line these sentences and names use: no game text. */
const madeUpWords = Object.fromEntries(
  references.map((reference) => [reference, `Name ${reference}`]),
);

async function setup(words: Record<string, string>, locale = "en-US" as const) {
  gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}.json`);
  vi.stubGlobal("fetch", async () => ({ ok: true, json: async () => ({ messages: {}, words }) }));
  const { setLanguage } = await import("../language");
  const { default: i18n } = await import("@/i18n/handheld");
  await setLanguage(locale);
  const texts = { t: i18n.global.t, te: i18n.global.te } as unknown as import("../map-view").Texts;
  return { texts, words: await import("../game-words"), view: await import("../map-view") };
}

const regularAttackFiles = {
  "en-US": enRegularAttacks.regularAttacks,
  "es-ES": esUi.regularAttacks,
  "fr-FR": frUi.regularAttacks,
  "de-DE": deUi.regularAttacks,
  "it-IT": itUi.regularAttacks,
} as const;

const techniqueFiles = {
  "en-US": enTechniques.enemyTechniques,
  "es-ES": esUi.enemyTechniques,
  "fr-FR": frUi.enemyTechniques,
  "de-DE": deUi.enemyTechniques,
  "it-IT": itUi.enemyTechniques,
} as const;

describe("the items, techniques and cards the app's texts name come from the disc", () => {
  beforeEach(() => {
    vi.resetModules();
    gameAssetUrl.mockReturnValue(null);
  });

  it("refers only to lines the extractor publishes", () => {
    expect(references.filter((reference) => !knownWords.has(reference))).toEqual([]);
  });

  it("names the techniques of the enemies with a placeholder in every language", () => {
    for (const [locale, file] of Object.entries(techniqueFiles)) {
      for (const [id, text] of Object.entries(file)) {
        const slots = Object.keys(EnemyTechniqueWords[id] ?? {});
        const placeholders = [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]);
        expect(placeholders, `${locale} ${id}`).toEqual(slots);
      }
    }
  });

  it("writes the regular attacks as the app's own sentences, without game words", () => {
    for (const [locale, file] of Object.entries(regularAttackFiles)) {
      for (const [id, text] of Object.entries(file))
        expect(text, `${locale} ${id}`).not.toMatch(/\{/);
    }
  });

  it("uses none of the technique table's placeholder lines, English in every language", () => {
    for (const placeholder of ["sklnam:436", "sklnam:439", "sklnam:441"]) {
      expect(references).not.toContain(placeholder);
    }
  });

  it("has a text for the regular attack of every enemy in every language", () => {
    const ids = new Set(
      Object.values(enemies).flatMap((enemy) =>
        enemy.regularAttackId === null ? [] : [enemy.regularAttackId],
      ),
    );
    for (const [locale, file] of Object.entries(regularAttackFiles)) {
      expect(Object.keys(file).sort(), locale).toEqual([...ids].sort());
    }
  });

  it("fills the Energy Leech, the enemies without a technique and the attack command from the disc", async () => {
    const { texts, words, view } = await setup(madeUpWords);
    expect(
      texts.t(
        "enemyTechniques.energySuck",
        words.fillWords(texts, EnemyTechniqueWords.energySuck!),
      ),
    ).toBe("Name sklnam:182 (Magic)");
    expect(view.gameText(texts, "enemyTechniques.none")).toBe("Name status:62");
    expect(view.gameText(texts, "enemy.regularAttack")).toBe("Name mfight:1");
  });

  it("fills an enemy technique with the technique names of the extracted text", async () => {
    const { texts, words } = await setup(madeUpWords);
    expect(
      texts.t(
        "enemyTechniques.godBombard",
        words.fillWords(texts, EnemyTechniqueWords.godBombard!),
      ),
    ).toBe("Name sklnam:163 (Magic-Machine), Name sklnam:211 (Enemy Can't Escape)");
  });

  it("fills the closing conditions of the auctions with the items of the extracted text", async () => {
    const { texts, words } = await setup(madeUpWords);
    expect(
      texts.t("handheld.auction.closesWhen.dramonShield", {
        ...words.fillWords(texts, AuctionWords.dramonShield!),
        place: "P",
      }),
    ).toBe("Closes on returning to P with the Name itmnam:14 (step 42) or on taking part.");
  });

  it("names the trophies and the Folder Bag of the tamers' wiki from the disc", async () => {
    const { texts, words } = await setup(madeUpWords);
    expect(words.itemWord(texts, NpcItemWords.sunTrophy)).toBe("Name itmnam:20");
    expect(
      texts.t("handheld.wiki.npc.folderBag", {
        item: words.itemWord(texts, NpcItemWords.folderBag),
      }),
    ).toBe("You need the Name itmnam:402 to challenge tamers.");
  });

  it("composes the names the app's English wrote otherwise from their lines", async () => {
    const { texts, view } = await setup(madeUpWords);
    expect(view.gameText(texts, "card.1024.name")).toBe("Name cardnm:25");
    expect(view.gameText(texts, "boosters.501.name")).toBe("Name itmnam:375");
    expect(view.gameText(texts, "technique.devilShot.name")).toBe("Name sklnam:136");
    expect(view.gameText(texts, "field.machine")).toBe("Name itmnam:82");
  });

  it("uses the app's own word, or nothing, for a name the extracted text lacks", async () => {
    const { texts, words, view } = await setup({});
    expect(words.itemWord(texts, NpcItemWords.asukaTrophy)).toBe("Item");
    expect(
      texts.t("enemyTechniques.madPump", words.fillWords(texts, EnemyTechniqueWords.madPump!)),
    ).toBe("Technique (Physical, Water)");
    for (const path of NamedPaths) expect(view.gameText(texts, path), path).toBeNull();
  });

  it("names the equipment slots as the equipment panel does, and the kinds as the shop does", async () => {
    const { texts, words } = await setup(madeUpWords);
    const t = (key: string) => texts.t(key);
    expect(words.slotWord(t, "head")).toBe("Name status:65");
    expect(words.slotWord(t, "accessory2")).toBe("Name status:68");
    expect(words.equipmentKindWord(t, "bodyArmor")).toBe("Name itshop:39");
    expect(words.equipmentKindWord(t, "oneHandedWeapon")).toBe("Name itshop:37");
    expect(words.equipmentKindWord(t, "shield")).toBe("Name itshop:36");
  });

  it("names an empty slot and an equipment kind with the app's words without their lines", async () => {
    const { texts, words } = await setup({});
    const t = (key: string) => texts.t(key);
    expect(words.slotWord(t, "left")).toBe("Not equipped");
    expect(words.equipmentKindWord(t, "bodyArmor")).toBe("Item");
    expect(words.equipmentKindWord(t, "unknownKind")).toBe("Item");
  });

  it("writes amounts of money as the game's shops do, with the game's currency", async () => {
    const { texts, words } = await setup({
      "status:5": "Coin",
      "itshop:18": "{1} Coin",
      "crdshp:12": "Coin {1}",
    });
    const t = (key: string, named?: Record<string, unknown>) => texts.t(key, named ?? {});
    expect(words.moneyUnit(t)).toBe("Coin");
    expect(words.moneyAmount(t, 300)).toBe("300 Coin");
    expect(words.moneyAmount(t, 300, "cardPrice")).toBe("Coin 300");
    expect(texts.t("handheld.auction.notEnoughBits", { bid: words.moneyAmount(t, 50) })).toBe(
      "Without 50 Coin on arrival, the auction won't start.",
    );
    expect(texts.t("handheld.auction.enoughBits", { money: words.moneyUnit(t) })).toBe(
      "You have enough Coin to bid.",
    );
  });

  it("writes amounts with the app's own unit without the currency's lines", async () => {
    const { texts, words } = await setup({});
    const t = (key: string, named?: Record<string, unknown>) => texts.t(key, named ?? {});
    expect(words.moneyUnit(t)).toBe("bits");
    expect(words.moneyAmount(t, 5, "cardPrice")).toBe("5 bits");
  });

  it("writes the currency in none of the handheld's own texts", () => {
    for (const ui of [enHandheld, esHandheld, frUi, deUi, itUi]) {
      const { auction, money, ...rest } = ui.handheld;
      const texts = JSON.stringify({ auction, rest }).replace(/\{\w+\}/g, "");
      expect(texts, JSON.stringify(money)).not.toMatch(/\bbits?\b/i);
      expect(Object.values(money).join(" ")).not.toMatch(/BIT/);
    }
    for (const ui of [frUi, deUi, itUi]) {
      expect(ui.enemy).not.toHaveProperty("bits");
      expect(ui.auction).not.toHaveProperty("bitsLabel");
    }
  });

  it("writes none of those names in the handheld's own texts", () => {
    for (const locale of [enLocale, esLocale, frUi, deUi, itUi]) {
      expect("equipmentType" in locale).toBe(false);
    }
    for (const digimon of [
      enDigimon,
      esDigimon,
      { digimon: frUi.digimon },
      { digimon: deUi.digimon },
    ]) {
      expect(digimon.digimon).not.toHaveProperty("equipmentSlot");
    }
    expect(esField).not.toHaveProperty(["field", "fire"]);
    for (const ui of [esUi, frUi, deUi, itUi]) {
      expect(ui.enemyTechniques).not.toHaveProperty("none");
      expect("boosters" in ui).toBe(false);
      expect(ui).not.toHaveProperty(["field", "fire"]);
    }
    for (const handheld of [enHandheld, esHandheld, frUi, deUi, itUi]) {
      expect(handheld.handheld.wiki.npc).not.toHaveProperty("asukaTrophy");
    }
  });
});
