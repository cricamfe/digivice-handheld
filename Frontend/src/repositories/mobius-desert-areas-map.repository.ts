import DesertAreasMapJson from "@/database/mobius-desert/mobius-desert-areas-map.json";
import DesertAreasMapAmaterasuJson from "@/database/mobius-desert/mobius-desert-areas-map-amaterasu.json";
import type { GameServer } from "@/types/game-server.type";
import type { MobiusDesertAreaMapCellRaw } from "./tables/raws/mobius-desert/mobius-desert-area-map-cell.raw";
import type { MobiusDesertAreasMapTable } from "./tables/desert/mobius-desert-areas-map.table";

export class MobiusDesertAreasMapRepository {
  private static readonly desertAreasMapTable = DesertAreasMapJson as MobiusDesertAreasMapTable;
  private static readonly amaterasuAreasMapTable =
    DesertAreasMapAmaterasuJson as MobiusDesertAreasMapTable;

  /** The area with this label in the maps of a server (the other server's maps repeat the labels). */
  public static findByLabel(
    label: string,
    server: GameServer = "asuka",
  ): { locationId: string; cell: MobiusDesertAreaMapCellRaw } | null {
    const table = server === "amaterasu" ? this.amaterasuAreasMapTable : this.desertAreasMapTable;
    for (const [locationId, cells] of Object.entries(table)) {
      const cell = Object.values(cells).find((candidateCell) => candidateCell.label === label);

      if (cell !== undefined) {
        return { locationId, cell };
      }
    }

    return null;
  }

  public static getMobiusDesertArea(
    locationId: string,
    mapVariantKey: string,
  ): MobiusDesertAreaMapCellRaw | null {
    const cells = this.desertAreasMapTable[locationId] ?? this.amaterasuAreasMapTable[locationId];
    return cells?.[mapVariantKey] ?? null;
  }
}
