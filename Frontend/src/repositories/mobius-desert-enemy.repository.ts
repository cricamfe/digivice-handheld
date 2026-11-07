import DesertEnemiesJson from "@/database/mobius-desert/mobius-desert-enemy.json";
import type { MobiusDesertEnemyTable } from "./tables/desert/mobius-desert-enemy.table";

export class MobiusDesertEnemyRepository {
  private static readonly desertEnemiesTable = DesertEnemiesJson as MobiusDesertEnemyTable;

  /** The enemies of a map of the Amaterasu server, or null for any other map (their data is in the map table). */
  public static findWalkingByLocationId(locationId: string): string[] | null {
    return this.desertEnemiesTable[locationId]?.walking ?? null;
  }
}
