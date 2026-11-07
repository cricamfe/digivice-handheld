import { describe, expect, it } from "vitest";
import "@/extensions/math.extensions";
import type { Party } from "@/models";
import type { JournalViewModel } from "@/viewmodels/quest/journal.viewmodel";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";
import { availableQuests, isGiven, kindedQuests } from "../quest-offers";

function quest(id: string, done: readonly string[] = [], steps = ["1", "2"]): QuestViewModel {
  return {
    id,
    requisites: [],
    steps: steps.map((number) => ({
      number,
      isDone: done.includes(number),
      location: "0211",
      innerLocation: [],
      coordinates: { x: 1, y: 2 },
    })),
    chapters: [],
    isDone: steps.every((number) => done.includes(number)),
    isLocked: false,
    isUnavailable: false,
    isNew: false,
    currentStep: null,
    currentChapter: null,
    cardVariant: "new",
  } as unknown as QuestViewModel;
}

function journal(reached: number, others: QuestViewModel[]): JournalViewModel {
  const main = quest(
    "mainQuest",
    Array.from({ length: reached }, (_, index) => String(index + 1)),
    Array.from({ length: 140 }, (_, index) => String(index + 1)),
  );
  return {
    mainQuest: main,
    sideQuests: others.filter((entry) => ["folderBag", "fishingPole"].includes(entry.id)),
    legendaryWeapons: others.filter((entry) =>
      ["invincible", "eternally", "muramasa"].includes(entry.id),
    ),
    driAgents: others.filter((entry) => entry.id.startsWith("dri")),
    duelIsland: others.filter((entry) => entry.id === "asukaTrophy"),
  };
}

function party(...levels: number[]): Party {
  return { slots: levels.map((level) => ({ digimon: { level } })) } as unknown as Party;
}

const ids = (view: JournalViewModel, group: Party): string[] =>
  availableQuests(view, group).map((entry) => entry.quest.id);

describe("availableQuests", () => {
  it("offers a quest only once the main story opened it", () => {
    expect(ids(journal(1, [quest("invincible")]), party(1))).toEqual([]);
    expect(ids(journal(2, [quest("invincible")]), party(1))).toEqual(["invincible"]);
  });

  it("stops offering a quest at the step that closes it", () => {
    expect(ids(journal(121, [quest("folderBag")]), party(1))).toEqual(["folderBag"]);
    expect(ids(journal(122, [quest("folderBag")]), party(1))).toEqual([]);
  });

  it("does not offer what is under way, done or an owned partner", () => {
    const owned = { ...quest("driAgentAgumon"), isUnavailable: true };
    const view = journal(50, [quest("folderBag", ["1", "2"]), quest("eternally", ["1"]), owned]);
    expect(ids(view, party(10, 10, 10))).toEqual([]);
  });

  it("asks for the quest that comes first", () => {
    expect(ids(journal(50, [quest("folderBag"), quest("fishingPole")]), party(1))).toEqual([
      "folderBag",
    ]);
    expect(
      ids(journal(50, [quest("folderBag", ["1", "2"]), quest("fishingPole")]), party(1)),
    ).toEqual(["fishingPole"]);
  });

  it("asks a DRI agent's quest for the party's total level, with both ends", () => {
    const agumon = [quest("driAgentAgumon")];
    expect(ids(journal(10, agumon), party(10, 10, 9))).toEqual([]);
    expect(ids(journal(10, agumon), party(10, 10, 10))).toEqual(["driAgentAgumon"]);
    expect(ids(journal(10, agumon), party(20, 20, 19))).toEqual(["driAgentAgumon"]);
    expect(ids(journal(10, agumon), party(20, 20, 20))).toEqual([]);
  });

  it("gives each its kind, who starts it and whether it takes two talks", () => {
    const [offer] = availableQuests(journal(10, [quest("driAgentAgumon")]), party(30));
    expect(offer).toMatchObject({ kind: "dri", giver: "talk06:690", twoTalks: true });
  });
});

describe("availableQuests with what the game says", () => {
  it("offers only what the game's conditions let start", () => {
    const view = journal(10, [quest("invincible"), quest("eternally")]);
    const offered = availableQuests(view, party(1), ["eternally", "cardKing"]);
    expect(offered.map((entry) => entry.quest.id)).toEqual(["eternally"]);
  });

  it("keeps what no script tests: when the player can get there", () => {
    expect(availableQuests(journal(10, [quest("muramasa")]), party(1), ["muramasa"])).toEqual([]);
  });
});

describe("isGiven", () => {
  it("is the first step done, not a partner owned from the start", () => {
    expect(isGiven(quest("eternally"))).toBe(false);
    expect(isGiven(quest("eternally", ["1"]))).toBe(true);
    expect(isGiven({ ...quest("eternally", ["2"]), isUnavailable: true })).toBe(false);
  });
});

describe("kindedQuests", () => {
  it("leaves the main quest out", () => {
    const all = kindedQuests(journal(1, [quest("folderBag")]));
    expect(all.map((entry) => entry.quest.id)).toEqual(["folderBag"]);
  });
});
