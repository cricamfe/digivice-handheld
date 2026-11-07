import type { DigimonDigievolutionRaw } from "./digimon-digievolution.raw";

export interface DigimonRaw {
  /** Line of the game's Digimon names: partner i is line i + 1. */
  nameId: number;
  experience: Record<string, number>;
  digievolutions: DigimonDigievolutionRaw;
}
