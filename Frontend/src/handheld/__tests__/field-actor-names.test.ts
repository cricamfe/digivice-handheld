import { beforeEach, describe, expect, it, vi } from "vitest";
import "@/extensions/math.extensions";
import type { Party } from "@/models";
import type { JournalViewModel } from "@/viewmodels/quest/journal.viewmodel";
import type { QuestViewModel } from "@/viewmodels/quest/quest.viewmodel";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import { ActorNames } from "../field-actors";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

const speakers = new Set(
  textKeys.words.filter((word) => word.selector === "speaker").map((word) => word.key),
);
const references = Object.values(ActorNames).flatMap((names) => Object.values(names));

/** A character of the field the tests place, the DRI agent of the first quest below, and the line that names it. */
const MapId = "022B";
const Character = 43;
const Reference = ActorNames[MapId]?.[Character] ?? "";

/** Made-up words in the shape of the disc's, by reference and language: nothing of the game. */
async function setup(words: Readonly<Record<string, Readonly<Record<string, string>>>>) {
  gameAssetUrl.mockImplementation((kind: string, name: string) => `/game/${kind}/${name}`);
  vi.stubGlobal("fetch", async (url: string) => {
    const locale = url.split("/").at(-1) ?? "";
    return { ok: true, json: async () => ({ messages: {}, words: words[locale] ?? {} }) };
  });
  const language = await import("../language");
  return { language, view: await import("../field-actor-view") };
}

describe("the names of the characters of the field", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("are references to talk lines whose speaker the extractor reads, not text", () => {
    expect(references.length).toBeGreaterThan(900);
    for (const reference of references) {
      expect(reference).toMatch(/^talk0\d:\d+$/);
      expect(speakers.has(reference)).toBe(true);
    }
  });

  it("label a character with the name the disc's text gives its line, shortened as before", async () => {
    const { language, view } = await setup({
      "en-US": { [Reference]: "Ziggy from Nowhere" },
      "es-ES": { [Reference]: "Ziggy el de Ninguna Parte" },
      "fr-FR": { [Reference]: "Ziggy from Nowhere" },
    });
    const actors = [{ key1: Character, x: 1, y: 2, dir: 0 }];

    await language.setLanguage("en-US");
    expect(view.liveActorsOf(actors, MapId)[0]).toMatchObject({
      name: "Ziggy",
      speaker: Reference,
    });
    await language.setLanguage("es-ES");
    expect(view.liveActorsOf(actors, MapId)[0]?.name).toBe("Parte");
    // Read as in English, a name keeps the English rule.
    await language.setLanguage("fr-FR");
    expect(view.liveActorsOf(actors, MapId)[0]?.name).toBe("Ziggy");
  });

  it("have no label when the disc's text lacks the line, and keep who they are", async () => {
    const { language, view } = await setup({ "en-US": {} });
    await language.setLanguage("en-US");
    expect(view.liveActorsOf([{ key1: Character, x: 1, y: 2, dir: 0 }], MapId)[0]).toMatchObject({
      name: null,
      speaker: Reference,
    });
    expect(view.shownName(Reference)).toBeNull();
  });
});

function quest(id: string): QuestViewModel {
  return {
    id,
    requisites: [],
    steps: ["1", "2"].map((number) => ({
      number,
      isDone: false,
      location: MapId,
      innerLocation: [],
      coordinates: { x: 1, y: 2 },
    })),
    chapters: [],
    isDone: false,
    isLocked: false,
    isUnavailable: false,
    isNew: false,
    currentStep: null,
    currentChapter: null,
    cardVariant: "new",
  } as unknown as QuestViewModel;
}

describe("who gives a quest", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("is the talk line of a character of the field, never an English name", async () => {
    const { QuestGivers } = await import("../quest-offers");
    const givers = Object.values(QuestGivers).filter((giver) => giver !== null);
    expect(givers.length).toBeGreaterThan(10);
    for (const giver of givers) {
      expect(references).toContain(giver);
    }
  });

  it("is found among the characters of the map by that line, and named from the disc", async () => {
    const { language, view } = await setup({ "en-US": { [Reference]: "Agent Ziggy" } });
    await language.setLanguage("en-US");
    const offers = await import("../quest-offers");
    const journal = {
      mainQuest: { steps: [{ number: "10", isDone: true }] },
      sideQuests: [],
      legendaryWeapons: [],
      driAgents: [quest("driAgentAgumon")],
      duelIsland: [],
    } as unknown as JournalViewModel;
    const party = { slots: [{ digimon: { level: 30 } }] } as unknown as Party;
    const [offer] = offers.availableQuests(journal, party);
    expect(offer?.giver).toBe(Reference);

    const actors = view.liveActorsOf(
      [
        { key1: 46, x: 5, y: 5, dir: 0 },
        { key1: Character, x: 1, y: 2, dir: 0 },
      ],
      MapId,
    );
    expect(actors.filter((actor) => actor.speaker === offer?.giver)).toHaveLength(1);
    expect(offers.giverName(Reference)).toBe("Agent Ziggy");
    expect(offers.giverName("talk00:0")).toBeNull();
  });
});
