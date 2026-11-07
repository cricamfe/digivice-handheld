import partners from "@/database/digimon.json";
import enemies from "@/database/enemy.json";
import evolutions from "@/database/digievolution/digievolution.json";
import cards from "@/handheld/card-table";
import maps from "@/database/map.json";
import { gameAssetUrl } from "@/handheld/game-assets";
import { fieldMapImageKey } from "@/handheld/field-image-keys";

// The maps the tables know, by id: a picture is the one of its own field mode.
const mapIds = new Set(Object.keys(maps));

export class ImageCatalog {
  /** The picture of a map of the tables, by its id (`0202`); null for scenes and maps without one. */
  public static getLocationImageUrl(mapId: string | null): string | null {
    if (mapId === null || !mapIds.has(mapId)) return null;
    const key = fieldMapImageKey(Number.parseInt(mapId, 16));
    return key === null ? null : gameAssetUrl("", key);
  }

  /** The portrait of a partner, by its id (0 to 7, as digimon.json numbers them). */
  public static getDigimonImageUrl(partnerId: number | null): string | null {
    return partnerId !== null && String(partnerId) in partners
      ? gameAssetUrl("portraits", String(partnerId))
      : null;
  }

  /** The picture of an enemy, by its id in enemy.json: drawn from the model of its enemy id (`memoryId`). */
  public static getEnemyImageUrl(enemyId: string | null): string | null {
    const enemy =
      enemyId === null ? undefined : (enemies as Record<string, { memoryId: number }>)[enemyId];
    return enemy === undefined ? null : gameAssetUrl("enemies", String(enemy.memoryId));
  }

  /** The picture of a digievolution, by its id in digievolution.json. */
  public static getDigievolutionImageUrl(evolutionId: number | null): string | null {
    return evolutionId !== null && String(evolutionId) in evolutions
      ? gameAssetUrl("digievolutions", String(evolutionId))
      : null;
  }

  /** A character of the field as the game draws it, by its character and direction (`59-0`). */
  public static getFieldActorImageUrl(actor: string | null | undefined): string | null {
    return actor ? gameAssetUrl("actors", actor) : null;
  }

  public static getCardImageUrl(name: string | null): string | null {
    if (name === null) return null;
    // The handheld table is populated after this module loads.
    const id = Object.entries(cards).find(([, card]) => card.imageName === name)?.[0];
    return id === undefined ? null : gameAssetUrl("cards", id);
  }

  public static getBattleStageImageUrl(stage: number): string | null {
    return Number.isSafeInteger(stage) && stage >= 0
      ? gameAssetUrl("battle-stages", String(stage))
      : null;
  }
}
