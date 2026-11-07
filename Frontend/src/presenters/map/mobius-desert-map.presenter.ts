import type { Quest } from "@/models";
import { MobiusDesertEnemyRepository } from "@/repositories/mobius-desert-enemy.repository";
import { MapService } from "@/services/map.service";
import { MobiusDesertService } from "@/services/mobius-desert.service";
import { QuestService } from "@/services/quest.service";
import type { DesertAreaMapCellViewModel } from "@/viewmodels/desert/desert-area-map-cell.viewmodel";

export class MobiusDesertMapPresenter {
  public static getEnemyIds(locationId: string, mainQuest: Quest): string[] {
    // The maps of the Amaterasu server have enemies of their own, at other levels, and no boss (the map table has none).
    const ownIds = MobiusDesertEnemyRepository.findWalkingByLocationId(locationId);
    if (ownIds !== null) {
      return ownIds;
    }

    const walkingIds = MapService.getWalkingEnemies(
      locationId,
      QuestService.getLastCompletedMainQuestStep(mainQuest),
    );
    const bossIds = MapService.getBoss(locationId);

    return [...bossIds, ...walkingIds];
  }

  public static getMobiusDesertArea(
    locationId: string,
    mapVariant: number | null,
  ): DesertAreaMapCellViewModel | null {
    return MobiusDesertService.getMobiusDesertArea(locationId, mapVariant);
  }
}
