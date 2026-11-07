import { Languages, type Locale } from "./language";

/** The screens of the preparation page, as `SetupFlow` in Android/core names them. */
export const SetupPhases = [
  "choose",
  "checking",
  "working",
  "waiting",
  "pausing",
  "paused",
  "failed",
  "ready",
] as const;
export type SetupPhase = (typeof SetupPhases)[number];

/** What the single button does; sent back to `DigiviceNative.setupAction`. */
export const SetupActions = [
  "choose",
  "chooseOther",
  "pause",
  "resume",
  "retry",
  "continue",
] as const;
export type SetupAction = (typeof SetupActions)[number];

/** Why a preparation stopped (`SetupError` in Android/core). */
export const SetupProblems = [
  "WrongGame",
  "NotADisc",
  "InvalidChd",
  "Truncated",
  "FileGone",
  "LocalFile",
  "NoSpace",
  "Interrupted",
  "Failed",
] as const;
export type SetupProblem = (typeof SetupProblems)[number];

/** The extractor's pieces, in the order it prepares them. */
export const SetupPieces = [
  "Text",
  "Cards",
  "Sprites",
  "Icons",
  "World",
  "Maps",
  "Models",
] as const;

/** The state the setup activity pushes as `SetupState`. */
export interface SetupPage {
  phase: SetupPhase;
  action: SetupAction;
  enabled: boolean;
  file: string | null;
  piece: string | null;
  done: number;
  total: number;
  /** Overall share from 0 to 1, or null when it is not known. */
  progress: number | null;
  problem: SetupProblem | null;
  language: string | null;
}

export type SetupIcon = "disc" | "pause" | "arrow";

/** Everything the screen draws, already in words. */
export interface SetupView {
  title: string;
  finished: boolean;
  /** The chosen file for the source row; null shows the accepted formats. */
  file: string | null;
  /** A wrapped message in place of the step line and the bar (choosing a file, a problem). */
  note: string | null;
  warning: boolean;
  step: { text: string; count: string } | null;
  /** `percent` null: not known yet. */
  meter: { mode: "value" | "sweep" | "idle"; percent: number | null; moving: boolean } | null;
  button: { action: SetupAction; label: string; icon: SetupIcon; disabled: boolean };
  /** How far the Digi-Egg has cracked, 0 to 3; 4 is hatched. */
  egg: number;
}

type Translate = (key: string) => string;

const icons: Readonly<Record<SetupAction, SetupIcon>> = {
  choose: "disc",
  chooseOther: "disc",
  pause: "pause",
  resume: "arrow",
  retry: "arrow",
  continue: "arrow",
};

const key = (name: string) => `handheld.setup.${name}`;

function pieceName(piece: string | null, t: Translate): string {
  return (SetupPieces as readonly string[]).includes(piece ?? "")
    ? t(key(`pieces.${piece}`))
    : t(key("pieces.other"));
}

function eggStage(page: SetupPage): number {
  if (page.phase === "ready") return 4;
  return page.progress === null ? 0 : Math.min(3, Math.floor(page.progress * 4));
}

/** What the screen shows for [page]. */
export function setupView(page: SetupPage, t: Translate): SetupView {
  const finished = page.phase === "ready";
  const percent = page.progress === null ? null : Math.floor(page.progress * 100);
  const still = (moving = false) => ({ mode: "value" as const, percent, moving });
  let note: string | null = null;
  let step: SetupView["step"] = null;
  let meter: SetupView["meter"] = null;
  switch (page.phase) {
    case "choose":
      note = t(key("intro"));
      break;
    case "failed":
      note = t(key(`problems.${page.problem ?? "Failed"}`));
      break;
    case "checking":
      step = { text: t(key("checking")), count: "" };
      meter = { mode: "sweep", percent: null, moving: true };
      break;
    case "working":
      step = { text: pieceName(page.piece, t), count: `${page.done}/${page.total}` };
      meter = still(true);
      break;
    case "waiting":
    case "pausing":
    case "paused":
      step = { text: t(key(page.phase)), count: "" };
      meter = percent === null ? { mode: "idle", percent, moving: false } : still();
      break;
    case "ready":
      step = { text: t(key("done")), count: "" };
      meter = still(true);
      break;
  }
  return {
    title: t(key(finished ? "readyTitle" : "title")),
    finished,
    file: page.phase === "choose" ? null : page.file,
    note,
    warning: page.phase === "failed",
    step,
    meter,
    button: {
      action: page.action,
      label: t(key(`actions.${page.action}`)),
      icon: icons[page.action],
      disabled: !page.enabled,
    },
    egg: eggStage(page),
  };
}

function oneOf<T extends string>(values: readonly T[], value: unknown): value is T {
  return typeof value === "string" && (values as readonly string[]).includes(value);
}

function count(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 ? Math.floor(value) : 0;
}

/** The pushed JSON as a [SetupPage], or null when it is not one. */
export function parseSetupPage(json: string): SetupPage | null {
  let data: unknown;
  try {
    data = JSON.parse(json);
  } catch {
    return null;
  }
  if (typeof data !== "object" || data === null) return null;
  const value = data as Record<string, unknown>;
  if (!oneOf(SetupPhases, value.phase) || !oneOf(SetupActions, value.action)) return null;
  const progress =
    typeof value.progress === "number" && Number.isFinite(value.progress)
      ? Math.min(1, Math.max(0, value.progress))
      : null;
  return {
    phase: value.phase,
    action: value.action,
    enabled: value.enabled !== false,
    file: typeof value.file === "string" && value.file !== "" ? value.file : null,
    piece: typeof value.piece === "string" ? value.piece : null,
    done: count(value.done),
    total: count(value.total),
    progress,
    problem: oneOf(SetupProblems, value.problem) ? value.problem : null,
    language: typeof value.language === "string" ? value.language : null,
  };
}

/** The page language for an Android language tag (`es-ES`, `es`, `de-AT`), or null when the game has none. */
export function matchLocale(tag: string | null): Locale | null {
  if (!tag) return null;
  const exact = Languages.find((language) => language.locale.toLowerCase() === tag.toLowerCase());
  const prefix = tag.slice(0, 2).toLowerCase();
  return (
    (exact ?? Languages.find((language) => language.locale.startsWith(prefix)))?.locale ?? null
  );
}
