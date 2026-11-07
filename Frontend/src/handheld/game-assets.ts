import { clearCardTable, loadCardTable } from "./card-table";

export type AssetState = "ready" | "missing" | "outdated";

export const FORMAT_VERSION = 10;
let assets = new Map<string, string>();

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function relativePath(value: unknown): value is string {
  if (
    typeof value !== "string" ||
    /[\\:%\u0000-\u001f\u007f-\u009f]/u.test(value) ||
    value.split("/").some((segment) => segment === "" || segment === "." || segment === "..")
  )
    return false;
  // Manifest paths are decoded filenames. Invalid UTF-16 cannot become a safe URL segment.
  try {
    encodeURIComponent(value);
    return true;
  } catch {
    return false;
  }
}

export async function loadGameAssets(): Promise<AssetState> {
  assets = new Map();
  clearCardTable();
  try {
    const response = await fetch("/game/manifest.json");
    if (!response.ok) return "missing";
    const manifest: unknown = await response.json();
    if (
      !record(manifest) ||
      typeof manifest.version !== "number" ||
      !Number.isSafeInteger(manifest.version) ||
      !record(manifest.assets)
    )
      return "missing";
    const entries = new Map<string, string>();
    for (const [key, path] of Object.entries(manifest.assets)) {
      if (!relativePath(key) || !relativePath(path)) return "missing";
      entries.set(key, path);
    }
    if (manifest.version !== FORMAT_VERSION) return "outdated";
    const cardPath = entries.get("tables/cards");
    if (cardPath === undefined) return "outdated";
    const cards = await fetch(assetPath(cardPath));
    if (!cards.ok || !loadCardTable(await cards.json())) return "missing";
    assets = entries;
    return "ready";
  } catch {
    return "missing";
  }
}

function assetPath(path: string): string {
  return `/game/${path.split("/").map(encodeURIComponent).join("/")}`;
}

/** An empty kind addresses a root key such as world-asuka. Unknown assets have no URL. */
export function gameAssetUrl(kind: string, name: string): string | null {
  const key = kind === "" ? name : `${kind}/${name}`;
  if (!relativePath(key)) return null;
  const path = assets.get(key);
  return path === undefined ? null : assetPath(path);
}
