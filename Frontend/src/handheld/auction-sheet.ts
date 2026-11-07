import type { State } from "@/models";
import { AuctionModalPresenter } from "@/presenters/auction/auction-modal.presenter";
import { CurrentAuctionPresenter } from "@/presenters/auction/current-auction.presenter";
import type { AuctionStatus } from "@/types/auction-status.type";
import type { AuctionViewModel } from "@/viewmodels/auction/auction.viewmodel";
import { AuctionWords, fillWords } from "./game-words";
import { gameText, type Texts } from "./map-view";
import { placeWord, type ScreenPlace } from "./place-words";
import { itemIcon } from "./wiki-common";

export interface AuctionRow {
  id: string;
  equipmentId: number;
  name: string;
  bid: number;
  resale: number;
  status: AuctionStatus;
}

export interface CurrentAuction extends AuctionRow {
  /** Kit icon that stands for the kind of equipment on offer. */
  icon: string;
  /** Closing condition, in Spanish. */
  closesWhen: string;
  /** The player has fewer Bits than the bid, so the auction would not start. */
  hasNotEnoughBits: boolean;
}

export interface AuctionSheet {
  bits: number;
  /** Null when no auction is open now. */
  current: CurrentAuction | null;
  rows: AuctionRow[];
}

function toRow(auction: AuctionViewModel, texts: Texts): AuctionRow {
  return {
    id: auction.id,
    equipmentId: auction.equipmentId,
    name: gameText(texts, `equipments.${auction.equipmentId}.name`) ?? `#${auction.equipmentId}`,
    bid: auction.bid,
    resale: auction.resale,
    status: auction.status,
  };
}

/** The place the condition that closes an auction names, for the auctions whose condition names one. */
const AuctionPlaces: Readonly<Record<string, ScreenPlace>> = {
  sniperShield: "undergroundPath",
  dramonShield: "biosSwamp",
  hazardFang: "masterRoom",
};

function closesWhen(texts: Texts, auctionId: string): string {
  const place = AuctionPlaces[auctionId];
  return texts.t(`handheld.auction.closesWhen.${auctionId}`, {
    ...fillWords(texts, AuctionWords[auctionId] ?? {}),
    ...(place === undefined ? {} : { place: placeWord(texts, place) }),
  });
}

/** Everything the Subastas screen draws, from the auction presenters the original modal uses. */
export function buildAuctionSheet(state: State, texts: Texts): AuctionSheet {
  const available = CurrentAuctionPresenter.getAvailableAuction(
    state.auctions,
    state.journal.mainQuest,
  );

  return {
    bits: state.player.bits,
    current:
      available === null
        ? null
        : {
            ...toRow(available, texts),
            icon: itemIcon(available.equipmentId),
            closesWhen: closesWhen(texts, available.id),
            hasNotEnoughBits: CurrentAuctionPresenter.hasNotEnoughBits(
              available,
              state.player.bits,
            ),
          },
    rows: AuctionModalPresenter.getAuctions(state.auctions, state.journal.mainQuest).map(
      (auction) => toRow(auction, texts),
    ),
  };
}
