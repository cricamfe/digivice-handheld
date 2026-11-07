import type { EnemyRaw } from "@/repositories/tables/raws/enemy/enemy.raw";
import type { EnemyResumedViewModel } from "@/viewmodels/enemy/enemy-resumed.viewmodel";
import { EnemyRepository } from "@/repositories/enemy.repository";

export class EnemyResumedConverter {
  public static convert(
    enemyId: string,
    enemyRaw: EnemyRaw,
    origins: { walking: boolean; fishing: boolean; kickingTree: boolean },
  ): EnemyResumedViewModel {
    return {
      id: enemyId,
      name: EnemyRepository.getName(enemyRaw),
      boss: enemyRaw.boss === true,
      walking: origins.walking,
      fishing: origins.fishing,
      kickingTree: origins.kickingTree,
    };
  }
}
