import i18n from "@/i18n/handheld";
import type { DigimonVariant } from "@/types/digimon-variant.type";
import { gameWord } from "./language";

/**
 * The name of a Digimon by its line of the game's Digimon names (`dignam:212`), read from the user's disc, so the
 * bundle ships no name. The variant that tells apart the enemies the game names alike follows in the app's own words (`Kuwagamon(Verde)`), a letter as it is.
 * Without the line in the extracted text, a generic name of the app's own with the line's number.
 */
export function digimonName(nameId: number, variant?: DigimonVariant): string {
  const t = i18n.global.t as (key: string, named?: Record<string, unknown>) => string;
  const name = gameWord(`dignam:${nameId}`, "") || t("handheld.digimon.unknown", { id: nameId });
  if (variant === undefined) {
    return name;
  }

  const word = /^[A-Z]$/.test(variant) ? variant : t(`handheld.digimon.variant.${variant}`);
  return `${name}(${word})`;
}
