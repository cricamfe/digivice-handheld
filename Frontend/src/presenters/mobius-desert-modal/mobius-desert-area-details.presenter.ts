import { ImageCatalog } from "@/catalogs/image.catalog";
import { MobiusDesertAreasMapRepository } from "@/repositories/mobius-desert-areas-map.repository";
import type { DesertAreaDetailsViewModel } from "@/viewmodels/desert/desert-area-details.viewmodel";
import type { DesertAreaViewModel } from "@/viewmodels/desert/desert-area.viewmodel";
import type { GameServer } from "@/types/game-server.type";
import type { MobiusDesertAreaType } from "@/types/mobius-desert-area-type.type";

/** The maps whose picture an area of those kinds shows: S Noise Desert and Mirage Tower. */
const MapIdByAreaType: Partial<Record<MobiusDesertAreaType, string>> = {
  noiseDesertS: "0257",
  mirageTower: "025A",
};

export class MobiusDesertAreaDetailsPresenter {
  public static getAreaDetails(
    area: DesertAreaViewModel | null,
    server: GameServer = "asuka",
  ): DesertAreaDetailsViewModel | null {
    if (area === null) {
      return null;
    }

    if (area.type === "noiseDesertS" || area.type === "mirageTower") {
      return {
        locationId: null,
        imageUrl: ImageCatalog.getLocationImageUrl(MapIdByAreaType[area.type] ?? null),
        coordinates: null,
      };
    }

    if (area.type !== "normal") {
      return null;
    }

    const areaRaw = MobiusDesertAreasMapRepository.findByLabel(area.label, server);

    if (areaRaw === null) {
      return null;
    }

    return {
      locationId: areaRaw.locationId,
      imageUrl: ImageCatalog.getLocationImageUrl(areaRaw.locationId),
      coordinates: areaRaw.cell.coordinates ?? null,
    };
  }
}
