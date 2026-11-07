import type { State } from "@/models";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { SearchItemSortHelper } from "@/presenters/helper/search-item-sort.helper";
import { AsukaServerMapPresenter } from "@/presenters/map/asuka-server-map.presenter";
import { MapEnemiesPresenter } from "@/presenters/map/map-enemies.presenter";
import { MapPresenter } from "@/presenters/map/map.presenter";
import { MobiusDesertMapPresenter } from "@/presenters/map/mobius-desert-map.presenter";
import { SeabedMapPresenter } from "@/presenters/map/seabed-map.presenter";
import { WikiModalPresenter } from "@/presenters/map/wiki-modal.presenter";
import { NpcBattleOpponentHelper } from "@/presenters/helper/npc-battle-opponent.helper";
import { CardRepository } from "@/repositories/card.repository";
import { CardShopRepository } from "@/repositories/card-shop.repository";
import { ConsumableItemRepository } from "@/repositories/consumable-item.repository";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { EquipmentRepository } from "@/repositories/equipment.repository";
import { MapService } from "@/services/map.service";
import type {
  SearchItemKind,
  SearchItemViewModel,
} from "@/viewmodels/search/search-item.viewmodel";
import { genericNpcUrl } from "./assets";
import { attempt } from "./attempt";
import { BoosterIcon } from "./item-icons";
import { gameText, locationName, mapName, originalOf, shownPlaceOf, type Texts } from "./map-view";
import { placeWord } from "./place-words";
import {
  enemyTarget,
  itemIcon,
  npcTarget,
  type WikiCategory,
  type WikiPicture,
  type WikiTarget,
} from "./wiki-common";

/** A row of a list or a hit of the search. */
export interface WikiRow {
  key: string;
  name: string;
  /** Second line: level, colour and points, resale value... */
  sub: string;
  /** Kit icon shown when the row has no picture of its own. */
  icon: string;
  picture: WikiPicture | null;
  /** Size of the picture slot the design gives this row. */
  box: { w: number; h: number };
  target: WikiTarget;
}

export interface WikiCategoryCard {
  category: WikiCategory;
  count: number;
}

export interface WikiHereSheet {
  /** Name of the map the player is in, null when the game does not say. */
  place: string | null;
  enemies: { id: string; name: string; isBoss: boolean; picture: WikiPicture }[];
}

export interface WikiIndexSheet {
  categories: WikiCategoryCard[];
  here: WikiHereSheet;
}

/** Everything the original wiki can look up, built once: the names do not change while the app runs. */
let allItems: SearchItemViewModel[] | null = null;

function searchItems(texts: Texts): SearchItemViewModel[] {
  allItems ??= WikiModalPresenter.getAllSearchItems(
    (labelKey) => texts.t(labelKey),
    (cardId) => texts.t(`card.${cardId}.name`),
    (locationId) => locationName(texts, locationId) ?? locationId,
    (cardShopId) => texts.t(`cardShop.${cardShopId}.name`),
    // The tamers and NPCs are named from the disc (composed-messages.ts); without their line, the app's word for one.
    (tamerId) => gameText(texts, `tamers.${tamerId}.name`) ?? texts.t("handheld.word.person"),
    (duelIslandId) => texts.t(`duelIsland.${duelIslandId}.name`),
    (npcId) => gameText(texts, `npcs.${npcId}.name`) ?? texts.t("handheld.word.person"),
  );

  return allItems;
}

const kindsOfCategory: Record<WikiCategory, readonly SearchItemKind[]> = {
  enemies: ["enemy"],
  cards: ["card"],
  equipment: ["equipment"],
  items: ["consumableItem"],
  places: ["location"],
  npcs: ["tamer", "leader", "npc", "cardShop"],
};

const sortedByCategory = new Map<WikiCategory, SearchItemViewModel[]>();

/** The items of a category, in alphabetical order (the same order the search uses). */
function itemsOf(texts: Texts, category: WikiCategory): SearchItemViewModel[] {
  const known = sortedByCategory.get(category);
  if (known !== undefined) {
    return known;
  }

  const kinds = kindsOfCategory[category];
  const items = SearchItemSortHelper.sort(
    searchItems(texts).filter((item) => item.kind !== undefined && kinds.includes(item.kind)),
  );
  sortedByCategory.set(category, items);
  return items;
}

function levelText(texts: Texts, level: number): string {
  return texts.t("handheld.wiki.level", { level });
}

function pointsText(texts: Texts, id: string): string {
  const raw = CardRepository.getCardById(id);
  if (raw === undefined) {
    return texts.t("handheld.wiki.noData");
  }

  const color = texts.t(`cardType.${raw.type}`);
  return raw.points === undefined ? color : `${color} · ${raw.points.ap}/${raw.points.hp}`;
}

/** What each type of equipment raises. */
const statOfEquipment: Record<string, string> = {
  shield: "defense",
  headArmor: "defense",
  bodyArmor: "defense",
  oneHandedWeapon: "strength",
  twoHandedWeapon: "strength",
  accessory: "charisma",
};

/** The icon of a piece of equipment and what it raises, in words: Fuerza, Defensa or Carisma. */
function equipmentSub(texts: Texts, id: string): { icon: string; sub: string } {
  const stat = statOfEquipment[EquipmentRepository.getById(id)?.type ?? ""];
  return {
    icon: itemIcon(id),
    sub: stat === undefined ? texts.t("handheld.wiki.noData") : texts.t(`stat.${stat}`),
  };
}

/** The kit icon and where tapping leads for an item of the search; one place for the lists and the search. */
function lookOf(texts: Texts, item: SearchItemViewModel): { icon: string; target: WikiTarget } {
  switch (item.kind) {
    case "enemy":
      return { icon: "boss", target: enemyTarget(item.id) };
    case "card":
      return { icon: "card", target: { screen: "wikiCarta", param: item.id } };
    case "equipment":
      return {
        icon: equipmentSub(texts, item.id).icon,
        target: { screen: "wikiEquipo", param: Number(item.id) },
      };
    case "consumableItem":
      return {
        icon: itemIcon(item.id),
        target: { screen: "wikiObjeto", param: Number(item.id) },
      };
    case "booster":
      return { icon: BoosterIcon, target: { screen: "wikiSobre", param: Number(item.id) } };
    case "location":
      return { icon: "place", target: { screen: "mapa", param: item.id } };
    case "cardShop":
      return { icon: "shop", target: { screen: "wikiTienda", param: item.id } };
    default:
      return { icon: "unknown", target: npcTarget(item.id) };
  }
}

/** Second line and look of the row of an enemy: level, boss or the owner. */
function enemyRow(texts: Texts, item: SearchItemViewModel): WikiRow {
  const { icon, target } = lookOf(texts, item);
  const raw = EnemyRepository.getEnemyById(item.id);
  const level = levelText(texts, raw.level);

  return {
    key: `enemy-${item.id}`,
    name: item.name,
    sub: raw.boss === true ? `${texts.t("handheld.wiki.boss")} · ${level}` : level,
    icon,
    picture: { url: ImageCatalog.getEnemyImageUrl(item.id) },
    box: { w: 44, h: 44 },
    target,
  };
}

function npcPicture(id: string): WikiPicture {
  return { url: NpcBattleOpponentHelper.getImageUrl(id) };
}

function npcRow(texts: Texts, item: SearchItemViewModel): WikiRow {
  const { icon, target } = lookOf(texts, item);
  const isShop = item.kind === "cardShop";
  const shopImage = isShop
    ? ImageCatalog.getFieldActorImageUrl(CardShopRepository.getById(item.id)?.keeper?.fieldActor)
    : null;

  return {
    key: `${item.kind}-${item.id}`,
    name: item.name,
    sub: texts.t(`handheld.wiki.kind.${item.kind}`),
    icon,
    picture: isShop ? { url: shopImage } : npcPicture(item.id),
    box: isShop ? { w: 31, h: 40 } : { w: 20, h: 34 },
    target,
  };
}

/** One row of a category list, from an item of the search. */
function rowOf(texts: Texts, category: WikiCategory, item: SearchItemViewModel): WikiRow {
  const { icon, target } = lookOf(texts, item);
  switch (category) {
    case "enemies":
      return enemyRow(texts, item);
    case "cards": {
      const raw = CardRepository.getCardById(item.id);
      return {
        key: `card-${item.id}`,
        name: item.name,
        sub: raw?.type === "action" ? texts.t("cardType.action") : pointsText(texts, item.id),
        icon,
        picture: { card: raw === undefined ? null : Number(item.id) },
        box: { w: 40, h: 46 },
        target,
      };
    }
    case "equipment": {
      return {
        key: `equipment-${item.id}`,
        name: item.name,
        sub: equipmentSub(texts, item.id).sub,
        icon,
        picture: null,
        box: { w: 22, h: 22 },
        target,
      };
    }
    case "items": {
      const resale = ConsumableItemRepository.getById(Number(item.id))?.resaleValue;
      return {
        key: `item-${item.id}`,
        name: item.name,
        sub:
          resale === undefined
            ? texts.t("handheld.wiki.noData")
            : texts.t("handheld.wiki.resale", { value: resale }),
        icon,
        picture: null,
        box: { w: 22, h: 22 },
        target,
      };
    }
    case "places":
      return {
        key: `place-${item.id}`,
        name: item.name,
        sub: texts.t("handheld.wiki.kind.location"),
        icon,
        picture: null,
        box: { w: 22, h: 22 },
        target,
      };
    case "npcs":
      return npcRow(texts, item);
  }
}

/** Rows of a category, in alphabetical order. */
export function buildWikiList(category: WikiCategory, texts: Texts): WikiRow[] {
  const rows = itemsOf(texts, category).map((item) => rowOf(texts, category, item));
  return category === "places" ? [...guideRows(texts), ...rows] : rows;
}

/** The guides of two places that have more to them than a picture: first of the places, whatever their names. */
function guideRows(texts: Texts): WikiRow[] {
  const sub = texts.t("handheld.wiki.kind.guide");
  const box = { w: 22, h: 22 };
  return [
    {
      key: "guide-seabed",
      name: texts.t("handheld.wiki.guide.seabed", { place: placeWord(texts, "seabed") }),
      sub,
      icon: "seabed",
      picture: null,
      box,
      target: { screen: "seabed", param: null },
    },
    {
      key: "guide-mobius",
      name: placeWord(texts, "mobiusDesert"),
      sub,
      icon: "desert",
      picture: null,
      box,
      target: { screen: "wikiMobius", param: null },
    },
  ];
}

/** How many entries each category has, from the same data the lists draw. */
export function countOf(category: WikiCategory, texts: Texts): number {
  return itemsOf(texts, category).length;
}

/**
 * The place the player is in as the game numbers it: a copy for the second server stays a copy, which is the one to
 * name. During a battle it is the place the battle started in.
 */
function currentShownMapId(state: State): string {
  const { mapId, previousMapId } = state.player;
  return MapPresenter.isInBattle(mapId) || MapPresenter.isInCardBattle(mapId)
    ? previousMapId
    : shownPlaceOf(mapId);
}

/** The place the player is in with the rules the data has for it: a copy for the second server is its original. */
function currentMapId(state: State): string {
  return originalOf(currentShownMapId(state));
}

/** Enemy ids of the place the player is in, with the rules the original map uses for it. */
function hereEnemies(state: State) {
  const { seabedRoute } = state.player;
  const mapId = currentMapId(state);
  const region = attempt(() => MapService.getRegionByMapId(mapId), "asukaServer");

  if (region === "seabed") {
    return MapEnemiesPresenter.getResumedEnemiesByIds(SeabedMapPresenter.getEnemyIds(seabedRoute));
  }

  if (region === "mobiusDesert") {
    return MapEnemiesPresenter.getResumedEnemiesByIds(
      // The Amaterasu server's desert has enemies of its own, by its own map.
      MobiusDesertMapPresenter.getEnemyIds(currentShownMapId(state), state.journal.mainQuest),
    );
  }

  const map = AsukaServerMapPresenter.getViewModel(
    mapId,
    state.journal.mainQuest,
    state.journal.sideQuests,
    state.party.slots,
    state.player.previousMapId,
    state.npcs,
    state.importantItems,
  );

  return MapEnemiesPresenter.getResumedEnemiesByEncounterSources(
    map.enemies,
    map.boss,
    map.fishing,
    map.kickingTree,
  );
}

export function buildWikiIndex(state: State, texts: Texts): WikiIndexSheet {
  const categories = (Object.keys(kindsOfCategory) as WikiCategory[]).map(
    (category): WikiCategoryCard => ({ category, count: countOf(category, texts) }),
  );

  const enemies = attempt(() => hereEnemies(state), []).map((enemy) => ({
    id: enemy.id,
    name: enemy.name,
    isBoss: enemy.boss,
    picture: { url: ImageCatalog.getEnemyImageUrl(enemy.id) },
  }));

  return {
    categories,
    here: { place: mapName(texts, currentShownMapId(state)), enemies },
  };
}

/** The sprite the index draws for the NPC category: the generic person of the game, never the kit's person icon. */
export function npcCategoryArt(): string {
  return genericNpcUrl();
}

/** A hit of the search. */
export interface WikiHit {
  key: string;
  name: string;
  /** What it is: Carta, Objeto, Lugar... with the level for an enemy. */
  kind: string;
  icon: string;
  picture: WikiPicture | null;
  target: WikiTarget;
}

/** Second line of a hit that is an enemy: wild, boss or the owner, with its level (the original search labels). */
function enemyKind(texts: Texts, item: SearchItemViewModel): string {
  const params = item.kindLabelParams ?? {};
  switch (item.kindLabelKey) {
    case "enemy.searchContext.boss":
      return texts.t("handheld.wiki.kindEnemy.boss", params);
    case "enemy.searchContext.tamer":
    case "enemy.searchContext.npc":
      return texts.t("handheld.wiki.kindEnemy.owned", params);
    default:
      return texts.t("handheld.wiki.kindEnemy.wild", params);
  }
}

const searchKindLabels: Record<SearchItemKind, string> = {
  enemy: "enemy",
  equipment: "equipment",
  consumableItem: "item",
  booster: "booster",
  card: "card",
  location: "location",
  cardShop: "cardShop",
  tamer: "tamer",
  leader: "leader",
  npc: "npc",
};

/** What the search finds for `query`: every name that contains it, in the order the original wiki sorts them. */
export function searchWiki(query: string, texts: Texts): WikiHit[] {
  const needle = query.toLowerCase();
  if (needle === "") {
    return [];
  }

  const found = SearchItemSortHelper.sort(
    searchItems(texts).filter((item) => item.name.toLowerCase().includes(needle)),
  );

  return found.flatMap((item): WikiHit[] => {
    const kind = item.kind;
    if (kind === undefined) {
      return [];
    }

    const { icon, target } = lookOf(texts, item);
    const isPerson = kind === "tamer" || kind === "leader" || kind === "npc";

    return [
      {
        key: `${kind}-${item.id}`,
        name: item.name,
        kind:
          kind === "enemy"
            ? enemyKind(texts, item)
            : texts.t(`handheld.wiki.kind.${searchKindLabels[kind]}`),
        icon,
        picture: isPerson ? { url: genericNpcUrl() } : null,
        target,
      },
    ];
  });
}
