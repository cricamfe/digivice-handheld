import SeabedEnemiesJson from "@/database/seabed/seabed-enemy.json";
import SeabedRoutesJson from "@/database/seabed/seabed-route.json";
import type { SeabedRouteLocationRaw } from "./tables/raws/seabed/seabed-route-location.raw";
import type { SeabedEnemyTable } from "./tables/seabed/seabed-enemy.table";
import type { SeabedRouteTable } from "./tables/seabed/seabed-route.table";

export class SeabedRoutesRepository {
  /**
   * The game numbers the routes of the Amaterasu server after the Asuka ones: route 11 is route 1 on the second server
   * (GAME.place). Their tunnels are the same maps, wired the same way, so the maps of a route are stored once.
   */
  public static readonly amaterasuRouteOffset = 10;

  private static readonly seabedRoutesTable = SeabedRoutesJson as SeabedRouteTable;
  private static readonly seabedEnemiesTable = SeabedEnemiesJson as SeabedEnemyTable;

  public static getAll(): SeabedRouteTable {
    return this.seabedRoutesTable;
  }

  /** The route as the Asuka server numbers it (1 to 9): the same for both servers. */
  public static normalizeRoute(routeId: string): string {
    const route = Number(routeId);
    return route > this.amaterasuRouteOffset ? String(route - this.amaterasuRouteOffset) : routeId;
  }

  public static getByRouteAndLocation(
    routeId: string,
    locationKey: string,
  ): SeabedRouteLocationRaw | null {
    return this.seabedRoutesTable[this.normalizeRoute(routeId)]?.maps[locationKey] ?? null;
  }

  /** The enemies of the route on the server it is of: their levels are not the same on both. */
  public static getEnemiesByRoute(routeId: string): string[] {
    return this.seabedEnemiesTable[routeId]?.walking ?? [];
  }
}
