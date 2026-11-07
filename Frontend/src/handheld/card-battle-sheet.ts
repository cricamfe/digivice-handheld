import { ImageCatalog } from "@/catalogs/image.catalog";
import { MapId } from "@/constants/map-id.constant";
import type { State } from "@/models";
import { NpcBattleOpponentHelper } from "@/presenters/helper/npc-battle-opponent.helper";
import { CardRepository } from "@/repositories/card.repository";
import { FolderRepository } from "@/repositories/folder.repository";
import type { TamerCardBattleRaw } from "@/repositories/tables/raws/tamer/tamer-card-battle.raw";
import { gameText, locationName, type Texts } from "./map-view";

export interface DeckCard {
  /** Position in the folder, so a card with several copies keeps one entry. */
  key: string;
  cardId: string;
  name: string;
  /** Numeric disc ID of the art, null when the card is not in the database. */
  imageId: number | null;
  quantity: number;
}

export interface CardBattleSheet {
  npcId: string | null;
  /** Name of the rival, null when the game did not say who it is. */
  rival: string | null;
  tamerImageUrl: string | null;
  backdropUrl: string | null;
  folderName: string | null;
  folderLevel: number | null;
  place: string | null;
  /** Charisma the rival asks for, like `60-209` or `378+`. */
  charisma: string | null;
  deck: DeckCard[];
  booster: { id: number; name: string } | null;
}

function findCardBattle(
  npcId: string | null,
  cardBattleId: number | null,
): TamerCardBattleRaw | null {
  if (npcId === null || cardBattleId === null) {
    return null;
  }

  const opponent = NpcBattleOpponentHelper.resolveById(npcId);
  if (opponent === undefined || opponent.source === "npc") {
    return null;
  }

  const battles = Object.values(opponent.raw.cardBattles ?? {});
  return battles.find((battle) => battle.id === cardBattleId) ?? null;
}

function charismaText(battle: TamerCardBattleRaw): string {
  const { min, max } = battle.charismaRequired;
  return max === undefined ? `${min}+` : `${min}-${max}`;
}

export function buildCardBattleSheet(state: State, texts: Texts): CardBattleSheet {
  const cardBattleId = state.cardBattle.id;
  const npcId = NpcBattleOpponentHelper.getNpcIdByCardBattleId(cardBattleId);
  const battle = findCardBattle(npcId, cardBattleId);
  const folder = battle === null ? undefined : FolderRepository.getFolderById(battle.folderId);
  const nameKey = npcId === null ? null : NpcBattleOpponentHelper.getNameKey(npcId);
  const opponent = npcId === null ? undefined : NpcBattleOpponentHelper.resolveById(npcId);

  const deck = (folder?.cards ?? []).map((folderCard, index): DeckCard => {
    const cardRaw = CardRepository.getCardById(folderCard.id);

    return {
      key: `${index}-${folderCard.id}`,
      cardId: folderCard.id,
      name: gameText(texts, `card.${folderCard.id}.name`) ?? folderCard.id,
      imageId: cardRaw === undefined ? null : Number(folderCard.id),
      quantity: folderCard.quantity,
    };
  });

  const boosterName = battle === null ? null : gameText(texts, `boosters.${battle.boosterId}.name`);

  return {
    npcId,
    rival: nameKey === null ? null : gameText(texts, nameKey),
    tamerImageUrl: npcId === null ? null : NpcBattleOpponentHelper.getImageUrl(npcId),
    backdropUrl: ImageCatalog.getLocationImageUrl(MapId.cardBattle),
    folderName: battle === null ? null : gameText(texts, `folder.${battle.folderId}`),
    folderLevel: folder?.level ?? null,
    place:
      opponent === undefined || opponent.source === "npc"
        ? null
        : locationName(texts, opponent.raw.locationId),
    charisma: battle === null ? null : charismaText(battle),
    deck,
    booster:
      battle === null
        ? null
        : { id: battle.boosterId, name: boosterName ?? String(battle.boosterId) },
  };
}
