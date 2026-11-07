import { ref } from "vue";
import { BoosterIcon, ItemIcons } from "./item-icons";
import { gameText, type Texts } from "./map-view";
import type { ScreenName, ScreenParam } from "./router";
import type { DropType } from "@/repositories/tables/raws/drop/drop-type";

/** The six groups of the Wiki index, in the order the design draws them. */
export type WikiCategory = "enemies" | "cards" | "equipment" | "items" | "places" | "npcs";

export const wikiCategories: readonly WikiCategory[] = [
  "enemies",
  "cards",
  "equipment",
  "items",
  "places",
  "npcs",
];

/** Kit icon of each category (the NPC one is a sprite the screens draw themselves, so it has none). */
export const categoryIcons: Record<WikiCategory, string> = {
  enemies: "boss",
  cards: "card",
  equipment: "defense",
  items: "chest",
  places: "gem",
  npcs: "",
};

/** Where tapping something of the Wiki leads. */
export interface WikiTarget {
  screen: ScreenName;
  param: ScreenParam;
}

/**
 * What a picture slot draws: the URL of an image (null when the game files have none), the numeric
 * ID of a card (cards load on demand), or a kit icon.
 */
export interface WikiPicture {
  url?: string | null;
  card?: number | null;
  icon?: string;
}

/** Where a sheet of an enemy leads. */
export function enemyTarget(enemyId: string): WikiTarget {
  return { screen: "wikiEnemigo", param: enemyId };
}

/** Where a sheet of an NPC (tamer, leader, duel island, story NPC) leads, with a battle option when known. */
export function npcTarget(npcId: string, option: string | null = null): WikiTarget {
  return { screen: "wikiNpc", param: option === null ? npcId : { npc: npcId, option } };
}

/** Where a drop leads, by its kind. */
export function dropTarget(type: DropType, dropId: number | string): WikiTarget {
  if (type === "equipment") {
    return { screen: "wikiEquipo", param: Number(dropId) };
  }

  return { screen: type === "booster" ? "wikiSobre" : "wikiObjeto", param: Number(dropId) };
}

/** Icon of a drop, the one the game's menus draw for it. */
export function dropIcon(type: DropType, dropId: number | string): string {
  return type === "booster" ? BoosterIcon : itemIcon(dropId);
}

/** Icon of a piece of equipment or an object, the one the game's menus draw for it. */
export function itemIcon(id: number | string): string {
  return ItemIcons[String(id)] ?? "unknown";
}

/** Name of a drop in the game's own language. */
export function dropName(texts: Texts, key: string, id: number | string): string {
  return gameText(texts, key) ?? `#${id}`;
}

/** The text of a tab or list that has to survive going to a sheet and back. */
export const wikiCategory = ref<WikiCategory>("enemies");
/** What the search box holds. */
export const wikiQuery = ref("");

/** Starts a new search: the box is empty when the player opens it from the index or a list. */
export function openSearch(): void {
  wikiQuery.value = "";
  wikiSearchScroll.top = 0;
}

/** Scroll of the search results, so Back from a sheet returns to the hit the player was on. */
export const wikiSearchScroll = { top: 0 };
/** Scroll of each list, so Back returns to the row the player was on. */
export const wikiScroll: Partial<Record<WikiCategory, number>> = {};

/** A param that names a category, from the index or the list tabs; anything else keeps the last one. */
export function categoryOf(param: unknown): WikiCategory | null {
  return typeof param === "string" && (wikiCategories as readonly string[]).includes(param)
    ? (param as WikiCategory)
    : null;
}

/** A param that carries a numeric id (equipment, object, booster), from a number or a numeric string. */
export function numberOf(param: unknown): number | null {
  const value = typeof param === "string" && param !== "" ? Number(param) : param;
  return typeof value === "number" && Number.isInteger(value) ? value : null;
}

/**
 * A name like `Airdramon(Gold)` may break before its bracket instead of inside the word or with a lone `)` on the next
 * line: the zero-width space is the only break the name offers, so the game's own wording stays whole.
 */
export function breakable(name: string): string {
  return name.replace("(", "\u200b(");
}
