import { gameWord } from "./language";
import type { Texts } from "./map-view";

/**
 * The places of the game the screens name by themselves (a title, a sentence of the auctions), as references to the
 * game's place names on the disc (`stname:85`), for `gameWord`.
 */
export const ScreenPlaces = {
  seabed: "stname:133",
  mobiusDesert: "stname:85",
  cargoTower: "stname:13",
  undergroundPath: "stname:135",
  biosSwamp: "stname:50",
  masterRoom: "stname:21",
} as const;

export type ScreenPlace = keyof typeof ScreenPlaces;

/** The name of a place as the game has it; without its line in the extracted text, a generic word of the app's own. */
export function placeWord(texts: Texts, place: ScreenPlace): string {
  return gameWord(ScreenPlaces[place], "") || texts.t(`handheld.place.${place}`);
}
