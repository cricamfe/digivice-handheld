import type { TamerCardBattleRaw } from "./tamer-card-battle.raw";
import type { TamerDigimonBattleRaw } from "./tamer-digimon-battle.raw";

export interface TamerRaw {
  /** The character of the field that draws them, by character and direction (`57-1`, actors/57-1). */
  fieldActor?: string | null;
  locationId: string;
  cardBattles?: Record<string, TamerCardBattleRaw>;
  digimonBattles?: Record<string, TamerDigimonBattleRaw>;
}
