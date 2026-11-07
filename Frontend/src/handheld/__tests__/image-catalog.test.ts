import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { NpcBattleOpponentHelper } from "@/presenters/helper/npc-battle-opponent.helper";
import { loadGameAssets } from "../game-assets";
import { gameManifest, gameResponse } from "./game-assets.fixture";

const assets = {
  "portraits/0": "portraits/0.png",
  "enemies/358": "enemies/358.png",
  "digievolutions/5": "digievolutions/5.png",
  "cards/1000": "cards/1000.png",
  "battle-stages/2": "battle-stages/2.webp",
  "actors/47-1": "actors/47-1.png",
  "actors/59-0": "actors/59-0.png",
  "actors/60-1": "actors/60-1.png",
  "maps/200": "maps/200.png",
  "maps/585": "maps/585.png",
  "maps/590": "maps/590.png",
};

beforeEach(async () => {
  vi.stubGlobal("fetch", async (path: string) => gameResponse(path, gameManifest(assets)));
  expect(await loadGameAssets()).toBe("ready");
});
afterEach(() => vi.unstubAllGlobals());

describe("legacy catalogue callers resolved through the extracted manifest", () => {
  it("uses existing numeric database identities for partners models and cards", () => {
    expect(ImageCatalog.getDigimonImageUrl(0)).toBe("/game/portraits/0.png");
    // Enemy 1 of enemy.json is drawn from the model of its enemy id, 358.
    expect(ImageCatalog.getEnemyImageUrl("1")).toBe("/game/enemies/358.png");
    expect(ImageCatalog.getDigievolutionImageUrl(5)).toBe("/game/digievolutions/5.png");
    expect(ImageCatalog.getCardImageUrl("1000")).toBe("/game/cards/1000.png");
    expect(ImageCatalog.getBattleStageImageUrl(2)).toBe("/game/battle-stages/2.webp");
  });

  it("draws tamers and NPCs as their field character, never by name", () => {
    // Tamer Chris and the Seiryu Leader of the tables.
    expect(NpcBattleOpponentHelper.getImageUrl("chris")).toBe("/game/actors/47-1.png");
    expect(NpcBattleOpponentHelper.getImageUrl("seiryuLeader")).toBe("/game/actors/60-1.png");
  });

  it("draws the Digimon of the field (bosses, shops, Duel Island) as their field character, by number", () => {
    expect(ImageCatalog.getFieldActorImageUrl("59-0")).toBe("/game/actors/59-0.png");
    expect(ImageCatalog.getFieldActorImageUrl(undefined)).toBeNull();
    expect(ImageCatalog.getFieldActorImageUrl("Pharaohmon")).toBeNull();
  });

  it("finds the picture of a map of the tables by its id, through its mode, and declines scenes", () => {
    expect(ImageCatalog.getLocationImageUrl("0200")).toBe("/game/maps/200.png");
    // Cargo Tower has no picture of its own field mode; In Menu is a scene.
    expect(ImageCatalog.getLocationImageUrl("0210")).toBeNull();
    expect(ImageCatalog.getLocationImageUrl("1000")).toBeNull();
  });

  it("gives each of the maps the game names alike its own picture", () => {
    expect(ImageCatalog.getLocationImageUrl("024E")).toBe("/game/maps/585.png");
    expect(ImageCatalog.getLocationImageUrl("024F")).toBe("/game/maps/590.png");
  });

  it("declines ids the map tables do not have, and names", () => {
    // A copy of the second server without a row of its own uses its original's picture (map-sheet).
    expect(ImageCatalog.getLocationImageUrl("0271")).toBeNull();
    expect(ImageCatalog.getLocationImageUrl("Asuka City")).toBeNull();
    expect(ImageCatalog.getLocationImageUrl(null)).toBeNull();
  });

  it("does not retain bundled or stale game pictures after a missing manifest", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("missing")));
    await loadGameAssets();
    expect(ImageCatalog.getEnemyImageUrl("1")).toBeNull();
    expect(ImageCatalog.getDigievolutionImageUrl(5)).toBeNull();
    expect(NpcBattleOpponentHelper.getImageUrl("chris")).toBeNull();
    expect(ImageCatalog.getLocationImageUrl("0200")).toBeNull();
  });

  it("returns null for absent or unknown names and images", () => {
    expect(ImageCatalog.getEnemyImageUrl(null)).toBeNull();
    expect(ImageCatalog.getDigimonImageUrl(8)).toBeNull();
    expect(ImageCatalog.getEnemyImageUrl("Pharaohmon")).toBeNull();
    expect(ImageCatalog.getCardImageUrl("unknown")).toBeNull();
    expect(NpcBattleOpponentHelper.getImageUrl("unknown")).toBeNull();
    expect(ImageCatalog.getBattleStageImageUrl(100)).toBeNull();
  });
});
