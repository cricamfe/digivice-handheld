import type { Enemy } from "./battle/enemy";

export interface DigimonBattle {
  field: number;
  /** How much the field boosts its element, in 128ths. */
  fieldAmount: number;
  /** The player's fighter in the field, 0 to 2 (the party slot index minus one). */
  activeFighter: number;
  /** The fight stage the battle is on (`BATTLE_SETUP.stage`), an index of the game's table of 57; 0 for none. */
  stage: number;
  enemy: Enemy | null;
}
