import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { actorUrl, cardUrl, genericNpcUrl, iconUrl, portraitUrl, spriteUrl } from "../assets";
import { loadGameAssets } from "../game-assets";
import { gameManifest, gameResponse } from "./game-assets.fixture";

const fetchManifest = vi.fn();

async function load(assets: Record<string, string>): Promise<void> {
  fetchManifest.mockImplementation(async (path: string) =>
    gameResponse(path, gameManifest(assets)),
  );
  expect(await loadGameAssets()).toBe("ready");
}

describe("handheld asset URLs", () => {
  beforeEach(async () => {
    vi.stubGlobal("fetch", fetchManifest);
    await load({});
  });

  afterEach(() => vi.unstubAllGlobals());

  it("keeps game artwork unavailable without an extracted asset", async () => {
    expect(portraitUrl(3)).toBeNull();
    expect(spriteUrl(3)).toBeNull();
    expect(await cardUrl(1000)).toBeNull();
    expect(actorUrl(47, 1)).toBeNull();
    expect(genericNpcUrl()).toBe(iconUrl("unknown"));
    expect(iconUrl("unknown")).not.toBe("");
  });

  it("resolves the eight numeric partner IDs and their sprite aliases", async () => {
    const assets: Record<string, string> = {};
    for (let id = 0; id < 8; id++) {
      assets[`portraits/${id}`] = `portraits/${id}.webp`;
      assets[`sprites/${id}`] = `portraits/${id}.webp`;
    }
    await load(assets);
    for (let id = 0; id < 8; id++) {
      expect(portraitUrl(id)).toBe(`/game/portraits/${id}.webp`);
      expect(spriteUrl(id)).toBe(`/game/portraits/${id}.webp`);
    }
  });

  it("rejects partner names, fractional values and IDs outside the eight partners", async () => {
    await load({
      "portraits/8": "wrong.png",
      "sprites/8": "wrong.png",
      "portraits/Agumon": "wrong.png",
    });
    for (const id of [-1, 8, 0.5, NaN, Infinity, "Agumon", "3"]) {
      expect(portraitUrl(id as number)).toBeNull();
      expect(spriteUrl(id as number)).toBeNull();
    }
  });

  it("resolves card IDs rather than the old artwork filenames", async () => {
    await load({
      "cards/1000": "cards/first.webp",
      "cards/1313": "cards/last.webp",
      "cards/1314": "wrong.png",
      "cards/Agumon": "wrong.png",
    });
    expect(await cardUrl(1000)).toBe("/game/cards/first.webp");
    expect(await cardUrl(1313)).toBe("/game/cards/last.webp");
    expect(await cardUrl(1001)).toBeNull();
    for (const id of [999, 1314, 1000.5, NaN, "Agumon", "1000"]) {
      expect(await cardUrl(id as number)).toBeNull();
    }
  });

  it("resolves actor ID and direction, using actor 47 facing 1 as the generic NPC", async () => {
    await load({ "actors/47-1": "actors/47-1.webp", "actors/34-0": "actors/34-0.png" });
    expect(actorUrl(34, 0)).toBe("/game/actors/34-0.png");
    expect(genericNpcUrl()).toBe("/game/actors/47-1.webp");
    expect(actorUrl(34, 1)).toBeNull();
    for (const [actor, direction] of [
      [-1, 1],
      [47.5, 1],
      [47, -1],
      [47, 8],
      [47, 1.5],
    ]) {
      expect(actorUrl(actor!, direction!)).toBeNull();
    }
  });

  it("keeps UI icons bundled and falls back to manifest item icons or the unknown UI icon", async () => {
    const cardIcon = iconUrl("card");
    await load({ "items/item-4a": "items/item-4a.webp", "items/card": "wrong.png" });
    expect(iconUrl("card")).toBe(cardIcon);
    expect(cardIcon).not.toBe("");
    expect(iconUrl("item-4a")).toBe("/game/items/item-4a.webp");
    expect(iconUrl("item-ff")).toBe(iconUrl("unknown"));
    expect(iconUrl("../item-4a")).toBe(iconUrl("unknown"));
  });

  it("does not retain game URLs after the manifest is no longer available", async () => {
    await load({ "portraits/3": "portraits/3.png", "actors/47-1": "actors/47-1.png" });
    expect(portraitUrl(3)).toBe("/game/portraits/3.png");
    fetchManifest.mockResolvedValue({ ok: false });
    expect(await loadGameAssets()).toBe("missing");
    expect(portraitUrl(3)).toBeNull();
    expect(genericNpcUrl()).toBe(iconUrl("unknown"));
  });
});
