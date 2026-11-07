import type { State } from "@/models";
import { MapPresenter } from "@/presenters/map/map.presenter";
import { MapEnemiesPresenter } from "@/presenters/map/map-enemies.presenter";
import { SeabedDocksPresenter } from "@/presenters/map/seabed-docks.presenter";
import { SeabedMapPresenter } from "@/presenters/map/seabed-map.presenter";
import { SeabedPresenter } from "@/presenters/map/seabed.presenter";
import { SeabedModalPresenter } from "@/presenters/seabed-modal/seabed-modal.presenter";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { SeabedRoutesRepository } from "@/repositories/seabed-routes.repository";
import type { GameServer } from "@/types/game-server.type";
import type { SeabedDirection } from "@/types/seabed-direction.type";
import { attempt } from "./attempt";
import { gameWord } from "./language";
import {
  copyOf,
  locationName,
  originalOf,
  placeOf,
  seabedNumber,
  serverCopyOf,
  serverOf,
  type Texts,
} from "./map-view";
import {
  SeabedDockPlaces,
  SeabedLandings,
  SeabedRouteDocks,
  SeabedRouteNotes,
  type SeabedNote,
} from "./seabed-docks";

/** The route as the Asuka server numbers it, 1 to 9: the number the player sees on both servers. */
export function routeNumber(route: number): number {
  return Number(SeabedRoutesRepository.normalizeRoute(String(route)));
}

/**
 * Name of a place of the Seabed (a tunnel or a dock) on a server: the Amaterasu docks are copies of the Asuka ones
 * with a name of their own (`server-copies.ts`).
 */
function placeName(texts: Texts, mapId: string, server: GameServer): string {
  if (seabedNumber(mapId) !== null) {
    return locationName(texts, mapId) ?? mapId;
  }

  if (server === "amaterasu") {
    // The id is either the copy already (the docks of the game's list) or the Asuka original (the tunnels' data).
    const copy = serverCopyOf(mapId) === null ? copyOf(mapId) : mapId;
    const word = copy === null ? undefined : serverCopyOf(copy)?.word;
    return word === undefined ? mapId : gameWord(word, mapId);
  }

  const dock = SeabedDockPlaces[mapId];
  return locationName(texts, mapId) ?? (dock === undefined ? mapId : gameWord(dock.word, mapId));
}

export interface SeabedDock {
  /** What the dock view opens with: the map and the route as the game has them (`0249:5`). */
  key: string;
  location: string;
  name: string;
  /** Position as a percentage of the world map picture (the same on both servers), null when the data has no point for it: then it has no line. */
  x: number | null;
  y: number | null;
  /** A dock the data marks as closed is shown closed. */
  isOpen: boolean;
}

export interface SeabedRoute {
  id: string;
  /** Number of the route in the game (`GAME.place` on the Asuka server), 1 to 9. */
  number: number;
  docks: SeabedDock[];
  /** The straight pieces of the line that joins the docks, as a percentage of the world map picture. Empty when the data has no line for the route. */
  segments: { from: { x: number; y: number }; to: { x: number; y: number } }[];
  /** What it has besides the way through: chests, a forge, dead ends. */
  notes: readonly SeabedNote[];
}

export interface SeabedOverview {
  server: GameServer;
  imageUrl: string | null;
  routes: SeabedRoute[];
}

/**
 * The routes of the Seabed on the world map, numbered as the game numbers them. The docks of each come from the game
 * (`seabed-docks.ts`); the point of a dock on the picture and the line between two are the data of the map
 * (`seabed-direction.json`), and a dock without a point is listed without a line. Both servers have the same routes
 * and the same world map layout; each has its own picture of it.
 */
export function buildSeabedOverview(texts: Texts, server: GameServer): SeabedOverview {
  const offset = server === "amaterasu" ? SeabedRoutesRepository.amaterasuRouteOffset : 0;
  const drawn = new Map(SeabedDocksPresenter.getRoutes().map((route) => [route.id, route]));
  const numbers = Object.keys(SeabedRouteDocks)
    .map(Number)
    .filter((route) => route <= SeabedRoutesRepository.amaterasuRouteOffset)
    .sort((first, second) => first - second);

  const routes = numbers.map((number): SeabedRoute => {
    const route = drawn.get(String(number));
    return {
      id: String(number),
      number,
      docks: (SeabedRouteDocks[number + offset] ?? []).map((location) => {
        const placed = route?.docks.find((dock) => {
          return dock.location === (server === "amaterasu" ? originalOf(location) : location);
        });

        return {
          key: `${location}:${number + offset}`,
          location,
          name: placeName(texts, location, server),
          x: placed?.x ?? null,
          y: placed?.y ?? null,
          isOpen: placed === undefined || placed.type === "normal",
        };
      }),
      segments: route?.segments ?? [],
      notes: SeabedRouteNotes[number + offset] ?? [],
    };
  });

  // Both pictures are the same map, drawn with the other server's colours and name: the points are valid on either.
  const imageUrl =
    (server === "amaterasu" ? SeabedDocksPresenter.getAmaterasuImageUrl() : null) ??
    SeabedDocksPresenter.getAsukaImageUrl();

  return { server, imageUrl, routes };
}

export interface SeabedExit {
  /** The side of the screen it opens on. */
  on: SeabedDirection;
  name: string;
}

export interface SeabedNeighbor {
  /** First line: the next Seabed map. Following lines: the places it leads to. */
  lines: string[];
}

export interface SeabedEncounter {
  id: string;
  name: string;
  level: number | null;
}

/** Where the player is inside the Seabed, or null when the game is not in a Seabed map. */
export interface SeabedHere {
  title: string;
  imageUrl: string | null;
  emerge: SeabedExit[];
  left: SeabedNeighbor | null;
  right: SeabedNeighbor | null;
  encounters: SeabedEncounter[];
}

export function buildSeabedHere(state: State, texts: Texts): SeabedHere {
  const { seabedRoute, mapVariant } = state.player;
  const server = serverOf(state);
  const mapId = placeOf(state.player.mapId);
  const name = locationName(texts, mapId) ?? texts.t("handheld.map.unknown");
  const route =
    seabedRoute === null
      ? ""
      : ` · ${texts.t("handheld.seabed.route", { number: routeNumber(seabedRoute) })}`;
  const place = attempt(
    () => SeabedPresenter.getRouteLocation(seabedRoute, mapId, mapVariant),
    null,
  );

  const neighbor = (ids: string[] | undefined): SeabedNeighbor | null => {
    if (ids === undefined || ids.length === 0) {
      return null;
    }

    return { lines: ids.map((id) => placeName(texts, id, server)) };
  };

  const enemyIds = attempt(() => SeabedMapPresenter.getEnemyIds(seabedRoute), []);
  const encounters = attempt(() => MapEnemiesPresenter.getResumedEnemiesByIds(enemyIds), []).map(
    (enemy): SeabedEncounter => ({
      id: enemy.id,
      name: enemy.name,
      level: attempt(() => EnemyRepository.getEnemyById(enemy.id).level, null),
    }),
  );

  return {
    title: `${name}${route}`,
    imageUrl: attempt(() => MapPresenter.getByLocationId(mapId).locationImageUrl, null),
    emerge: (place?.emerge ?? []).map((exit) => ({
      on: exit.on,
      name: placeName(texts, exit.location, server),
    })),
    left: neighbor(place?.left),
    right: neighbor(place?.right),
    encounters,
  };
}

/** A dock of the overview, seen from the inside: its picture and where the dock is on it. */
export interface SeabedDockView {
  title: string;
  imageUrl: string | null;
  /** Position of the dock as a percentage of the picture. */
  x: number | null;
  y: number | null;
}

/** The dock the key (`SeabedDock.key`) names, or the first route that has the map when it is a bare map id. */
export function buildSeabedDock(key: string, texts: Texts): SeabedDockView {
  const [location = key, routeText] = key.split(":");
  const route =
    routeText === undefined
      ? Number(
          Object.keys(SeabedRouteDocks).find((candidate) => {
            return SeabedRouteDocks[Number(candidate)]?.includes(location);
          }),
        )
      : Number(routeText);
  const server: GameServer =
    route > SeabedRoutesRepository.amaterasuRouteOffset ? "amaterasu" : "asuka";
  const original = server === "amaterasu" ? originalOf(location) : location;

  // The pin is where the game puts the player when they come out of the Seabed there; the table of the map is the way
  // out of a dock that has no landing of its own.
  const landing = SeabedLandings[`${location}:${route}`];
  const view = attempt(() => SeabedModalPresenter.getViewModel(original), null);
  const imageUrl =
    (serverCopyOf(location) === null ? null : ImageCatalog.getLocationImageUrl(location)) ??
    view?.imageUrl ??
    (SeabedDockPlaces[original] === undefined ? null : ImageCatalog.getLocationImageUrl(original));
  const name = placeName(texts, location, server);

  return {
    title: Number.isNaN(route)
      ? name
      : `${name} · ${texts.t("handheld.seabed.route", { number: routeNumber(route) })}`,
    imageUrl,
    x: landing?.x ?? view?.coordinates.x ?? null,
    y: landing?.y ?? view?.coordinates.y ?? null,
  };
}
