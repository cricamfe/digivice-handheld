import { ref } from "vue";
import type { State } from "@/models";
import { AuctionCardPresenter } from "@/presenters/auction/auction-card.presenter";
import { JournalPresenter } from "@/presenters/journal/journal.presenter";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";
import { locationName, type Texts } from "./map-view";
import { questText } from "./quest-words";
import {
  availableQuests,
  giverName,
  isGiven,
  kindedQuests,
  type AvailableQuest,
  type KindedQuest,
  type QuestKind,
} from "./quest-offers";

/** Look of a quest card: the frame and the colours follow it. */
export type CardTone = "done" | "fresh";

export interface JournalCard {
  id: string;
  tone: CardTone;
  kind: QuestKind;
  title: string;
  /** Number of the step the card points to, or null when the text is not a step. */
  step: string | null;
  text: string;
}

export type JournalTabKey = "underWay" | "available" | "done";

export interface JournalTab {
  key: JournalTabKey;
  label: string;
  count: number;
  cards: JournalCard[];
}

export interface JournalMain {
  id: string;
  /** The chapter the player is in, or the title of the quest once it is finished. */
  title: string;
  step: string | null;
  text: string;
}

export interface JournalSheet {
  main: JournalMain | null;
  /** Whether an auction is open now. */
  hasAuction: boolean;
  tabs: JournalTab[];
}

/** The section the Diario shows. It lives here so it survives opening a quest and coming back. */
export const selectedTab = ref<JournalTabKey>("underWay");

function describe(quest: QuestViewModel, texts: Texts): { step: string | null; text: string } {
  if (quest.isDone) {
    return { step: null, text: texts.t("journal.questCompleted") };
  }
  if (quest.currentStep === null) {
    return { step: null, text: questText(texts, `${quest.id}.description`) ?? "" };
  }

  const step = quest.currentStep.number;
  return {
    step,
    text: questText(texts, `${quest.id}.steps.${step}.description`) ?? "",
  };
}

function buildCard({ quest, kind }: KindedQuest, texts: Texts): JournalCard {
  return {
    id: quest.id,
    tone: quest.isDone ? "done" : "fresh",
    kind,
    title: questText(texts, `${quest.id}.title`) ?? quest.id,
    ...describe(quest, texts),
  };
}

/** Not yet given: what to do to be given it. */
function buildOffer(offer: AvailableQuest, texts: Texts): JournalCard {
  const place = offer.places[0] === undefined ? null : locationName(texts, offer.places[0]);
  const name = offer.giver === null ? null : giverName(offer.giver);
  return {
    ...buildCard(offer, texts),
    step: null,
    text:
      name === null
        ? texts.t("handheld.journal.goTo", { place: place ?? "" })
        : texts.t(offer.twoTalks ? "handheld.journal.talkTwice" : "handheld.journal.talkTo", {
            name,
            place: place ?? "",
          }),
  };
}

function buildMain(quest: QuestViewModel | null, texts: Texts): JournalMain | null {
  if (quest === null) {
    return null;
  }

  const description = describe(quest, texts);
  const title =
    quest.isDone || quest.currentChapter === null
      ? (questText(texts, `${quest.id}.title`) ?? quest.id)
      : texts.t("journal.chapter", { number: quest.currentChapter.number });

  return { id: quest.id, title, ...description };
}

/** Everything the Diario draws, from the journal and auction presenters the original app uses. */
export function buildJournalSheet(
  state: State,
  texts: Texts,
  liveOffers: readonly string[] | null = null,
): JournalSheet {
  const journal = JournalPresenter.getJournalViewModel(state.journal, state.party);
  const auction = AuctionCardPresenter.getAvailableAuction(state.auctions, state.journal.mainQuest);
  const given = kindedQuests(journal).filter(({ quest }) => isGiven(quest));
  const offers = availableQuests(journal, state.party, liveOffers);
  const card = (quest: KindedQuest): JournalCard => buildCard(quest, texts);

  return {
    main: buildMain(journal.mainQuest, texts),
    hasAuction: auction !== null,
    tabs: [
      tabOf("underWay", given.filter(({ quest }) => !quest.isDone).map(card), texts),
      tabOf(
        "available",
        offers.map((offer) => buildOffer(offer, texts)),
        texts,
      ),
      tabOf("done", given.filter(({ quest }) => quest.isDone).map(card), texts),
    ],
  };
}

function tabOf(key: JournalTabKey, cards: JournalCard[], texts: Texts): JournalTab {
  return { key, label: texts.t(`handheld.journal.${key}`), count: cards.length, cards };
}
