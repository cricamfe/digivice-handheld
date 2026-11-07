import { createPinia, setActivePinia } from "pinia";
import { afterEach, expect, it, vi } from "vitest";
import type { StateDTO } from "@/events/dto/state.dto";
import "@/extensions/math.extensions";
import { useGameStore } from "@/stores/use-game-store";
import initialState from "../../../../Android/core/src/test/resources/native/initial-state.json";
import { buildBattleSheet } from "../battle-sheet";
import { buildBattleAttacks } from "../battle-attacks";
import { loadGameAssets } from "../game-assets";
import { gameManifest, gameResponse } from "./game-assets.fixture";

vi.mock("../game-techniques", async (original) => ({
  ...(await original<typeof import("../game-techniques")>()),
  ownBattleForm: (id: number) => (id === 4 ? 145 : undefined),
  battleForm: (id: number) => (id === 145 ? { name: "Veemon", signature: 1 } : undefined),
  battleTechnique: (id: number) => ({
    name: `Technique ${id}`,
    description: null,
    type: "physical",
    element: "none",
    elementStrength: 0,
    mp: 0,
    power: 10,
  }),
}));

// The names come from the disc's text by their line (dignam:N); here, made-up names that show the line.
vi.mock("../digimon-names", () => ({
  digimonName: (nameId: number, variant?: string) =>
    variant === undefined ? `Line ${nameId}` : `Line ${nameId}(${variant})`,
}));

afterEach(() => vi.unstubAllGlobals());

it("recognizes the PAL BK Imperialdramon encounter and resolves its combat animation", async () => {
  setActivePinia(createPinia());
  const store = useGameStore();
  store.setInitialState(initialState as unknown as StateDTO);
  const state = store.currentState!;
  state.digimonBattle.enemy = {
    id: 444,
    groupId: 15,
    condition: 0,
    strength: 0,
    defense: 0,
    speed: 0,
    hp: { current: 2448, max: 2448 },
  };
  const manifest = gameManifest({
    "enemies/444": "enemies/444.webp",
    "battle-enemies/444": "battle-enemies/444.webp",
  });
  vi.stubGlobal("fetch", async (path: string) => gameResponse(path, manifest));
  expect(await loadGameAssets()).toBe("ready");
  const texts = { t: (key: string) => key, te: () => false };

  expect(buildBattleSheet(state, texts, "0600", [])).toMatchObject({
    enemyId: "593",
    name: "Line 229",
    isBoss: true,
    level: 46,
    hp: { text: "2448/2448" },
    enemyImageUrl: "/game/enemies/444.webp",
    enemyAnimationUrl: "/game/battle-enemies/444.webp",
  });
});

it("compares base and equipped attacks and keeps an active-only form visible until the switch", () => {
  setActivePinia(createPinia());
  const store = useGameStore();
  store.setInitialState(initialState as unknown as StateDTO);
  const state = store.currentState!;
  state.digimonBattle.enemy = {
    id: 444,
    groupId: 15,
    condition: 0,
    strength: 0,
    defense: 0,
    speed: 0,
    hp: { current: 2448, max: 2448 },
  };
  const partner = state.party.slots[0]!.digimon!;
  state.party.slots[1] = {
    index: 2,
    digimonId: 4,
    digimon: {
      ...partner,
      inBattle: { ...partner.inBattle, formId: 145, strength: 7, hp: { current: 80, max: 100 } },
      activeDigievolutionId: 377,
      digievolutions: [{ index: 1, digievolutionId: 377, digievolution: { level: 1, dvexp: 0 } }],
    },
  };
  const texts = { t: (key: string) => key, te: () => false };
  const techniques = [{ digimonId: 4, forms: [{ id: 377, skills: [2, 3] }] }];
  const preview = (selected?: number) =>
    buildBattleAttacks(state, texts, "0600", techniques, 2, selected);
  state.digimonBattle.activeFighter = 0;
  expect(preview().fighterName).toBe("Line 51");
  expect(preview().attacks.map(({ key }) => key)).toEqual(["2", "3"]);
  expect(preview(145).attacks.map(({ key }) => key)).toEqual(["1"]);
  expect(preview(145).stats).toContainEqual({ key: "strength", value: 7 });
  expect(preview().stats.find(({ key }) => key === "strength")?.value).toBeGreaterThan(7);

  state.digimonBattle.activeFighter = 1;
  state.party.slots[1]!.digimon!.inBattle.formId = 359;
  expect(preview().fighterName).toBe("Line 44");
  expect(preview().forms).toEqual([
    { id: 145, name: "Veemon", live: false },
    { id: 377, name: "Line 51", live: false },
    { id: 359, name: "Line 44", live: true },
  ]);
  expect(preview(377).fighterName).toBe("Line 51");

  state.digimonBattle.activeFighter = 0;
  state.party.slots[1]!.digimon!.digievolutions = [];
  expect(preview(359).fighterName).toBe("Veemon");
  expect(preview().forms).toEqual([{ id: 145, name: "Veemon", live: false }]);
});
