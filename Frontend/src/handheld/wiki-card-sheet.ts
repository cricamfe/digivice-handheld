import type { State } from "@/models";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { WikiCardPanelPresenter } from "@/presenters/map/wiki-modal/wiki-card-panel.presenter";
import { WikiCardShopPanelPresenter } from "@/presenters/map/wiki-modal/wiki-card-shop-panel.presenter";
import { WikiCardShopsPresenter } from "@/presenters/map/wiki-modal/wiki-card-shops.presenter";
import { CardRepository } from "@/repositories/card.repository";
import { digimonName } from "@/handheld/digimon-names";
import { CardShopRepository } from "@/repositories/card-shop.repository";
import type { CardType } from "@/repositories/tables/raws/tcg/card.raw";
import { QuestService } from "@/services/quest.service";
import { attempt } from "./attempt";
import { gameText, locationName, type Texts } from "./map-view";
import { type WikiTarget } from "./wiki-common";

/** A card as the sheets draw it: name, the ID of its art and what the data says about it. */
export interface WikiCardRef {
  id: string;
  name: string;
  /** Numeric disc ID of the art, null when the card is not in the database. */
  imageId: number | null;
  target: WikiTarget;
}

export function cardRef(id: string, texts: Texts): WikiCardRef {
  return {
    id,
    name: gameText(texts, `card.${id}.name`) ?? `#${id}`,
    imageId: CardRepository.getCardById(id) === undefined ? null : Number(id),
    target: { screen: "wikiCarta", param: id },
  };
}

export interface CardBooster {
  id: number;
  name: string;
  target: WikiTarget;
}

export interface CardShop {
  id: string;
  name: string;
  /** Price in Bits, null when the data does not carry one for the phase that is open. */
  price: number | null;
  target: WikiTarget;
}

export interface WikiCardSheet {
  card: WikiCardRef;
  type: CardType;
  typeLabel: string;
  isAction: boolean;
  /** AP and HP of a Digimon card; null for an action card, which has a note instead. */
  points: { ap: number; hp: number } | null;
  /** What an action card does, in the game's language; null when the data has none. */
  note: string | null;
  boosters: CardBooster[];
  shops: CardShop[];
}

/** The sheet of a card, or null when the id is not in the data. */
export function buildCardSheet(id: string, state: State, texts: Texts): WikiCardSheet | null {
  const view = WikiCardPanelPresenter.getViewModel(id);
  if (view === null) {
    return null;
  }

  const isAction = view.card.type === "action";
  const note = isAction ? gameText(texts, view.card.noteKey) : null;

  const shops = WikiCardShopsPresenter.getCardShopsAccordingMainQuest(
    view.cardShops,
    state.journal.mainQuest,
  ).map((shop): CardShop => {
    // The presenter drops the price, so it is read back from the same entry of the shop table.
    const entry = CardShopRepository.getCardShopsByCardId(id).find(
      (candidate) =>
        candidate.cardShopId === shop.id &&
        candidate.mainQuestAvailabilityWindow === shop.mainQuestAvailabilityWindow,
    );

    return {
      id: shop.id,
      name: gameText(texts, `cardShop.${shop.id}.name`) ?? shop.id,
      price: entry?.price ?? null,
      target: { screen: "wikiTienda", param: shop.id },
    };
  });

  return {
    card: cardRef(id, texts),
    type: view.card.type,
    typeLabel: texts.t(`cardType.${view.card.type}`),
    isAction,
    points: isAction || view.card.points === undefined ? null : view.card.points,
    note: note === null || note === "" ? null : note,
    boosters: view.boosters.map((boosterId) => ({
      id: boosterId,
      name: gameText(texts, `boosters.${boosterId}.name`) ?? `#${boosterId}`,
      target: { screen: "wikiSobre", param: boosterId },
    })),
    shops,
  };
}

export interface WikiShopSheet {
  id: string;
  name: string;
  /** Name of the keeper (the picture of the shop is of them), null when the data has no picture. */
  keeper: string | null;
  imageUrl: string | null;
  /** When the stock on offer is sold: from the start, from a step or until a step. */
  window: { kind: "start" | "from" | "until"; step: number };
  place: { id: string; name: string } | null;
  stock: { card: WikiCardRef; price: number }[];
}

/** The sheet of a card shop, or null when the id is not in the data. */
export function buildShopSheet(id: string, state: State, texts: Texts): WikiShopSheet | null {
  const raw = CardShopRepository.getById(id);
  if (raw === undefined) {
    return null;
  }

  const view = WikiCardShopPanelPresenter.getViewModel(id, state.journal.mainQuest);
  const last = QuestService.getLastCompletedMainQuestStep(state.journal.mainQuest);
  const phase =
    raw.phases.find((candidate) =>
      QuestService.isOnMainQuestRange(last, candidate.mainQuestAvailabilityWindow),
    ) ?? raw.phases[0];
  const window = phase?.mainQuestAvailabilityWindow;

  return {
    id,
    name: gameText(texts, `cardShop.${id}.name`) ?? id,
    keeper: raw.keeper === undefined ? null : digimonName(raw.keeper.nameId),
    imageUrl: ImageCatalog.getFieldActorImageUrl(raw.keeper?.fieldActor),
    window:
      window?.ends !== undefined
        ? { kind: "until", step: window.ends }
        : window === undefined || window.starts === 0
          ? { kind: "start", step: 0 }
          : { kind: "from", step: window.starts },
    place:
      view.locationId === null
        ? null
        : {
            id: view.locationId,
            name:
              attempt(() => locationName(texts, view.locationId ?? ""), null) ?? view.locationId,
          },
    stock: view.cards.map((entry) => ({ card: cardRef(entry.cardId, texts), price: entry.price })),
  };
}
