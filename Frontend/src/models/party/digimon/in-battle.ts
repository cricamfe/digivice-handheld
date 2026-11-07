import type { Vital } from "./vital";

export interface InBattle {
  /** The Digimon the fighter is now: its own id, or the form it digivolved to (only meaningful in a battle). */
  formId: number;
  condition: number;
  strength: number;
  defense: number;
  speed: number;
  hp: Vital;
  mp: Vital;
}
