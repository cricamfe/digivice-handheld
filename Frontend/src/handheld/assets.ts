import { gameAssetUrl } from "./game-assets";

// Vite resolves these at build time with relative URLs, so the bundle works from any WebView origin.
const icons = import.meta.glob<string>("./assets/icons/*.svg", {
  eager: true,
  query: "?url",
  import: "default",
});

// Mobius Desert terrain, the compass dial and the digievolution fork. All of them are pixel art.
const mapArt = import.meta.glob<string>("./assets/ui/{tile-*,compass,fork}.svg", {
  eager: true,
  query: "?url",
  import: "default",
});

function lookup(modules: Record<string, string>, path: string): string | null {
  return modules[path] ?? null;
}

/** Icon of the kit (`strength`, `party`, `unknown`...) or of an object (`item-60`), or the question mark when the name is unknown. */
export function iconUrl(name: string): string {
  return (
    lookup(icons, `./assets/icons/${name}.svg`) ??
    gameAssetUrl("items", name) ??
    lookup(icons, "./assets/icons/unknown.svg") ??
    ""
  );
}

/** Extracted portrait of one of the eight partners; its condition is drawn by the UI. */
export function portraitUrl(partnerId: number): string | null {
  return Number.isInteger(partnerId) && partnerId >= 0 && partnerId < 8
    ? gameAssetUrl("portraits", String(partnerId))
    : null;
}

/** A card's whole frame and art, addressed by its number on the disc. */
export async function cardUrl(cardId: number): Promise<string | null> {
  return Number.isInteger(cardId) && cardId >= 1000 && cardId <= 1313
    ? gameAssetUrl("cards", String(cardId))
    : null;
}

/** Sprite alias of one of the eight partners, addressed by partner ID. */
export function spriteUrl(partnerId: number): string | null {
  return Number.isInteger(partnerId) && partnerId >= 0 && partnerId < 8
    ? gameAssetUrl("sprites", String(partnerId))
    : null;
}

export type TileName = "sand-0" | "sand-1" | "sand-2" | "wrap" | "noise" | "mirage" | "cursor";

/** Mobius Desert tile, 27 px drawn at 54 px. */
export function tileUrl(name: TileName): string {
  return lookup(mapArt, `./assets/ui/tile-${name}.svg`) ?? "";
}

/** Compass dial, 44 px drawn at 88 px. */
export function compassUrl(): string {
  return lookup(mapArt, "./assets/ui/compass.svg") ?? "";
}

/** Connector of a digievolution family that forks into two branches, 24 x 190 px, made for two branches of 88 px with 14 px between them. */
export function forkUrl(): string {
  return lookup(mapArt, "./assets/ui/fork.svg") ?? "";
}

/** Picture of a character of the field (`Actor.key1`) facing a direction (0 to 7), or null when it has none. */
export function actorUrl(key1: number, dir: number): string | null {
  return Number.isSafeInteger(key1) && key1 >= 0 && Number.isInteger(dir) && dir >= 0 && dir < 8
    ? gameAssetUrl("actors", `${key1}-${dir}`)
    : null;
}

/** Sprite of a person with no picture of their own, 19x34. */
export function genericNpcUrl(): string {
  return actorUrl(47, 1) ?? iconUrl("unknown");
}
