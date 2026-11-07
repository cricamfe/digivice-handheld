import { FORMAT_VERSION } from "../game-assets";

export const cardFactsPath = "tables/cards.json";
export const cardFacts = Object.fromEntries(
  Array.from({ length: 314 }, (_, index) => [
    1000 + index,
    index === 1 ? { type: "red", points: { ap: 9, hp: 7 } } : { type: "action" },
  ]),
);

export function gameManifest(assets: Record<string, string>) {
  return { version: FORMAT_VERSION, assets: { "tables/cards": cardFactsPath, ...assets } };
}

export function gameResponse(path: string, manifest: unknown, cards: unknown = cardFacts) {
  const body =
    path === "/game/manifest.json"
      ? manifest
      : path === `/game/${cardFactsPath}`
        ? cards
        : undefined;
  return { ok: body !== undefined, json: async () => body };
}
