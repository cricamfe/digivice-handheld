import type { SeabedRouteLocationRaw } from "./seabed-route-location.raw";

export interface SeabedRouteRaw {
  maps: Record<string, SeabedRouteLocationRaw>;
}
