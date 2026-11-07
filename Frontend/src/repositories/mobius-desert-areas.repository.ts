import DesertAreasJson from "@/database/mobius-desert/mobius-desert-areas.json";
import DesertAreasAmaterasuJson from "@/database/mobius-desert/mobius-desert-areas-amaterasu.json";
import type { GameServer } from "@/types/game-server.type";
import type { MobiusDesertAreasRaw } from "./tables/raws/mobius-desert/mobius-desert-areas.raw";
import type { MobiusDesertAreasTable } from "./tables/desert/mobius-desert-areas.table";

export class MobiusDesertAreasRepository {
  private static readonly desertAreasTable = DesertAreasJson as MobiusDesertAreasTable;
  private static readonly amaterasuAreasTable = DesertAreasAmaterasuJson as MobiusDesertAreasTable;

  /** The grid with the notes of a server: the characters are not the same on both. */
  public static getAll(server: GameServer = "asuka"): MobiusDesertAreasRaw {
    return server === "amaterasu" ? this.amaterasuAreasTable : this.desertAreasTable;
  }
}
