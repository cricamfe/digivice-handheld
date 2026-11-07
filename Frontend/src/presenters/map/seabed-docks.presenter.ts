import { ImageCatalog } from "@/catalogs/image.catalog";
import { SeabedDirectionConverter } from "@/presenters/converter/seabed-direction.converter";
import { SeabedDirectionsRepository } from "@/repositories/seabed-direction.repository";
import type { SeabedDirectionViewModel } from "@/viewmodels/seabed-modal/seabed-direction.viewmodel";

export class SeabedDocksPresenter {
  public static getAsukaImageUrl(): string | null {
    return ImageCatalog.getLocationImageUrl("Asuka");
  }

  /**
   * The second server's world map: the game's own picture of it (`STWMP1`), laid out like the Asuka one, so the points
   * measured on `Asuka.webp` fall on the same places.
   */
  public static getAmaterasuImageUrl(): string | null {
    return ImageCatalog.getLocationImageUrl("Amaterasu");
  }

  public static getRoutes(): SeabedDirectionViewModel[] {
    const seabedDirectionsTable = SeabedDirectionsRepository.getAll();

    return Object.entries(seabedDirectionsTable).map(([routeId, seabedDirectionRaw]) => {
      return SeabedDirectionConverter.convert(routeId, seabedDirectionRaw);
    });
  }
}
