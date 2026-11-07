import type { State } from "@/models";
import { JournalPresenter } from "@/presenters/journal/journal.presenter";
import { MapPresenter } from "@/presenters/map/map.presenter";
import { WikiLocationsPanelPresenter } from "@/presenters/map/wiki-modal/wiki-locations-panel.presenter";
import { attempt } from "./attempt";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { fieldMapImageKey } from "./field-image-keys";
import { gameAssetUrl } from "./game-assets";
import { gameText, mapName, originalOf, serverCopyOf, shownPlaceOf, type Texts } from "./map-view";
import { questText } from "./quest-words";
import { questTarget } from "./quest-target-words";
import type { ScreenName, ScreenParam } from "./router";
import { gameWord } from "./language";
import { availableQuests, giverName, isGiven, type QuestKind } from "./quest-offers";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";

export type MarkerKind = "npc" | "boss" | "cardShop";

export interface MapMarker {
  key: string;
  kind: MarkerKind;
  label: string;
  imageUrl: string | null;
  /** Position as a percentage of the map image. */
  x: number;
  y: number;
  /** Where a quest under way asks to go. */
  isObjective: boolean;
  /** Drawn as the signpost, never as a character: the player's own place shows the ones the game has on the field. */
  isWaypoint: boolean;
  /** A quest that can be started there, and who starts it (the talk line that names them), when there is someone. */
  offer: { kind: QuestKind; giver: string | null } | null;
  /** Where tapping it leads (its wiki entry). */
  target: { screen: ScreenName; param: ScreenParam };
}

/** Everything the current map screen draws. */
export interface MapSheet {
  mapId: string;
  /** The map as the game has it, which for a place of the second server is its copy (`mapId` is its original). */
  placeId: string;
  /** Null when the map is not in the catalog. */
  name: string | null;
  /** Null when the map has no name or no picture; the screen then shows the question mark. */
  imageUrl: string | null;
  markers: MapMarker[];
  /** Where the quests under way ask to go, wherever that is. */
  questPlaces: readonly string[];
  /** Where a quest can be started. */
  offerPlaces: readonly string[];
}

/** What the Mision screen opens when it is asked for no quest. */
export const MainQuestParam = "mainQuest";

const wikiOfMarker: Record<MarkerKind, ScreenName> = {
  npc: "wikiNpc",
  boss: "wikiEnemigo",
  cardShop: "wikiTienda",
};

/** The quests under way: the main one and those given, not done. */
function questsUnderWay(state: State): QuestViewModel[] {
  const journal = attempt(
    () => JournalPresenter.getJournalViewModel(state.journal, state.party),
    null,
  );
  if (journal === null) {
    return [];
  }

  const others = [
    ...journal.sideQuests,
    ...journal.legendaryWeapons,
    ...journal.driAgents,
    ...journal.duelIsland,
  ].filter(isGiven);
  return [...(journal.mainQuest === null ? [] : [journal.mainQuest]), ...others].filter(
    (quest) => !quest.isDone,
  );
}

/** What the quests under way ask for in a place, as markers: where the journal says to go, whoever stands there. */
function objectivesIn(state: State, texts: Texts, places: readonly string[]): MapMarker[] {
  return questsUnderWay(state).flatMap((quest): MapMarker[] => {
    const step = quest.currentStep;
    if (step?.coordinates == null || !places.includes(step.location)) {
      return [];
    }

    const label = questTarget(texts, quest.id, step.number) ?? step.number;
    return [
      {
        key: `objective-${quest.id}`,
        kind: "npc",
        label,
        imageUrl: null,
        x: step.coordinates.x,
        y: step.coordinates.y,
        isObjective: true,
        isWaypoint: true,
        offer: null,
        target: { screen: "mision", param: quest.id },
      },
    ];
  });
}

/** The quests that can be started now. */
function offers(state: State, live: readonly string[] | null) {
  return attempt(() => {
    const journal = JournalPresenter.getJournalViewModel(state.journal, state.party);
    return availableQuests(journal, state.party, live);
  }, []);
}

/** The quests that can be started in a place, as markers on where their first step says. */
function offersIn(
  state: State,
  texts: Texts,
  places: readonly string[],
  live: readonly string[] | null,
): MapMarker[] {
  return offers(state, live).flatMap(({ quest, kind, giver, places: where }): MapMarker[] => {
    const first = quest.steps.find((step) => step.number === "1");
    if (first?.coordinates == null || !where.some((place) => places.includes(place))) {
      return [];
    }

    return [
      {
        key: `offer-${quest.id}`,
        kind: "npc",
        label:
          (giver === null ? null : giverName(giver)) ??
          questText(texts, `${quest.id}.title`) ??
          quest.id,
        imageUrl: null,
        x: first.coordinates.x,
        y: first.coordinates.y,
        isObjective: true,
        isWaypoint: true,
        offer: { kind, giver },
        target: { screen: "mision", param: quest.id },
      },
    ];
  });
}

/**
 * What the Mapa screen draws. By default it is the place the player is in; the Wiki asks for another
 * one by id (`placeId`).
 *
 * In the player's own place the characters are not markers: the game's live actors are drawn instead. Only a marker that
 * is not a character stays (a boss, a card shop), and what the quests under way ask for there, as a waypoint.
 * The Wiki's view of another place has no live data, so it keeps all the static markers.
 */
export function buildMapSheet(
  state: State,
  texts: Texts,
  placeId?: string,
  liveOffers: readonly string[] | null = null,
): MapSheet {
  // A copy for the second server has its own picture and name; the rules and the markers are its original's.
  const shown = placeId ?? shownPlaceOf(state.player.mapId);
  const mapId = originalOf(shown);
  const copy = serverCopyOf(shown);
  const name = mapName(texts, shown);
  const fieldImageKey = fieldMapImageKey(Number(`0x${shown}`), state.player.extraChapter ?? false);
  const imageUrl =
    fieldImageKey !== null
      ? gameAssetUrl("", fieldImageKey)
      : ((copy !== null ? ImageCatalog.getLocationImageUrl(shown) : null) ??
        attempt(() => MapPresenter.getByLocationId(mapId).locationImageUrl, null));

  const isOwnPlace = placeId === undefined;
  const markers = attempt(
    () => WikiLocationsPanelPresenter.getMapMarkers(mapId, state.journal.mainQuest),
    [],
  ).flatMap((marker): MapMarker[] => {
    const label =
      (marker.nameKey !== undefined ? gameText(texts, marker.nameKey) : null) ??
      (marker.name === undefined ? undefined : gameWord(marker.name)) ??
      marker.id;

    const isCharacter = marker.kind === "npc";
    if (isOwnPlace && isCharacter) {
      return [];
    }

    return [
      {
        key: `${marker.kind}-${marker.id}`,
        kind: marker.kind,
        label,
        imageUrl: marker.imageUrl,
        x: marker.coordinates.x,
        y: marker.coordinates.y,
        isObjective: false,
        isWaypoint: false,
        offer: null,
        target: { screen: wikiOfMarker[marker.kind], param: marker.id },
      },
    ];
  });

  return {
    mapId,
    placeId: shown,
    name,
    imageUrl,
    markers: isOwnPlace
      ? [
          ...markers,
          ...objectivesIn(state, texts, [mapId, shown]),
          ...offersIn(state, texts, [mapId, shown], liveOffers),
        ]
      : markers,
    questPlaces: questsUnderWay(state).flatMap((quest) => quest.currentStep?.location ?? []),
    offerPlaces: offers(state, liveOffers).flatMap((offer) => offer.places),
  };
}
