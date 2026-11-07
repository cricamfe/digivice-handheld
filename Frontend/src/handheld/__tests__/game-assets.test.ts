import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { CardRepository } from "@/repositories/card.repository";
import { FORMAT_VERSION, gameAssetUrl, loadGameAssets } from "../game-assets";
import { cardFacts, gameManifest, gameResponse } from "./game-assets.fixture";

const fetchManifest = vi.fn();
const validManifest = gameManifest({
  "maps/200": "maps/200.png",
  "maps/201": "maps/200.png",
  "npc/Señor Bob": "npc/Señor Bob.webp",
  "world-asuka": "world-asuka.png",
});

function reply(value: unknown, cards?: unknown) {
  fetchManifest.mockImplementation(async (path: string) => gameResponse(path, value, cards));
}

describe("extracted game asset manifest", () => {
  beforeEach(async () => {
    vi.stubGlobal("fetch", fetchManifest);
    fetchManifest.mockResolvedValue({ ok: false });
    await loadGameAssets();
    fetchManifest.mockClear();
  });

  afterEach(() => vi.unstubAllGlobals());

  it("keeps URLs unavailable before a valid manifest is loaded", () => {
    expect(gameAssetUrl("maps", "200")).toBeNull();
  });

  it("loads known images and aliases using their actual relative filenames", async () => {
    reply(validManifest);
    expect(await loadGameAssets()).toBe("ready");
    expect(fetchManifest).toHaveBeenCalledWith("/game/manifest.json");
    expect(gameAssetUrl("maps", "200")).toBe("/game/maps/200.png");
    expect(gameAssetUrl("maps", "201")).toBe("/game/maps/200.png");
    expect(gameAssetUrl("", "world-asuka")).toBe("/game/world-asuka.png");
    expect(gameAssetUrl("maps", "unknown")).toBeNull();
    expect(gameAssetUrl("", "toString")).toBeNull();
    expect(gameAssetUrl("", "__proto__")).toBeNull();
  });

  it("encodes each filename segment, including spaces and Unicode NPC aliases", async () => {
    reply(validManifest);
    await loadGameAssets();
    expect(gameAssetUrl("npc", "Señor Bob")).toBe("/game/npc/Se%C3%B1or%20Bob.webp");
  });

  it("clears the previous manifest when the request is missing or fails", async () => {
    for (const failure of ["missing", "network"]) {
      reply(validManifest);
      await loadGameAssets();
      if (failure === "missing") fetchManifest.mockResolvedValue({ ok: false });
      else fetchManifest.mockRejectedValue(new Error("Disconnected"));
      expect(await loadGameAssets()).toBe("missing");
      expect(gameAssetUrl("maps", "200")).toBeNull();
    }
  });

  it("reports another numeric format version as outdated and clears old URLs", async () => {
    reply(validManifest);
    await loadGameAssets();
    reply({ ...validManifest, version: FORMAT_VERSION - 1 });
    expect(await loadGameAssets()).toBe("outdated");
    expect(gameAssetUrl("maps", "200")).toBeNull();
  });

  it("requires the extracted card table and reads card facts through the shared repository", async () => {
    expect(CardRepository.getCardIds()).toEqual([]);
    reply(validManifest);
    expect(await loadGameAssets()).toBe("ready");
    expect(CardRepository.getCardIds()).toHaveLength(314);
    expect(CardRepository.getCardById("1000")).toEqual({ imageName: "1000", type: "action" });
    expect(CardRepository.getCardById("1001")).toEqual({
      imageName: "1001",
      type: "red",
      points: { ap: 9, hp: 7 },
    });

    reply({ version: FORMAT_VERSION, assets: { "maps/200": "maps/200.png" } });
    expect(await loadGameAssets()).toBe("outdated");
    expect(CardRepository.getCardIds()).toEqual([]);
    expect(gameAssetUrl("maps", "200")).toBeNull();
  });

  it("rejects incomplete or invalid card tables without retaining old facts", async () => {
    for (const cards of [{}, { ...cardFacts, 1000: { type: "red", points: { ap: -1, hp: 7 } } }]) {
      reply(validManifest);
      await loadGameAssets();
      reply(validManifest, cards);
      expect(await loadGameAssets()).toBe("missing");
      expect(CardRepository.getCardIds()).toEqual([]);
    }
  });

  it("clears the previous manifest when JSON cannot be parsed", async () => {
    reply(validManifest);
    await loadGameAssets();
    fetchManifest.mockResolvedValue({
      ok: true,
      json: async () => {
        throw new SyntaxError("Truncated JSON");
      },
    });
    expect(await loadGameAssets()).toBe("missing");
    expect(gameAssetUrl("maps", "200")).toBeNull();
  });

  it("rejects malformed schemas without retaining any entries", async () => {
    for (const value of [
      null,
      [],
      "manifest",
      {},
      { version: "1", assets: {} },
      { version: 1.5, assets: {} },
      { version: 1, assets: [] },
      { version: 1, assets: null },
      { version: 1, assets: { "maps/200": 200 } },
    ]) {
      reply(validManifest);
      await loadGameAssets();
      reply(value);
      expect(await loadGameAssets()).toBe("missing");
      expect(gameAssetUrl("maps", "200")).toBeNull();
    }
  });

  const invalidPaths = [
    "",
    "/absolute.png",
    "//host/asset.png",
    "../outside.png",
    "a/../b.png",
    "a/./b.png",
    "a//b.png",
    "a/",
    "a\\b.png",
    "https://example.com/asset.png",
    "data:image/webp;base64,AA",
    "C:asset.png",
    "a/%2e%2e/b.png",
    "a/%252e%252e/b.png",
    "a/%2Fb.png",
    "a\0b.png",
    "a\nb.png",
    "a\u007fb.png",
    "a\u0085b.png",
    "a\ud800b.png",
  ];

  it("rejects unsafe manifest routes as a whole and clears previous assets", async () => {
    for (const route of invalidPaths) {
      reply(validManifest);
      await loadGameAssets();
      reply({
        version: FORMAT_VERSION,
        assets: { "maps/200": "maps/200.png", "maps/evil": route },
      });
      expect(await loadGameAssets(), JSON.stringify(route)).toBe("missing");
      expect(gameAssetUrl("maps", "200")).toBeNull();
    }
  });

  it("rejects unsafe manifest keys and never constructs traversal URLs", async () => {
    for (const key of invalidPaths) {
      reply({ version: FORMAT_VERSION, assets: { [key]: "maps/200.png" } });
      expect(await loadGameAssets(), JSON.stringify(key)).toBe("missing");
      expect(gameAssetUrl("", key)).toBeNull();
    }
    reply(validManifest);
    await loadGameAssets();
    expect(gameAssetUrl("../maps", "200")).toBeNull();
    expect(gameAssetUrl("maps", "../200")).toBeNull();
    expect(gameAssetUrl("maps", "https://example.com/200")).toBeNull();
  });
});
