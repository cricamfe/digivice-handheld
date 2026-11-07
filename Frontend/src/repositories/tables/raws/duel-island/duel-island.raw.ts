import type { TamerCardBattleRaw } from "@/repositories/tables/raws/tamer/tamer-card-battle.raw";
import type { TamerDigimonBattleRaw } from "@/repositories/tables/raws/tamer/tamer-digimon-battle.raw";

export interface DuelIslandRaw {
  /** The field character the rival stands as (`206-0`: character and direction), for its picture. */
  fieldActor?: string | null;
  locationId: string;
  cardBattles?: Record<string, TamerCardBattleRaw>;
  digimonBattles?: Record<string, TamerDigimonBattleRaw>;
}
