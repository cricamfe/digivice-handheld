import type { Party } from "@/models";
import { PartyService } from "@/services/party.service";
import type { JournalViewModel } from "@/viewmodels/quest/journal.viewmodel";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";
import { genericNpcUrl, iconUrl } from "./assets";
import { gameWord } from "./language";

/** The kind of a quest, which only says its icon. */
export type QuestKind = "extra" | "weapon" | "dri" | "duel";

export function questKindIcon(kind: QuestKind): string {
  switch (kind) {
    case "extra":
      return iconUrl("chest");
    case "weapon":
      return iconUrl("battle");
    case "dri":
      return genericNpcUrl();
    case "duel":
      return iconUrl("card");
  }
}

interface QuestOffer {
  /** The main step that opens it (done), and the one that closes it for good, or null. */
  from: number;
  until: number | null;
  /**
   * Who starts it by talking: the talk line that names them, as `ActorNames` has it for the characters of the map (no
   * text, the app reads the name from the disc); null when a place starts it by itself.
   */
  giver: string | null;
  /** Where else they stand than where its first step says. */
  alsoIn?: readonly string[];
  /** Quests that must be done first. */
  needs?: readonly string[];
  /** The party's total level the talk asks for. */
  levels?: readonly [number, number];
  /** The first talk only introduces them: the quest starts with the second. */
  twoTalks?: boolean;
}

/**
 * When the game lets each quest start, from its scripts in ReGame-Labs/dw3_decomp. Muramasa, Super Nova and the two Amaterasu duels are inferred from how the floors connect, not from a
 * condition of the scripts. Who only comes back after the end of the story through another door (Folder Bag, Eternally,
 * Muramasa) is taken as closed from the step that closes the first.
 */
const Offers: Readonly<Record<string, QuestOffer>> = {
  folderBag: { from: 0, until: 122, giver: "talk00:47" },
  fishingPole: {
    from: 0,
    until: 122,
    giver: "talk02:657",
    needs: ["folderBag"],
    twoTalks: true,
  },
  treeBoots: { from: 0, until: 125, giver: null, needs: ["folderBag"] },
  digimonOnline: { from: 133, until: null, giver: null },
  eternally: { from: 0, until: 125, giver: "talk06:235" },
  invincible: { from: 2, until: null, giver: "talk06:241" },
  muramasa: { from: 25, until: 125, giver: "talk06:483", alsoIn: ["0244"] },
  punishment: { from: 59, until: null, giver: "talk06:332" },
  superNova: { from: 43, until: 125, giver: "talk05:179" },
  driAgentAgumon: { from: 4, until: 122, giver: "talk06:690", levels: [30, 59], twoTalks: true },
  driAgentGuilmon: { from: 4, until: 122, giver: "talk01:276", levels: [30, 59], twoTalks: true },
  driAgentPatamon: { from: 36, until: 122, giver: "talk02:631", levels: [45, 74], twoTalks: true },
  driAgentRenamon: { from: 36, until: 122, giver: "talk02:645", levels: [45, 74], twoTalks: true },
  driAgentKotemon: { from: 43, until: 122, giver: "talk02:829", levels: [60, 89], twoTalks: true },
  driAgentKumamon: { from: 43, until: 122, giver: "talk02:837", levels: [60, 89], twoTalks: true },
  driAgentMonmon: { from: 43, until: 122, giver: "talk02:845", levels: [60, 89], twoTalks: true },
  driAgentVeemon: { from: 48, until: 122, giver: "talk06:675" },
  asukaTrophy: { from: 43, until: 125, giver: "talk00:47", needs: ["folderBag"] },
  asukaRematch: { from: 43, until: 125, giver: "talk00:47", needs: ["asukaTrophy"] },
  amaterasuTrophy: { from: 94, until: 125, giver: "talk00:81", needs: ["folderBag"] },
  amaterasuRematch: { from: 94, until: 125, giver: "talk00:81", needs: ["amaterasuTrophy"] },
  cardKing: { from: 133, until: null, giver: "talk02:1", needs: ["folderBag"] },
};

/** Who gives each quest that someone gives, by quest id: the talk line that names them. */
export const QuestGivers: Readonly<Record<string, string | null>> = Object.fromEntries(
  Object.entries(Offers).map(([id, offer]) => [id, offer.giver]),
);

/**
 * As in a game, a quest appears when it is given: the first of its steps the player has done. A partner owned from the
 * start has its last step done and no quest was ever given.
 */
export function isGiven(quest: QuestViewModel): boolean {
  return !quest.isUnavailable && quest.steps.some((step) => step.isDone);
}

export interface KindedQuest {
  quest: QuestViewModel;
  kind: QuestKind;
}

/** All the quests but the main one, with their kind. */
export function kindedQuests(journal: JournalViewModel): KindedQuest[] {
  const of = (quests: QuestViewModel[], kind: QuestKind): KindedQuest[] =>
    quests.map((quest) => ({ quest, kind }));
  return [
    ...of(journal.sideQuests, "extra"),
    ...of(journal.legendaryWeapons, "weapon"),
    ...of(journal.driAgents, "dri"),
    ...of(journal.duelIsland, "duel"),
  ];
}

export interface AvailableQuest extends KindedQuest {
  /** The talk line that names who gives it (the `speaker` of the live actor), or null. */
  giver: string | null;
  twoTalks: boolean;
  /** Where it can be started. */
  places: readonly string[];
}

/** The name of who gives a quest, from the disc's text in the language shown; null when the text doesn't have it. */
export function giverName(giver: string): string | null {
  const name = gameWord(giver, "");
  return name === "" ? null : name;
}

/** The highest step of the main quest the player has done. */
function reachedStep(journal: JournalViewModel): number {
  return (journal.mainQuest?.steps ?? [])
    .filter((step) => step.isDone)
    .reduce((highest, step) => Math.max(highest, Number(step.number)), 0);
}

/**
 * What the player can start now: nothing they have to find out first, only someone to talk to or a place to go.
 *
 * `live` is what the game's own conditions say from the RAM, or null when nobody has said (the simulator). It knows who
 * stands there and what their talks ask. `Offers` adds what no script tests, because the walls do it: when the player can
 * get there.
 */
export function availableQuests(
  journal: JournalViewModel,
  party: Party,
  live: readonly string[] | null = null,
): AvailableQuest[] {
  const reached = reachedStep(journal);
  const level = PartyService.getLevel(party);
  const quests = kindedQuests(journal);
  const done = new Set(quests.filter(({ quest }) => quest.isDone).map(({ quest }) => quest.id));

  return quests.flatMap(({ quest, kind }): AvailableQuest[] => {
    const offer = Offers[quest.id];
    const first = quest.steps.find((step) => step.number === "1");
    if (
      offer === undefined ||
      first === undefined ||
      (live !== null && !live.includes(quest.id)) ||
      quest.isUnavailable ||
      quest.isDone ||
      isGiven(quest) ||
      reached < offer.from ||
      (offer.until !== null && reached >= offer.until) ||
      (offer.needs ?? []).some((id) => !done.has(id)) ||
      (offer.levels !== undefined && (level < offer.levels[0] || level > offer.levels[1]))
    ) {
      return [];
    }

    return [
      {
        quest,
        kind,
        giver: offer.giver,
        twoTalks: offer.twoTalks === true,
        places: [first.location, ...(offer.alsoIn ?? [])],
      },
    ];
  });
}
