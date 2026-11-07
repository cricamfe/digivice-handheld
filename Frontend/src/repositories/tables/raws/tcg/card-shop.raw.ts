import type { MainQuestAvailabilityWindowRaw } from "@/repositories/tables/raws/quest/main-quest-availability-window.raw";

export interface CardShopInventoryItemRaw {
  cardId: string;
  price: number;
}

export interface CardShopPhaseRaw {
  mainQuestAvailabilityWindow: MainQuestAvailabilityWindowRaw;
  inventory: CardShopInventoryItemRaw[];
}

export interface CardShopCatalogRaw {
  locationId: string;
  /** Who keeps the shop: a Digimon, by its line of the game's names, drawn as the field character it stands as. */
  keeper?: { nameId: number; fieldActor: string };
  phases: CardShopPhaseRaw[];
}

export interface CardShopCardRaw {
  cardShopId: string;
  mainQuestAvailabilityWindow: MainQuestAvailabilityWindowRaw;
  price: number;
}
