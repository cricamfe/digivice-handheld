import { useGameStore } from "@/stores/use-game-store";
import type { GameEventSource } from "./event-source";
import { eventsLogger } from "./logger";

/**
 * Wires the events of any source to the Pinia store actions.
 * Source-agnostic on purpose: the bridge and the simulator share it.
 */
export function initializeGameEventHandlers(source: GameEventSource): void {
  const store = useGameStore();

  source.on("HubConnectionStatusChanged", (data) => {
    store.syncHubConnectionStatus(data);
  });

  source.on("HealthChanged", (data) => {
    store.syncHealth(data);
  });

  source.on("InitialState", (data) => {
    store.setInitialState(data);
  });

  source.on("PlayerChanged", (data) => {
    store.syncPlayer(data);
  });

  // Only the Android core sends it: it is not part of the state's diff.
  source.on("PlayerPositionChanged", (data) => {
    store.syncPlayerPosition(data);
  });

  source.on("FieldActorsChanged", (data) => {
    store.syncFieldActors(data);
  });

  source.on("FieldChanging", (data) => {
    store.syncFieldChange(data);
  });

  source.on("PartnerTechniquesChanged", (data) => {
    store.syncPartnerTechniques(data);
  });

  source.on("QuestOffersChanged", (data) => {
    store.syncQuestOffers(data);
  });

  source.on("ImportantItemsChanged", (data) => {
    store.syncImportantItems(data);
  });

  source.on("PartyChanged", (data) => {
    store.syncParty(data);
  });

  source.on("DigimonBattleChanged", (data) => {
    store.syncDigimonBattle(data);
  });

  source.on("CardBattleChanged", (data) => {
    store.syncCardBattle(data);
  });

  source.on("AuctionsChanged", (data) => {
    store.syncAuctions(data);
  });

  source.on("NpcsChanged", (data) => {
    store.syncNpcs(data);
  });

  source.on("JournalChanged", (data) => {
    store.syncJournal(data);
  });

  eventsLogger.debug("Game event handlers successfully initialized.");
}
