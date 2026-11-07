import type { QuestOffersDTO } from "@/events/dto/quest-offers.dto";
import { defineStore } from "pinia";
import { ref } from "vue";
import type { HealthStatus, State } from "@/models";
import type { HealthDTO } from "@/events/dto/health.dto";
import type { StateDTO } from "@/events/dto/state.dto";
import type { PlayerDTO } from "@/events/dto/player.dto";
import type { PlayerPositionDTO } from "@/events/dto/player-position.dto";
import type { FieldActorDTO, FieldActorsDTO } from "@/events/dto/field-actors.dto";
import type { FieldChangingDTO } from "@/events/dto/field-changing.dto";
import type {
  PartnerTechniquesDTO,
  PartnerTechniquesPartnerDTO,
} from "@/events/dto/partner-techniques.dto";
import type { ImportantItemsDTO } from "@/events/dto/important-items.dto";
import type { PartyDTO } from "@/events/dto/party.dto";
import type { DigimonBattleDTO } from "@/events/dto/digimon-battle.dto";
import type { CardBattleDTO } from "@/events/dto/card-battle.dto";
import type { AuctionsDTO } from "@/events/dto/auctions.dto";
import type { NpcsDTO } from "@/events/dto/npcs.dto";
import type { JournalDTO } from "@/events/dto/journal.dto";
import { storeLogger } from "@/events/logger";
import { PlayerConverter } from "@/events/converters/player.converter";
import { ImportantItemsConverter } from "@/events/converters/important-items.converter";
import { PartyConverter } from "@/events/converters/party.converter";
import { DigimonBattleConverter } from "@/events/converters/digimon-battle.converter";
import { CardBattleConverter } from "@/events/converters/card-battle.converter";
import { AuctionsConverter } from "@/events/converters/auctions.converter";
import { NpcsConverter } from "@/events/converters/npcs.converter";
import { JournalConverter } from "@/events/converters/journal.converter";
import { PlayerSyncer } from "./syncers/player.syncer";
import { ImportantItemsSyncer } from "./syncers/important-items.syncer";
import { JournalSyncer } from "./syncers/journal.syncer";
import { PartySyncer } from "./syncers/party.syncer";
import { DigimonBattleSyncer } from "./syncers/digimon-battle.syncer";
import { CardBattleSyncer } from "./syncers/card-battle.syncer";
import { AuctionsSyncer } from "./syncers/auctions.syncer";
import { NpcsSyncer } from "./syncers/npcs.syncer";

export const useGameStore = defineStore("game", () => {
  const healthStatus = ref<HealthStatus>("Loading");
  const lastErrorCode = ref<string | null>(null);
  const lastErrorDetail = ref<string | null>(null);
  const currentState = ref<State | null>(null);
  /** The characters of the map the field draws; not part of the state, they come with their own event. */
  const fieldActors = ref<FieldActorDTO[]>([]);
  /** The change of field going on, or null while the field is running. */
  const fieldChange = ref<FieldChangingDTO | null>(null);
  /** The techniques of the party, as the RAM has them; not part of the state either. */
  const partnerTechniques = ref<PartnerTechniquesPartnerDTO[]>([]);
  /** The quests the game would start now; null until the core says. */
  const questOffers = ref<string[] | null>(null);

  function clearGameState(): void {
    currentState.value = null;
    fieldActors.value = [];
    fieldChange.value = null;
    partnerTechniques.value = [];
    questOffers.value = null;
  }

  function syncHubConnectionStatus(event: {
    isConnected: boolean;
    errorMessage?: string;
    preserveGameState?: boolean;
  }): void {
    if (!event.isConnected && !event.preserveGameState) {
      clearGameState();
    }
  }

  function syncHealth(healthDto: HealthDTO): void {
    healthStatus.value = healthDto.status;

    if (healthDto.status === "Healthy") {
      lastErrorCode.value = null;
      lastErrorDetail.value = null;
      return;
    }

    if (healthDto.status === "Loading") {
      lastErrorCode.value = null;
      lastErrorDetail.value = null;
      return;
    }

    clearGameState();
    lastErrorCode.value = healthDto.errorCode;
    lastErrorDetail.value = healthDto.errorDetail;
  }

  function setInitialState(state: StateDTO): void {
    currentState.value = {
      player: PlayerConverter.convert(state.player),
      importantItems: ImportantItemsConverter.convert(state.importantItems),
      party: PartyConverter.convert(state.party),
      digimonBattle: DigimonBattleConverter.convert(state.digimonBattle),
      cardBattle: CardBattleConverter.convert(state.cardBattle),
      auctions: AuctionsConverter.convert(state.auctions),
      npcs: NpcsConverter.convert(state.npcs),
      journal: JournalConverter.convert(state.journal),
    };
  }

  function getStateOrWarn(eventName: string): State | null {
    const state = currentState.value;
    if (!state) {
      storeLogger.warn(`${eventName} ignored: InitialState not received yet.`);
      return null;
    }

    return state;
  }

  function syncPlayer(newPlayerDto: PlayerDTO): void {
    const state = getStateOrWarn("PlayerChanged");
    if (!state) {
      return;
    }

    PlayerSyncer.sync(state.player, newPlayerDto);
  }

  function syncPlayerPosition(position: PlayerPositionDTO): void {
    const state = getStateOrWarn("PlayerPositionChanged");
    if (!state) {
      return;
    }

    PlayerSyncer.syncPosition(state.player, position);
  }

  function syncQuestOffers(dto: QuestOffersDTO): void {
    questOffers.value = dto.quests;
  }

  function syncPartnerTechniques(dto: PartnerTechniquesDTO): void {
    partnerTechniques.value = dto.partners;
  }

  function syncFieldChange(dto: FieldChangingDTO): void {
    fieldChange.value = dto.changing ? dto : null;
  }

  function syncFieldActors(dto: FieldActorsDTO): void {
    fieldActors.value = dto.actors;
  }

  function syncImportantItems(newImportantItemsDto: ImportantItemsDTO): void {
    const state = getStateOrWarn("ImportantItemsChanged");
    if (!state) {
      return;
    }

    ImportantItemsSyncer.sync(state.importantItems, newImportantItemsDto);
  }

  function syncJournal(newJournalDto: JournalDTO): void {
    const state = getStateOrWarn("JournalChanged");
    if (!state) {
      return;
    }

    JournalSyncer.sync(state.journal, newJournalDto);
  }

  function syncParty(newPartyDto: PartyDTO): void {
    const state = getStateOrWarn("PartyChanged");
    if (!state) {
      return;
    }

    PartySyncer.sync(state.party, newPartyDto);
  }

  function syncDigimonBattle(newDigimonBattleDto: DigimonBattleDTO): void {
    const state = getStateOrWarn("DigimonBattleChanged");
    if (!state) {
      return;
    }

    DigimonBattleSyncer.sync(state.digimonBattle, newDigimonBattleDto);
  }

  function syncCardBattle(newCardBattleDto: CardBattleDTO): void {
    const state = getStateOrWarn("CardBattleChanged");
    if (!state) {
      return;
    }

    CardBattleSyncer.sync(state.cardBattle, newCardBattleDto);
  }

  function syncAuctions(newAuctionsDto: AuctionsDTO): void {
    const state = getStateOrWarn("AuctionsChanged");
    if (!state) {
      return;
    }

    AuctionsSyncer.sync(state.auctions, newAuctionsDto);
  }

  function syncNpcs(newNpcsDto: NpcsDTO): void {
    const state = getStateOrWarn("NpcsChanged");
    if (!state) {
      return;
    }

    NpcsSyncer.sync(state.npcs, newNpcsDto);
  }

  return {
    healthStatus,
    lastErrorCode,
    lastErrorDetail,
    currentState,
    fieldActors,
    fieldChange,
    partnerTechniques,
    questOffers,
    syncHubConnectionStatus,
    syncHealth,
    setInitialState,
    syncPlayer,
    syncPlayerPosition,
    syncFieldActors,
    syncFieldChange,
    syncPartnerTechniques,
    syncQuestOffers,
    syncImportantItems,
    syncParty,
    syncDigimonBattle,
    syncCardBattle,
    syncAuctions,
    syncNpcs,
    syncJournal,
  };
});
