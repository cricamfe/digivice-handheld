import type { EnemyDropRaw } from "./enemy-drop.raw";
import type { EnemyLocationRaw } from "./enemy-location.raw";
import type { DigimonSpecies } from "@/types/digimon-species.type";
import type { DigimonVariant } from "@/types/digimon-variant.type";

export interface EnemyRaw {
  /** Line of the game's Digimon names (`dignam:212`), from the battle table of the disc. */
  nameId: number;
  /** What tells apart the enemies the game names alike: a colour of the app's own, or a letter. */
  variant?: DigimonVariant;
  /** The field character a boss stands as before its battle (`59-0`: character and direction), for its picture. */
  fieldActor?: string;
  level: number;
  hp: number;
  species: DigimonSpecies;
  rate: number;
  strength: number;
  defense: number;
  spirit: number;
  wisdom: number;
  speed: number;
  fire: number;
  water: number;
  ice: number;
  wind: number;
  thunder: number;
  machine: number;
  dark: number;
  canPoison: boolean;
  poison: number;
  canParalyze: boolean;
  paralyze: number;
  canConfuse: boolean;
  confuse: number;
  canSleep: boolean;
  sleep: number;
  canKO: boolean;
  ko: number;
  canDrain: boolean;
  canSteal: boolean;
  strDown: string;
  defDown: string;
  spdDown: string;
  canEscape: boolean;
  dvexp?: number;
  exp?: number;
  bits?: number;
  drops?: EnemyDropRaw[];
  locations?: EnemyLocationRaw[];
  regularAttackId: string | null;
  techniqueId: string | null;
  boss?: boolean;
  memoryId: number;
  groupId: number | null;
}
