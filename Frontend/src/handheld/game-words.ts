import { gameWord } from "./language";
import type { Texts } from "./map-view";

/** The kinds of the placeholders, which say the app's own word for a name the extracted text lacks. */
export const WordKinds = ["place", "digimon", "person", "item", "tech", "card"] as const;

export type WordKind = (typeof WordKinds)[number];

/** The kind of a placeholder: `place2` is a place. */
export function wordKind(slot: string): WordKind {
  const kind = slot.replace(/\d+$/, "");
  return (WordKinds as readonly string[]).includes(kind) ? (kind as WordKind) : "place";
}

/**
 * Fills the game names of a sentence of the app: each placeholder with the disc's line, or the app's generic word for
 * its kind (`handheld.word.place`) when the extracted text lacks the line. Never the game's text written by the app.
 */
export function fillWords(
  texts: Texts,
  words: Readonly<Record<string, string>>,
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(words).map(([slot, reference]) => [
      slot,
      gameWord(reference, "") || texts.t(`handheld.word.${wordKind(slot)}`),
    ]),
  );
}

/**
 * The techniques of the enemies, in the app's sentence that says what they do (`enemyTechniques.madPump`, "{tech}
 * (Physical, Water)"): the line of the game's technique names that names each (`sklnam:396`).
 */
export const EnemyTechniqueWords: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  necromist: { tech: "sklnam:382" },
  madPump: { tech: "sklnam:396" },
  masterFire: { tech: "sklnam:384" },
  atomicRay: { tech: "sklnam:386" },
  airBlast: { tech: "sklnam:153" },
  confuseGas: { tech: "sklnam:176" },
  divineRain: { tech: "sklnam:145" },
  smallHeal: { tech: "sklnam:184" },
  doubleGuard: { tech: "sklnam:200" },
  thunderbolt: { tech: "sklnam:156" },
  thunderGemini: { tech: "sklnam:157" },
  speedUp: { tech: "sklnam:202" },
  magicMissile: { tech: "sklnam:160", tech2: "sklnam:207" },
  paralyzeShock: { tech: "sklnam:175" },
  twinMissile: { tech: "sklnam:161" },
  lovelyCharm: { tech: "sklnam:204" },
  confuseNebula: { tech: "sklnam:177" },
  darkMatter: { tech: "sklnam:164" },
  darkFear: { tech: "sklnam:165" },
  hardRain: { tech: "sklnam:146" },
  flameBall: { tech: "sklnam:137" },
  flameLance: { tech: "sklnam:138" },
  iceBlow: { tech: "sklnam:149" },
  flameBreath: { tech: "sklnam:139" },
  darkElemental: { tech: "sklnam:166" },
  godBombard: { tech: "sklnam:163", tech2: "sklnam:211" },
  grandWave: { tech: "sklnam:147" },
  megaHeal: { tech: "sklnam:185" },
  fullHeal: { tech: "sklnam:186" },
  slyphStorm: { tech: "sklnam:155" },
  darknessChaos: { tech: "sklnam:167" },
  flameSphere: { tech: "sklnam:140" },
  berserkSword: { tech: "sklnam:398", tech2: "sklnam:209" },
  heartBreakAttack: { tech: "sklnam:388" },
  autoRecover: { tech: "sklnam:189" },
  helterSkelter: { tech: "sklnam:391" },
  // DemiDevimon's: its entry of the battle table (FILE_BATTLE_TABLE) names technique 182, Energy Leech.
  energySuck: { tech: "sklnam:182" },
};

/**
 * The items and people the closing conditions of the auctions name (`handheld.auction.closesWhen.*`), by auction: Kail
 * and the Byakko Leader by the speaker of a line of their talk.
 */
export const AuctionWords: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  hazardShield: { item: "itmnam:21" },
  sniperShield: { person: "talk00:77" },
  dramonShield: { item: "itmnam:14" },
  yinYangWand: { person: "talk01:7" },
};

/** The items the wiki of the tamers names: the trophies a battle asks for and the Folder Bag. */
export const NpcItemWords = {
  asukaTrophy: "itmnam:18",
  sunTrophy: "itmnam:20",
  folderBag: "itmnam:402",
} as const;

/** The name of an item of the game, or the app's own word for one when the extracted text lacks its line. */
export function itemWord(texts: Texts, reference: string): string {
  return gameWord(reference, "") || texts.t("handheld.word.item");
}

/**
 * The equipment slots of a partner as the field menu's equipment panel names them when they are empty: the lines of
 * the menu's text (STATUS) that dw3_decomp's STSTATUS_slotStrings gives each slot. Both accessories are "Accessory".
 */
export const EquipmentSlotWords = {
  head: "status:65",
  body: "status:66",
  right: "status:67",
  left: "status:77",
  accessory1: "status:68",
  accessory2: "status:68",
} as const;

/**
 * The kind of a piece of equipment as the item shop names it (STITSHOP_kindStrings of dw3_decomp, the shop's text
 * ITSHOP), by the app's kind: the disc's records put every one-handed weapon in either hand (kind 3, "Right or Left"),
 * the shields in the left one (kind 2), the armours on head or body and the accessories in kinds 6 and 8, which the shop
 * names alike. The game has no kind word of its own for a shield: it says where it goes.
 */
export const EquipmentKindWords = {
  oneHandedWeapon: "itshop:37",
  twoHandedWeapon: "itshop:41",
  shield: "itshop:36",
  headArmor: "itshop:38",
  bodyArmor: "itshop:39",
  accessory: "itshop:40",
} as const;

/** The name of an equipment slot; without its line in the extracted text, the app's word for an empty one. */
export function slotWord(
  t: (key: string) => string,
  slot: keyof typeof EquipmentSlotWords,
): string {
  return gameWord(EquipmentSlotWords[slot], "") || t("handheld.equipment.empty");
}

/** Where a kind of equipment goes; without its line (or for a kind the disc does not have), the app's word for an item. */
export function equipmentKindWord(t: (key: string) => string, kind: string): string {
  const reference = (EquipmentKindWords as Readonly<Record<string, string>>)[kind];
  return (reference === undefined ? "" : gameWord(reference, "")) || t("handheld.word.item");
}

/**
 * The game's currency, as its menus print it: the field menu's label of the player's money (`status:5`, "BIT"), and an
 * amount as the item shop writes it (`itshop:18`, "{1} BIT") or the card shop (`crdshp:12`, "BIT {1}" in Spanish).
 */
export const MoneyWords = {
  unit: "status:5",
  amount: "itshop:18",
  cardPrice: "crdshp:12",
} as const;

type Translate = (key: string, named?: Record<string, unknown>) => string;

/** The currency's name; without its line, the app's own word for the unit (handheld.money.unit). */
export function moneyUnit(t: Translate): string {
  return gameWord(MoneyWords.unit, "") || t("handheld.money.unit");
}

/**
 * An amount of money as the game writes it, by the item shop's line or, for a card's price, the card shop's; without
 * the line, the app's own amount (handheld.money.amount) with the currency's name.
 */
export function moneyAmount(
  t: Translate,
  amount: number,
  format: "amount" | "cardPrice" = "amount",
): string {
  const line = gameWord(MoneyWords[format], "");
  return line.includes("{1}")
    ? line.replace("{1}", String(amount))
    : t("handheld.money.amount", { n: amount, unit: moneyUnit(t) });
}
