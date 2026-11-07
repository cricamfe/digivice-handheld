import type { ResistancesRaw } from "./resistances.raw";
import type { AttributesRaw } from "./attributes.raw";
import type { ElementsRaw } from "./elements.raw";
import type { DigievolutionTechniqueRaw } from "./digievolution-technique.raw";
import type { DigievolutionTier } from "@/types/digievolution-tier.type";

export interface DigievolutionRaw {
  /** Line of the game's Digimon names (`dignam:12`), from the form table of the disc. */
  nameId: number;
  tier: DigievolutionTier;
  attributes: AttributesRaw;
  elements: ElementsRaw;
  resistances: ResistancesRaw;
  techniques: DigievolutionTechniqueRaw[];
}
