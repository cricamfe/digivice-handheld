import type { Translate } from "./party-sheet";

export type RowTone = "" | "up" | "down" | "gold";

export interface DetailRow {
  label: string;
  value: string;
  tone: RowTone;
}

/** What a floating panel says about a stat, an element or a condition: its meaning and the sum behind its figure. */
export interface FigureDetail {
  /** Kit icon of the figure. */
  key: string;
  label: string;
  /** What it means in the game. */
  meaning: string;
  rows: DetailRow[];
  /** A condition says yes or no: true when the party can inflict it. Without it (a stat) the meaning is plain text. */
  verdict?: boolean;
  /** A line of the panel that is not the meaning: where the sum leaves something out. */
  note?: string;
}

export function signed(amount: number): string {
  return amount > 0 ? `+${amount}` : `-${Math.abs(amount)}`;
}

/** A stat's detail: the base value and each change it adds up from (equipment, battle), then the total. */
export function statDetail(
  t: Translate,
  stat: {
    key: string;
    label: string;
    base: number;
    total: number;
    /** Changes over the base, in the order they are written; the ones that are 0 do not show. */
    changes: { label: string; amount: number }[];
    /** The bonus of the active digievolution, which the game adds apart from the total. */
    digievolution?: number;
  },
): FigureDetail {
  const changes = stat.changes.filter((change) => change.amount !== 0);
  const rows: DetailRow[] = [];

  if (changes.length === 0) {
    rows.push({ label: t("handheld.figure.base"), value: String(stat.total), tone: "" });
  } else {
    rows.push({ label: t("handheld.figure.base"), value: String(stat.base), tone: "" });
    for (const change of changes) {
      rows.push({
        label: change.label,
        value: signed(change.amount),
        tone: change.amount > 0 ? "up" : "down",
      });
    }
    rows.push({ label: t("handheld.figure.total"), value: String(stat.total), tone: "gold" });
  }

  const hasDigievolution = stat.digievolution !== undefined && stat.digievolution > 0;
  if (hasDigievolution) {
    rows.push({
      label: t("handheld.figure.fromDigievolution"),
      value: signed(stat.digievolution!),
      tone: "gold",
    });
  }

  return {
    key: stat.key,
    label: stat.label,
    meaning: t(`stat.${stat.key}-explanation`),
    rows,
    note: hasDigievolution ? t("handheld.figure.digievolutionNote") : undefined,
  };
}
