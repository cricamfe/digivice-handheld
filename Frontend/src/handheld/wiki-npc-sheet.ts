import type { State } from "@/models";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { WikiModalPresenter } from "@/presenters/map/wiki-modal.presenter";
import { NpcBattleOpponentHelper } from "@/presenters/helper/npc-battle-opponent.helper";
import { WikiEnemyDropsPresenter } from "@/presenters/map/wiki-modal/wiki-enemy-drops.presenter";
import { WikiNpcCardBattlePresenter } from "@/presenters/map/wiki-modal/wiki-npc-card-battle.presenter";
import { WikiNpcDigimonBattlePresenter } from "@/presenters/map/wiki-modal/wiki-npc-digimon-battle.presenter";
import { WikiNpcPanelPresenter } from "@/presenters/map/wiki-modal/wiki-npc-panel.presenter";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { QuestRepository } from "@/repositories/quest.repository";
import { NpcService } from "@/services/npc.service";
import { QuestService } from "@/services/quest.service";
import type { TamerCharismaRequiredRaw } from "@/repositories/tables/raws/tamer/tamer-charisma-required.raw";
import type { WikiNpcBattleOptionViewModel } from "@/viewmodels/wiki-modal/wiki-npc-battle-option.viewmodel";
import { itemWord, NpcItemWords } from "./game-words";
import { gameText, locationName, type Texts } from "./map-view";
import { cardRef, type WikiCardRef } from "./wiki-card-sheet";
import { dropTarget, enemyTarget, type WikiTarget } from "./wiki-common";

/** What the player can do with one of the battles of an NPC. */
export type NpcOptionState = "open" | "done" | "locked";

export interface NpcOption {
  id: string;
  kind: "card" | "digimon";
  /** Charisma asked for, like `60-209` or `378+`; empty for a story NPC, which asks for none. */
  range: string;
  state: NpcOptionState;
  /** Whether the battle can no longer be fought because a later one took its place. */
  isClosed: boolean;
  /** What is missing (a Carisma range, a trophy) or why it is closed, in words. */
  need: string | null;
}

export interface WikiNpcSheet {
  id: string;
  name: string;
  /** Tamer, Lider, NPC. */
  kindLabel: string;
  imageUrl: string | null;
  place: { id: string; name: string } | null;
  /** The Folder Bag is still missing, so the tamers cannot be challenged. */
  needsFolderBag: boolean;
  options: NpcOption[];
  /** The option to open first: the one asked for, or the one the original wiki picks. */
  selected: string | null;
}

/** The charisma a battle asks for, from the same raw data the presenter reads; null for a story NPC. */
function charismaRequiredOf(
  npcId: string,
  option: WikiNpcBattleOptionViewModel,
): TamerCharismaRequiredRaw | null {
  const opponent = NpcBattleOpponentHelper.resolveById(npcId);
  if (opponent === undefined || opponent.source === "npc") {
    return null;
  }

  const battles = option.kind === "card" ? opponent.raw.cardBattles : opponent.raw.digimonBattles;
  return battles?.[option.battleId]?.charismaRequired ?? null;
}

/** `60-209` or `378+`. */
function rangeOf(required: TamerCharismaRequiredRaw | null): string {
  if (required === null) {
    return "";
  }

  return required.max === undefined ? `${required.min}+` : `${required.min}-${required.max}`;
}

/**
 * What the player lacks, from the view model's structured fields: a trophy the player does not own,
 * and a charisma outside the range (`NpcService`, the service the presenter's verdict comes from).
 */
function needOf(
  option: WikiNpcBattleOptionViewModel,
  required: TamerCharismaRequiredRaw | null,
  partyCharisma: number,
  texts: Texts,
): string | null {
  if (option.requirementsMet && !option.isSuperseded) {
    return null;
  }

  if (option.isSuperseded && option.supersededTooltipKey !== null) {
    return texts.t(option.supersededTooltipKey);
  }

  const isCharismaMissing =
    required !== null && !NpcService.isCharismaInRange(partyCharisma, required);
  const isTrophyMissing = option.trophyRequired !== undefined && !option.trophyOwned;
  const charisma = isCharismaMissing
    ? texts.t("handheld.wiki.npc.charisma", { range: rangeOf(required) })
    : null;
  const trophy =
    isTrophyMissing && option.trophyRequired !== undefined
      ? itemWord(texts, NpcItemWords[option.trophyRequired])
      : null;

  if (charisma !== null && trophy !== null) {
    return texts.t("handheld.wiki.npc.and", { first: charisma, second: trophy });
  }

  return charisma ?? trophy;
}

function optionOf(
  npcId: string,
  option: WikiNpcBattleOptionViewModel,
  partyCharisma: number,
  texts: Texts,
): NpcOption {
  const required = charismaRequiredOf(npcId, option);
  const isClosed = option.isSuperseded && option.status === "missingRequirements";

  return {
    id: option.id,
    kind: option.kind,
    range: rangeOf(required),
    state:
      option.status === "completed" ? "done" : option.status === "available" ? "open" : "locked",
    isClosed,
    need:
      option.status === "completed" || option.status === "available"
        ? null
        : needOf(option, required, partyCharisma, texts),
  };
}

/** The sheet of an NPC, or null when the id is not in the data. */
export function buildNpcSheet(
  npcId: string,
  initialOption: string | null,
  state: State,
  texts: Texts,
): WikiNpcSheet | null {
  const journalNpc = state.npcs[npcId as keyof typeof state.npcs] ?? null;
  const partyCharisma = WikiNpcPanelPresenter.getPartyCharisma(state.party);
  const view = WikiNpcPanelPresenter.getPanelViewModel(
    npcId,
    journalNpc,
    partyCharisma,
    state.importantItems,
  );
  if (view === null) {
    return null;
  }

  const folderBag = state.journal.sideQuests.find((quest) => quest.id === "folderBag");
  const folderBagRaw = QuestRepository.getSideQuestsRaw().find((raw) => raw.id === "folderBag");
  const needsFolderBag =
    view.searchKind === "tamer" &&
    folderBagRaw !== undefined &&
    !QuestService.isQuestCompleted(folderBag, folderBagRaw);

  const selected =
    initialOption !== null && view.battleOptions.some((option) => option.id === initialOption)
      ? initialOption
      : WikiNpcPanelPresenter.getDefaultSelectedBattleOptionId(view.battleOptions);

  return {
    id: npcId,
    name: gameText(texts, view.nameKey) ?? npcId,
    kindLabel: texts.t(`handheld.wiki.kind.${view.searchKind}`),
    imageUrl: view.imageUrl,
    place: { id: view.locationId, name: locationName(texts, view.locationId) ?? view.locationId },
    needsFolderBag,
    // Cards first, then Digimon, each by charisma: the two rows of the design.
    options: view.battleOptions
      .map((option) => optionOf(npcId, option, partyCharisma, texts))
      .sort(
        (first, second) => Number(first.kind === "digimon") - Number(second.kind === "digimon"),
      ),
    selected,
  };
}

export interface NpcCardBattle {
  /** Name of the folder and its level, like `Bug Bug Folder · Nivel 4`. */
  title: string;
  cards: { card: WikiCardRef; quantity: number }[];
  prize: { name: string; target: WikiTarget } | null;
}

export function buildNpcCardBattle(
  npcId: string,
  optionId: string,
  texts: Texts,
): NpcCardBattle | null {
  const battle = WikiNpcCardBattlePresenter.getBattleViewModel(
    npcId,
    optionId.replace(/^card-/, ""),
  );
  if (battle === null) {
    return null;
  }

  const drop = WikiEnemyDropsPresenter.getViewModel(battle.drops)[0];

  return {
    title: `${gameText(texts, battle.nameKey) ?? battle.nameKey} · ${texts.t("enemy.level")} ${battle.level}`,
    cards: battle.cards.map((card) => ({
      card: cardRef(card.cardId, texts),
      quantity: card.quantity,
    })),
    prize:
      drop === undefined
        ? null
        : {
            name: gameText(texts, drop.labelKey) ?? `#${drop.dropId}`,
            target: dropTarget(drop.type, drop.dropId),
          },
  };
}

export interface NpcMember {
  name: string;
  level: number;
  hp: number;
  imageUrl: string | null;
  target: WikiTarget;
}

export interface NpcDigimonBattle {
  exp: number;
  dvexp: number;
  bits: number;
  members: NpcMember[];
}

/** The party of a battle as raw members, from the same source the presenter reads. */
function partyOf(npcId: string, battleId: string) {
  const opponent = NpcBattleOpponentHelper.resolveById(npcId);
  if (opponent === undefined) {
    return [];
  }

  return (
    (opponent.source === "npc"
      ? opponent.raw.party
      : opponent.raw.digimonBattles?.[battleId]?.party) ?? []
  );
}

export function buildNpcDigimonBattle(npcId: string, optionId: string): NpcDigimonBattle | null {
  const battleId = optionId.replace(/^digimon-/, "");
  const battle = WikiNpcDigimonBattlePresenter.getBattleViewModel(npcId, battleId);
  if (battle === null) {
    return null;
  }

  // Each member is resolved once: its enemy id first, then everything drawn about it comes from that id,
  // so the picture, the numbers and the link cannot belong to different enemies.
  const members = partyOf(npcId, battleId).flatMap((member): NpcMember[] => {
    const id = EnemyRepository.getEnemyIdByMemoryIdAndGroupId(member.enemyId, member.groupId);
    if (id === null) {
      return [];
    }

    const enemy = WikiModalPresenter.getEnemyById(id);
    return [
      {
        name: enemy.name,
        level: enemy.level,
        hp: enemy.hp,
        imageUrl: ImageCatalog.getEnemyImageUrl(id),
        target: enemyTarget(id),
      },
    ];
  });

  return { exp: battle.exp, dvexp: battle.dvexp, bits: battle.bits, members };
}
