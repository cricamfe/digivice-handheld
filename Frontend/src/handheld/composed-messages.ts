/**
 * Messages the app composes from the game's words on the disc with rules of its own, merged into each language when it
 * loads (language.ts): the bundle ships the rules, never the names.
 */

/**
 * The rivals of Duel Island: the five Divermon in the order the player meets them (`dignam:88`, numbered in the app's
 * words, `handheld.duelIsland.nth`) and KingDivermon (the speaker of a line of talk, `talk06:791`).
 */
const Rivals: Readonly<Record<string, { word: string; order: number | null }>> = {
  divermon1: { word: "dignam:88", order: 1 },
  divermon2: { word: "dignam:88", order: 2 },
  divermon3: { word: "dignam:88", order: 3 },
  divermon4: { word: "dignam:88", order: 4 },
  divermon5: { word: "dignam:88", order: 5 },
  kingDivermon: { word: "talk06:791", order: null },
};

/**
 * The A.o.A troopers and soldiers of the Administration Center, whom the game names all alike (the speaker of their
 * talk, `talk01:299` "Trooper", `talk03:444` "Soldier"): numbered in the order of the story, with the rule of the rivals.
 */
const NumberedNpcs: Readonly<Record<string, { word: string; order: number }>> = {
  trooper1AdminCenterB1F: { word: "talk01:299", order: 1 },
  trooper2AdminCenterB1F: { word: "talk01:299", order: 2 },
  trooper3AdminCenterB1F: { word: "talk01:299", order: 3 },
  optionalTrooper1AdminCenterB1F: { word: "talk01:299", order: 4 },
  optionalTrooper2AdminCenterB1F: { word: "talk01:299", order: 5 },
  trooper1BasementStairs: { word: "talk01:299", order: 6 },
  trooper2BasementStairs: { word: "talk01:299", order: 7 },
  trooper3BasementStairs: { word: "talk01:299", order: 8 },
  trooper1AdminFloor1: { word: "talk01:299", order: 9 },
  trooper2AdminFloor1: { word: "talk01:299", order: 10 },
  optionalTrooperAdminFloor1: { word: "talk01:299", order: 11 },
  trooper1AdminFloor2: { word: "talk01:299", order: 12 },
  trooper2AdminFloor2: { word: "talk01:299", order: 13 },
  optionalTrooper1AdminFloor2: { word: "talk01:299", order: 14 },
  optionalTrooper2AdminFloor2: { word: "talk01:299", order: 15 },
  soldierAdminFloor2: { word: "talk03:444", order: 1 },
  optionalSoldier1AdminFloor2: { word: "talk03:444", order: 2 },
};

/**
 * The cards that summon a Digimon, which the disc gives no note: the summoned Digimon's card (`cardnm:81`) and how many
 * come, in the app's sentence (`handheld.card.summon`).
 */
const Summons: Readonly<Record<string, { word: string; count: number }>> = {
  "1001": { word: "cardnm:81", count: 2 },
  "1005": { word: "cardnm:100", count: 1 },
  "1011": { word: "cardnm:141", count: 1 },
  "1017": { word: "cardnm:185", count: 1 },
  "1023": { word: "cardnm:230", count: 1 },
  "1029": { word: "cardnm:252", count: 1 },
  "1049": { word: "cardnm:314", count: 1 },
};

/**
 * App messages that are a game name the app's English wrote otherwise ("Darness Gale", "1B Booster"), so the extractor
 * finds no line for them: by message path, the line of the disc that names it. The field of a battle is named like the
 * item that sets it. The tamers and the NPCs that the game names are named by the speaker of a line of their talk
 * (`talk06:70`, "Tamer Lucia"), like the characters of the field (ActorNames).
 */
const NamedMessages: Readonly<Record<string, string>> = {
  "technique.desperadoBlaster.name": "sklnam:77",
  "technique.devilShot.name": "sklnam:136",
  "technique.kongouKaimandra.name": "sklnam:91",
  "equipments.335.name": "itmnam:335",
  "card.1024.name": "cardnm:25",
  "card.1034.name": "cardnm:35",
  "card.1146.name": "cardnm:147",
  "card.1189.name": "cardnm:190",
  "card.1192.name": "cardnm:193",
  "card.1197.name": "cardnm:198",
  "card.1198.name": "cardnm:199",
  "card.1240.name": "cardnm:241",
  "boosters.501.name": "itmnam:375",
  "boosters.502.name": "itmnam:376",
  "boosters.503.name": "itmnam:377",
  "boosters.504.name": "itmnam:378",
  "boosters.505.name": "itmnam:379",
  "boosters.506.name": "itmnam:380",
  "boosters.507.name": "itmnam:381",
  "boosters.508.name": "itmnam:382",
  "boosters.509.name": "itmnam:383",
  "boosters.510.name": "itmnam:384",
  "boosters.511.name": "itmnam:385",
  "boosters.512.name": "itmnam:386",
  "boosters.513.name": "itmnam:387",
  "boosters.514.name": "itmnam:388",
  "boosters.515.name": "itmnam:389",
  "boosters.516.name": "itmnam:390",
  "boosters.517.name": "itmnam:391",
  "boosters.518.name": "itmnam:392",
  "boosters.519.name": "itmnam:393",
  "boosters.520.name": "itmnam:394",
  "boosters.521.name": "itmnam:91",
  "boosters.522.name": "itmnam:361",
  "boosters.523.name": "itmnam:362",
  "boosters.524.name": "itmnam:363",
  "boosters.525.name": "itmnam:364",
  "boosters.526.name": "itmnam:365",
  "boosters.527.name": "itmnam:366",
  "boosters.528.name": "itmnam:367",
  "boosters.529.name": "itmnam:368",
  "boosters.530.name": "itmnam:369",
  "boosters.531.name": "itmnam:370",
  "boosters.532.name": "itmnam:371",
  "boosters.533.name": "itmnam:372",
  "boosters.534.name": "itmnam:373",
  "boosters.535.name": "itmnam:374",
  "tamers.genji.name": "talk00:104",
  "tamers.natsumi.name": "talk02:46",
  "tamers.nacky.name": "talk00:112",
  "tamers.wong.name": "talk00:117",
  "tamers.steve.name": "talk00:122",
  "tamers.gloria.name": "talk00:152",
  "tamers.mitch.name": "talk02:41",
  "tamers.catherine.name": "talk06:58",
  "tamers.lucia.name": "talk06:70",
  "tamers.robert.name": "talk06:82",
  "tamers.akiba.name": "talk06:220",
  "tamers.chris.name": "talk01:213",
  "tamers.bob.name": "talk06:123",
  "tamers.tomomi.name": "talk06:94",
  "tamers.andy.name": "talk06:138",
  "tamers.george.name": "talk06:151",
  "tamers.meiLin.name": "talk06:164",
  "tamers.alice.name": "talk01:226",
  "tamers.gordon.name": "talk06:178",
  "tamers.jessica.name": "talk06:190",
  "tamers.nakano.name": "talk02:205",
  "npcs.seiryuLeader.name": "talk01:6",
  "npcs.keith.name": "talk01:366",
  "npcs.suzakuLeader.name": "talk01:146",
  "npcs.fakeByakkoLeader.name": "talk01:7",
  "npcs.byakkoLeader.name": "talk01:7",
  "npcs.generalAdminFloor2.name": "talk01:301",
  "npcs.aoaAttacker.name": "talk03:439",
  // Named only by the speaker of a story scene (`dmg200/24`, entry 24 of ESDMG200.BIN).
  "npcs.gameMasterHeadquarters.name": "dmg200/24:8",
  "field.fire": "itmnam:77",
  "field.water": "itmnam:78",
  "field.ice": "itmnam:79",
  "field.wind": "itmnam:80",
  "field.thunder": "itmnam:81",
  "field.machine": "itmnam:82",
  "field.dark": "itmnam:83",
  // The battle menu's command of a regular attack (MFIGHT, FIGHTSTG_showCommands) and the field menu's words for a
  // Digimon without a technique (STATUS).
  "enemy.regularAttack": "mfight:1",
  "enemyTechniques.none": "status:62",
};

/** Messages that join several game names with the app's " / ": the menu scene of Dr. Kadomatsu and Piximon. */
const JoinedMessages: Readonly<Record<string, readonly string[]>> = {
  "map.0D00": ["talk05:4", "talk01:76"],
};

/** The references the composed messages use, for the tests. */
export const ComposedWords = [
  ...Object.values(Rivals).map((rival) => rival.word),
  ...Object.values(NumberedNpcs).map((npc) => npc.word),
  ...Object.values(Summons).map((summon) => summon.word),
  ...Object.values(NamedMessages),
  ...Object.values(JoinedMessages).flat(),
];

/** The message paths composed from a game name alone, for the tests. */
export const NamedPaths = [...Object.keys(NamedMessages), ...Object.keys(JoinedMessages)];

type Tree = { [key: string]: string | Tree };

function put(tree: Tree, path: string, value: string): void {
  const keys = path.split(".");
  let node = tree;
  for (const key of keys.slice(0, -1)) {
    const next = node[key];
    node = typeof next === "object" ? next : (node[key] = {});
  }
  node[keys[keys.length - 1]!] = value;
}

type Translate = (key: string, named: Record<string, unknown>) => string;

/**
 * The composed messages of a language, from its words of the disc and its rules: `duelIsland.<id>.name`,
 * `card.<id>.note`, the numbered `npcs.<id>.name` and the names of NamedMessages. One whose line the extracted text lacks is left out, so the screen
 * shows what it shows without it.
 */
export function composedMessages(words: ReadonlyMap<string, string>, t: Translate): Tree {
  const duelIsland: Record<string, { name: string }> = {};
  for (const [id, rival] of Object.entries(Rivals)) {
    const name = words.get(rival.word);
    if (name === undefined || name === "") continue;
    duelIsland[id] = {
      name: rival.order === null ? name : t("handheld.duelIsland.nth", { n: rival.order, name }),
    };
  }

  const card: Record<string, { note: string }> = {};
  for (const [id, summon] of Object.entries(Summons)) {
    const name = words.get(summon.word);
    if (name === undefined || name === "") continue;
    card[id] = {
      note:
        summon.count === 1
          ? t("handheld.card.summon", { name })
          : t("handheld.card.summonMany", { name, count: summon.count }),
    };
  }

  const npcs: Record<string, { name: string }> = {};
  for (const [id, npc] of Object.entries(NumberedNpcs)) {
    const name = words.get(npc.word);
    if (name === undefined || name === "") continue;
    npcs[id] = { name: t("handheld.duelIsland.nth", { n: npc.order, name }) };
  }

  const composed: Tree = { duelIsland, card, npcs };
  for (const [path, word] of Object.entries(NamedMessages)) {
    const name = words.get(word);
    if (name !== undefined && name !== "") put(composed, path, name);
  }
  for (const [path, joined] of Object.entries(JoinedMessages)) {
    const names = joined.map((word) => words.get(word) ?? "");
    if (names.every((name) => name !== "")) put(composed, path, names.join(" / "));
  }
  return composed;
}
