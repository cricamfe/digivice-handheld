import type { FieldActorDTO } from "@/events/dto/field-actors.dto";

// Dev only: reached through mock.service.ts, which main.ts imports behind `import.meta.env.DEV`,
// so none of this ends up in the production bundle.

/** Variants of a golden snapshot, so every Mapa screen can be reviewed in the browser. */
export const mockCaseNames = [
  "unknownMap",
  "digimonBattle",
  "cardBattle",
  "seabed",
  "seabedPelche",
  "seabedAmaterasu",
  "mobius",
  "mobiusAmaterasu",
  "centralPark",
  "serverCopyBridge",
  "serverCopyPark",
  "battleForm",
  "battleOwnForm",
  "battleAttacks",
  "battleOne",
  "battleMany",
  "battleLongNames",
  "battleNeutral",
  "battleWideField",
  "battleFieldNeutral",
  "battleOneElement",
  "battleTwoElements",
  "battleFiveElements",
  "battleBench",
  "battleBenchUnknown",
  "battleStage",
  "battleNoStage",
  "mapActors",
  "mapFollowWide",
  "mapPlazaNear",
  "mapPlazaFar",
  "mapFollowTall",
  "mapFollowFits",
  "auctionNone",
  "auctionPoor",
  "auctionEarly",
] as const;

export type MockCaseName = (typeof mockCaseNames)[number];

/**
 * Cases that change the connection, not the snapshot: the snapshot is delivered first, so the player is on a screen,
 * and then the health event of the case arrives, as it does when the game is lost. One per `ConnectionCodes` value of
 * the Android core, plus `loading` (connected, no state yet) and `unknownCode`.
 */
export const mockConnectionCases = {
  retroarchUnreachable: { status: "Error", errorCode: "retroarch_unreachable" },
  noContent: { status: "Error", errorCode: "no_content" },
  memoryUnavailable: { status: "Error", errorCode: "memory_unavailable" },
  wrongGame: { status: "Error", errorCode: "wrong_game" },
  waitingForSave: { status: "Error", errorCode: "waiting_for_save" },
  memoryReadFailed: { status: "Error", errorCode: "memory_read_failed" },
  stateComposeFailed: { status: "Error", errorCode: "state_compose_failed" },
  unknownCode: { status: "Error", errorCode: "something_else" },
  loading: { status: "Loading", errorCode: null },
} as const;

export type MockConnectionCaseName = keyof typeof mockConnectionCases;

export function isMockConnectionCase(name: string): name is MockConnectionCaseName {
  return Object.hasOwn(mockConnectionCases, name);
}

/** The parts of a snapshot a case may change. Everything else in the snapshot stays as recorded. */
interface MockState {
  player: {
    mapId: string;
    previousMapId?: string;
    seabedRoute?: number | null;
    mapVariant?: number | null;
    bits?: number;
    positionX?: number;
    positionY?: number;
  };
  auctions?: Record<string, boolean>;
  journal?: { mainQuest?: { steps: { number: number; isDone: boolean }[] } };
  digimonBattle: {
    field?: number;
    fieldAmount?: number;
    stage?: number;
    enemy?: Record<string, unknown> | null;
  };
  cardBattle: { id: number | null };
  party?: {
    slots: {
      digimon: {
        inBattle: { formId: number };
        storedDigievolutions: { digievolutionId: number; level: number }[];
      } | null;
    }[];
  };
}

/** An enemy for the battle case when the snapshot has none (Raremon, as the design draws it). */
/** The form a battle case puts the first partner in, and its level (learned techniques follow the level). */
/** The enemy a battle case fights when it is not the snapshot's: its `memoryId` and `groupId` in `enemy.json`. */
const battleEnemies: Record<string, { id: number; groupId: number }> = {
  battleNeutral: { id: 34, groupId: 69 }, // Ogremon: no element to be weak or strong to
  battleOneElement: { id: 358, groupId: 1 }, // Pharaohmon: weak to fire only
  battleTwoElements: { id: 124, groupId: 2 }, // MasterTyrannomon: weak to ice, resists fire
  battleFiveElements: { id: 334, groupId: 74 }, // Maildramon: five elements out of seven
};

const battleForms: Record<string, { id: number; level: number }> = {
  battleOne: { id: 389, level: 5 },
  battleMany: { id: 389, level: 99 },
  battleLongNames: { id: 230, level: 99 },
};

const fallbackEnemy = {
  id: 139,
  groupId: 192,
  condition: 0,
  strength: 0,
  defense: 0,
  speed: 0,
  hp: { current: 816, max: 816 },
};

export const allMockCaseNames: readonly string[] = [
  ...mockCaseNames,
  ...Object.keys(mockConnectionCases),
];

function isMockCase(name: string): name is MockCaseName {
  return (mockCaseNames as readonly string[]).includes(name);
}

/**
 * Changes the snapshot in place for `?case=<name>`:
 * - `unknownMap`: MapId 0x279, which the catalogs do not know.
 * - `digimonBattle`: MapId 0x600 with an enemy.
 * - `cardBattle`: MapId 0x700 against Genji (first card battle).
 * - `seabed`: Seabed 5 on route 1.
 * - `seabedPelche`: Seabed 3 on route 5, which goes from Pelche Oasis to Lake of Ice.
 * - `seabedAmaterasu`: Seabed 3 on route 13, the Amaterasu server's route 3.
 * - `mobius`: Mobius Desert, area A3, with the player at (600, 500).
 * - `mobiusAmaterasu`: Mobius Desert of the Amaterasu server (map 02C1), area A3, with the player at (600, 500).
 * - `centralPark`: Central Park with the player at the middle of its picture (1664 x 1280), position (832, 640).
 * - `serverCopyBridge`: map 0272, Amaterasu Bridge of the second server (picture S206PACK, 1408 x 640), position (776, 224).
 * - `serverCopyPark`: map 028C, Central Park of the second server (picture S331PACK, 1664 x 1280), position (832, 640).
 * - `battleForm`: a battle (0600) where the first partner fights as Dinohumon (386) and the form it starts in, the one the
 *   game marks, is Kyukimon (389): the lit row on Equipo is the fighter's.
 * - `battleOwnForm`: the same battle, the partner in its own form (Kotemon, 383): no row is lit.
 * - `battleAttacks`: the same battle with the partner as Dinohumon (386, a form with many techniques learned) in a fire
 *   field that boosts fire by half (64 of 128): the Equipo's mock `battleForm` with the attacks of the Batalla screen.
 * - `battleStage`: a battle (0600) on fight stage 22, another picture than the snapshots' 12 and 15.
 * - `battleNoStage`: a battle on fight stage 40, which the game has no fight picture of: the screen fills flat.
 * - `battleOne`, `battleMany`, `battleLongNames`: the `battleAttacks` battle with the partner as Kyukimon at level 5 (one
 *   attack), as Kyukimon at 99 (six: the list scrolls) and as GranKuwagamon (the longest names).
 * - `battleFieldNeutral`: the `battleAttacks` battle in a field without an element. `battleOneElement`,
 *   `battleTwoElements`, `battleFiveElements`: enemies (Pharaohmon, MasterTyrannomon, Maildramon) with one, two and
 *   five elements to be weak or strong to.
 * - `battleBench`: the `battleAttacks` battle with the other two partners holding Angemon (second) and Kyubimon (third):
 *   tapping their portraits lists their attacks. `battleBenchUnknown`: the third one holds a form the data does not have.
 * - `battleFieldNeutral`: the `battleAttacks` battle in a field without an element. `battleOneElement`,
 *   `battleTwoElements`, `battleFiveElements`: enemies with one, two and five elements to be weak or strong to.
 * - `battleNeutral`: an enemy with no element to be weak or strong to (Ogremon). `battleWideField`: the machine field,
 *   whose two effects have the longest words.
 * - `mapActors`: map 028C as `serverCopyPark`, with the characters the game had on the field there (read on the device)
 *   as `FieldActorsChanged`, and the one on the bridge (0xB2) walking along it, as the core sends them every 500 ms.
 * - `mapFollowWide`: map 0290 (West Wire Forest, S351PACK, 1536 x 768) with the player at (1329, 405), 86.5 % across: where
 *   `cover` hid the dot. `mapFollowTall`: map 02B6 (Bullet Valley, S576PACK, 768 x 1152) with the player in its lower part.
 *   `mapFollowFits`: map 0291 (S356PACK, 1024 x 896), whose proportions are the panel's: nothing to pan, no button.
 * - `mapPlazaNear`, `mapPlazaFar`: map 0271 (Amaterasu City), whose El Dorado exit stands at the door of the domed
 *   building (372, 208), with the player next to it or far from it.
 * - `auctionNone`: no auction open (Hazard Fang already taken part in).
 * - `auctionPoor`: the Hazard Fang auction is open but the player has 100 Bits (needs 2700).
 * - `auctionEarly`: main quest back at step 30, so the Sniper Shield auction is open, the Divine
 *   Barrier one was missed, the Hazard Shield one taken and the rest are still to come.
 * The connection cases (`mockConnectionCases`) leave the snapshot as it is.
 * Returns false when the name is not a case.
 */
export function applyMockCase(state: MockState, name: string): boolean {
  if (isMockConnectionCase(name)) {
    return true;
  }

  if (!isMockCase(name)) {
    return false;
  }

  switch (name) {
    case "unknownMap":
      state.player.mapId = "0279";
      break;
    case "digimonBattle":
      state.player.mapId = "0600";
      state.digimonBattle.enemy ??= { ...fallbackEnemy };
      break;
    case "cardBattle":
      state.player.mapId = "0700";
      state.cardBattle.id = 1;
      break;
    case "seabed":
      state.player.mapId = "02E5";
      state.player.seabedRoute = 1;
      state.player.mapVariant = 1;
      break;
    case "seabedPelche":
      state.player.mapId = "02E3";
      state.player.seabedRoute = 5;
      state.player.mapVariant = 1;
      break;
    case "seabedAmaterasu":
      state.player.mapId = "02E3";
      state.player.seabedRoute = 13;
      state.player.mapVariant = 1;
      break;
    case "mobius":
      state.player.mapId = "0258";
      state.player.mapVariant = 1;
      state.player.positionX = 600;
      state.player.positionY = 500;
      break;
    case "mobiusAmaterasu":
      state.player.mapId = "02C1";
      state.player.mapVariant = 1;
      state.player.positionX = 600;
      state.player.positionY = 500;
      break;
    case "centralPark":
      state.player.mapId = "021D";
      state.player.positionX = 832;
      state.player.positionY = 640;
      break;
    case "serverCopyBridge":
      state.player.mapId = "0272";
      state.player.positionX = 776;
      state.player.positionY = 224;
      break;
    case "serverCopyPark":
      state.player.mapId = "028C";
      state.player.positionX = 832;
      state.player.positionY = 640;
      break;
    case "battleOne":
    case "battleMany":
    case "battleLongNames":
    case "battleNeutral":
    case "battleWideField":
    case "battleFieldNeutral":
    case "battleOneElement":
    case "battleTwoElements":
    case "battleFiveElements":
    case "battleBench":
    case "battleBenchUnknown":
    case "battleAttacks":
      state.digimonBattle.field =
        name === "battleWideField" ? 7 : name === "battleFieldNeutral" ? 0 : 2;
      state.digimonBattle.fieldAmount = name === "battleFieldNeutral" ? 0 : 64;
    // falls through: the rest is the battle of `battleForm`
    case "battleForm":
    case "battleOwnForm": {
      state.player.mapId = "0600";
      state.digimonBattle.enemy ??= { ...fallbackEnemy };
      const first = state.party?.slots.find((slot) => slot.digimon !== null);
      if (first?.digimon) {
        first.digimon.inBattle.formId = name === "battleOwnForm" ? 383 : 386;
        const form = battleForms[name];
        if (form !== undefined) {
          first.digimon.inBattle.formId = form.id;
          first.digimon.storedDigievolutions = [{ digievolutionId: form.id, level: form.level }];
        }
        const enemy = battleEnemies[name];
        if (enemy !== undefined && state.digimonBattle.enemy) {
          state.digimonBattle.enemy.id = enemy.id;
          state.digimonBattle.enemy.groupId = enemy.groupId;
        }
      }
      if (name === "battleBench" || name === "battleBenchUnknown") {
        // The bench partners hold the forms their fighter slots start in: Angemon (20) for the second and Kyubimon
        // (374) for the third, or, for `battleBenchUnknown`, a form in the third one's slots the data does not have.
        const [, second, third] = state.party?.slots.filter((slot) => slot.digimon !== null) ?? [];
        if (second?.digimon) {
          second.digimon.inBattle.formId = 20;
        }
        if (third?.digimon) {
          const unknown = name === "battleBenchUnknown";
          third.digimon.inBattle.formId = unknown ? 400 : 374;
          if (unknown) {
            third.digimon.storedDigievolutions = [
              ...third.digimon.storedDigievolutions,
              { digievolutionId: 400, level: 10 },
            ];
          }
        }
      }
      break;
    }
    case "battleStage":
    case "battleNoStage":
      state.player.mapId = "0600";
      state.digimonBattle.enemy ??= { ...fallbackEnemy };
      state.digimonBattle.stage = name === "battleStage" ? 22 : 40;
      break;
    case "mapActors":
      state.player.mapId = "028C";
      state.player.positionX = 1454;
      state.player.positionY = 274;
      break;
    case "mapFollowWide":
      state.player.mapId = "0290";
      state.player.positionX = 1329;
      state.player.positionY = 405;
      break;
    case "mapPlazaNear":
    case "mapPlazaFar":
      state.player.mapId = "0271";
      state.player.positionX = name === "mapPlazaNear" ? 390 : 880;
      state.player.positionY = name === "mapPlazaNear" ? 240 : 420;
      break;
    case "mapFollowTall":
      state.player.mapId = "02B6";
      state.player.positionX = 384;
      state.player.positionY = 940;
      break;
    case "mapFollowFits":
      state.player.mapId = "0291";
      state.player.positionX = 512;
      state.player.positionY = 448;
      break;
    case "auctionNone":
      state.auctions = { ...state.auctions, hazardFang: true };
      break;
    case "auctionPoor":
      state.player.bits = 100;
      break;
    case "auctionEarly":
      for (const step of state.journal?.mainQuest?.steps ?? []) {
        step.isDone = step.number <= 30;
      }
      state.auctions = {
        divineBarrier: false,
        hazardShield: true,
        sniperShield: false,
        dramonShield: false,
        yinYangWand: false,
        hazardFang: false,
      };
      break;
  }

  return true;
}

/** The characters of the map a case has on the field, or null when the case has none. */
export function mockActors(name: string): FieldActorDTO[] | null {
  if (name !== "mapActors") {
    return null;
  }

  // Map 028C as the game had it: the character, then where it stood (0x3F is a sign: it has no picture).
  return [
    { key1: 0x3f, x: 961, y: 681, dir: 1 },
    { key1: 0x89, x: 923, y: 374, dir: 7 },
    { key1: 0x9d, x: 1072, y: 665, dir: 1 },
    { key1: 0x9e, x: 864, y: 577, dir: 5 },
    { key1: 0x9f, x: 560, y: 728, dir: 7 },
    { key1: 0xa0, x: 401, y: 785, dir: 1 },
    { key1: 0xa1, x: 913, y: 233, dir: 3 },
    { key1: 0xa2, x: 688, y: 633, dir: 1 },
    { key1: 0xb2, x: 1448, y: 236, dir: 7 },
    { key1: 0x13d, x: 1472, y: 224, dir: 1 },
  ];
}
