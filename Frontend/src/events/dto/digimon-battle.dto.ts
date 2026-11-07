import type { EnemyDTO } from "./battles/enemy.dto";

export interface DigimonBattleDTO {
  field?: number;
  fieldAmount?: number;
  activeFighter?: number;
  stage?: number;
  enemy?: EnemyDTO | null;
}
