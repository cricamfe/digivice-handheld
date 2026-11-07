import type { CardRaw, CardType } from "@/repositories/tables/raws/tcg/card.raw";
import type { CardTable } from "@/repositories/tables/tcg/card.table";

// The table of the cards the shared repository reads. Its contents arrive from /game.
const table: CardTable = Object.create(null);
export default table;

const types = new Set<CardType>(["action", "white", "blue", "green", "red", "black", "brown"]);

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function points(value: unknown): value is number {
  return typeof value === "number" && Number.isInteger(value) && value >= 0 && value <= 99;
}

function parseCard(id: string, value: unknown): CardRaw | null {
  if (!record(value)) return null;
  const type = value.type as CardType;
  if (!types.has(type)) return null;
  if (type === "action") return value.points === undefined ? { imageName: id, type } : null;
  const stats = value.points;
  if (!record(stats) || !points(stats.ap) || !points(stats.hp)) return null;
  return { imageName: id, type, points: { ap: stats.ap, hp: stats.hp } };
}

export function clearCardTable(): void {
  for (const id of Object.keys(table)) delete table[id];
}

export function loadCardTable(value: unknown): boolean {
  if (!record(value) || Object.keys(value).length !== 314) return false;
  const valid: CardTable = {};
  for (let id = 1000; id <= 1313; id++) {
    const key = String(id);
    const card = parseCard(key, value[key]);
    if (card === null) return false;
    valid[key] = card;
  }
  Object.assign(table, valid);
  return true;
}
