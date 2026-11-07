import type { InnerLocationRaw } from "@/repositories/tables/raws/map/inner-location.raw";
import type { CoordinatesRaw } from "./coordinates.raw";
import type { RequisiteRaw } from "./requisite.raw";

export interface StepRaw {
  requisites: RequisiteRaw[];
  location: string;
  /** Missing when the game files have no picture of the place. */
  coordinates?: CoordinatesRaw;
  innerLocation?: InnerLocationRaw[];
}
