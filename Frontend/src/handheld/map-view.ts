import { ref } from "vue";
import type { State } from "@/models";
import { MapPresenter } from "@/presenters/map/map.presenter";
import { SeabedRoutesRepository } from "@/repositories/seabed-routes.repository";
import { MapService } from "@/services/map.service";
import type { GameServer } from "@/types/game-server.type";
import { gameWord } from "./language";
import { PlaceWords } from "./map-exits";
import type { ScreenName } from "./router";
import { ServerCopyData, type ServerCopy } from "./server-copies";

/** Texts the view builders need from vue-i18n. */
export interface Texts {
  t: (key: string, named?: Record<string, unknown>) => string;
  /** Whether a key exists in `locale`, or in the current locale without it. */
  te: (key: string, locale?: string) => boolean;
}

/** The game data is in English, and `es-ES` does not carry it: it comes through the fallback locale. */
const GameDataLocale = "en-US";

/** Translates a key of the game data, or returns null when no language has it. */
export function gameText(
  texts: Texts,
  key: string,
  named?: Record<string, unknown>,
): string | null {
  if (!texts.te(key, GameDataLocale) && !texts.te(key)) {
    return null;
  }

  return texts.t(key, named);
}

/** Map ids of whole-screen scenes, with a picture of the scene in place of a map: menu, shops, rewards and the like. */
const SceneIds = new Set(["0500", "0A00", "0D00", "0F00", "1000", "1300", "1400"]);

/** Whether the id is a scene the player opens over a place, not a place. */
export function isScene(mapId: string): boolean {
  return SceneIds.has(mapId);
}

const lastPlace = ref<string | null>(null);

function isPlace(mapId: string): boolean {
  return !isScene(mapId) && !MapPresenter.isInBattle(mapId) && !MapPresenter.isInCardBattle(mapId);
}

/**
 * Notes where the player is, so a scene opened there can go on showing it. If the app starts inside a scene, the
 * map the game remembers as the previous one is the best guess.
 */
export function rememberPlace(mapId: string, previousMapId: string): void {
  if (isPlace(mapId)) {
    lastPlace.value = mapId;
  } else if (lastPlace.value === null && isPlace(previousMapId)) {
    lastPlace.value = previousMapId;
  }
}

/**
 * The maps the game repeats for its second server (Amaterasu, Qing Long, Zhu Que, Bai Hu), as `[first id, last id, id
 * of the first original]`. The map table only has the originals. From the list of maps on the disc, where each copy
 * follows its original; some have no copy.
 */
const ServerCopies: readonly (readonly [number, number, number])[] = [
  [0x270, 0x273, 0x200],
  [0x274, 0x29b, 0x205],
  [0x29c, 0x2a3, 0x22e],
  [0x2a4, 0x2af, 0x237],
  [0x2b0, 0x2b0, 0x244],
  [0x2b1, 0x2bf, 0x247],
  [0x2c0, 0x2c7, 0x257],
  [0x2c8, 0x2d4, 0x260],
  [0x2d5, 0x2d6, 0x26e],
];

/** The original of a map the game repeats for its second server, or the map itself. */
export function originalOf(mapId: string): string {
  const id = parseInt(mapId, 16);
  const range = ServerCopies.find(([first, last]) => id >= first && id <= last);
  return range === undefined
    ? mapId
    : (range[2] + id - range[0]).toString(16).toUpperCase().padStart(4, "0");
}

/**
 * The server the player is on. In the Seabed it is the number of the route (GAME.place: Amaterasu's start after the
 * Asuka ones, and the tunnels are shared); everywhere else the place, since the Amaterasu server has copies of its own.
 */
export function serverOf(state: State): GameServer {
  const { seabedRoute, mapId } = state.player;
  if (seabedRoute !== null && seabedRoute > SeabedRoutesRepository.amaterasuRouteOffset) {
    return "amaterasu";
  }

  return serverCopyOf(shownPlaceOf(mapId)) === null ? "asuka" : "amaterasu";
}

/** The map the game repeats `mapId` as on its second server, or null when it has no copy there. */
export function copyOf(mapId: string): string | null {
  const id = parseInt(mapId, 16);
  const range = ServerCopies.find(([first, last, original]) => {
    return id >= original && id <= original + (last - first);
  });
  return range === undefined
    ? null
    : (range[0] + id - range[2]).toString(16).toUpperCase().padStart(4, "0");
}

/**
 * The place to show for a map id, as the game has it: a scene shows the place it was opened in, if one is known yet.
 * A copy for the second server stays a copy here, which has its own picture and name.
 */
export function shownPlaceOf(mapId: string): string {
  return isScene(mapId) ? (lastPlace.value ?? mapId) : mapId;
}

/** The picture and the name of a copy for the second server, or null for any other map. */
export function serverCopyOf(mapId: string): ServerCopy | null {
  return ServerCopyData[mapId] ?? null;
}

/**
 * The place to take the rules from (screen, region, markers, enemies): a copy is its original, which is the one the
 * data is about.
 */
export function placeOf(mapId: string): string {
  return originalOf(shownPlaceOf(mapId));
}

/** The screens the Mapa tab chooses from on its own. */
export type MapKind = Extract<
  ScreenName,
  "mapa" | "batalla" | "batallaCartas" | "seabedRuta" | "mobius"
>;

/** Screen of the Mapa tab for the place the player is in, with the rules the original map uses. */
export function mapKindOf(mapId: string): MapKind {
  if (MapPresenter.isInBattle(mapId)) {
    return "batalla";
  }

  if (MapPresenter.isInCardBattle(mapId)) {
    return "batallaCartas";
  }

  const region = MapService.getRegionByMapId(mapId);
  if (region === "seabed") {
    return "seabedRuta";
  }

  if (region === "mobiusDesert") {
    return "mobius";
  }

  return "mapa";
}

const SeabedFirstId = 0x2e0;
const SeabedLastId = 0x2e7;

/** Number of a Seabed map (`02E5` is Seabed 5), or null for any other place. */
export function seabedNumber(mapId: string): number | null {
  const id = Number.parseInt(mapId, 16);
  if (Number.isNaN(id) || id < SeabedFirstId || id > SeabedLastId) {
    return null;
  }

  return id - SeabedFirstId;
}

/**
 * Name of a map as the game has it, for any id: a copy for the second server has its own name (a reference to the
 * game's place names), any other map is named as `locationName` does. Null when no name is known (yet).
 */
export function mapName(texts: Texts, mapId: string): string | null {
  const copy = serverCopyOf(mapId);
  if (copy !== null) {
    return gameWord(copy.word, "") || null;
  }

  return locationName(texts, originalOf(mapId));
}

/** The four sectors of the world map, named by the game's sector names (STAREA), for `gameWord`. */
export const SectorWords: Readonly<Record<string, string>> = {
  "0001": "starea:12",
  "0002": "starea:13",
  "0003": "starea:14",
  "0004": "starea:15",
};

/** The scenes of saving the game, by the place of the game's place names where the player saves, for `gameWord`. */
export const SavingWords: Readonly<Record<string, string>> = {
  "0C01": "stname:8",
  "0C02": "stname:45",
  "0C03": "stname:53",
  "0C04": "stname:60",
  "0C12": "stname:32",
  "0C13": "stname:88",
  "0C14": "stname:70",
};

/**
 * Name of a place, with the number the Seabed maps carry. The few maps the messages of the game's text do not name are
 * named by a reference to its place names (`PlaceWords`, `SectorWords`); a scene of saving says where the player saves.
 * Null when the map has no name, or when the extracted text lacks its line.
 */
export function locationName(texts: Texts, mapId: string): string | null {
  const saving = SavingWords[mapId];
  if (saving !== undefined) {
    const place = gameWord(saving, "");
    return place === "" ? null : texts.t("handheld.map.saving", { place });
  }

  const mode = Number(`0x${mapId}`);
  // The game's field banners use STNAME[134] for every Circuit Board sector.
  const nameId = mode >= 0x2e8 && mode <= 0x2ee ? "02EC" : mapId;
  const word = PlaceWords[mapId] ?? SectorWords[mapId];
  const name =
    gameText(texts, `map.${nameId}`) ?? (word === undefined ? null : gameWord(word, "") || null);
  if (name === null) {
    return null;
  }

  const number = seabedNumber(mapId);
  return number === null ? name : `${name} ${number}`;
}
