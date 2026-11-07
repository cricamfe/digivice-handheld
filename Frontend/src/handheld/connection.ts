import { EmulatorConnectionErrorCodes as Codes } from "@/events/emulator-connection-error-codes";
import type { HealthStatus } from "@/models";

/**
 * Every state the waiting screen (`EsperaScreen`) can show. One per `ConnectionCodes` value of the Android core, plus:
 * - `inicio`: the first-run permission page, which only the launcher activity on the upper screen shows;
 * - `loading`: the core is connected and still composing the first state;
 * - `unknown`: an error code this build does not know.
 */
export const waitKinds = [
  "inicio",
  "loading",
  "retroarchUnreachable",
  "noContent",
  "memoryUnavailable",
  "wrongGame",
  "waitingForSave",
  "memoryReadFailed",
  "stateComposeFailed",
  "unknown",
] as const;

export type WaitKind = (typeof waitKinds)[number];

/** What a state draws besides its texts (`handheld.wait.<kind>.*`). */
export interface WaitLook {
  /** Icon kit name, drawn at 44 px under the Digimon. */
  icon: string;
  /** How many `stepN` texts the state has. */
  steps: number;
  /** Whether it ends with the button (`action` text). */
  action: boolean;
  /** Whether it ends with the long-press note (`note` text). */
  note: boolean;
}

export const waitLooks: Record<WaitKind, WaitLook> = {
  inicio: { icon: "alert", steps: 3, action: true, note: true },
  loading: { icon: "chip", steps: 0, action: false, note: false },
  retroarchUnreachable: { icon: "locked", steps: 3, action: false, note: false },
  noContent: { icon: "unknown", steps: 0, action: false, note: false },
  memoryUnavailable: { icon: "alert", steps: 2, action: false, note: false },
  wrongGame: { icon: "cross", steps: 1, action: false, note: false },
  waitingForSave: { icon: "check", steps: 0, action: false, note: false },
  memoryReadFailed: { icon: "alert", steps: 2, action: false, note: false },
  stateComposeFailed: { icon: "alert", steps: 1, action: false, note: false },
  unknown: { icon: "alert", steps: 0, action: false, note: false },
};

/**
 * Which waiting state to draw while the game has sent no state. Only an `Error` health carries a code; any other
 * status without a state is the core still reading.
 */
export function waitKindOf(status: HealthStatus, errorCode: string | null): WaitKind {
  if (status !== "Error") {
    return "loading";
  }

  switch (errorCode) {
    case Codes.RetroArchUnreachable:
      return "retroarchUnreachable";
    case Codes.NoContent:
      return "noContent";
    case Codes.MemoryUnavailable:
      return "memoryUnavailable";
    case Codes.WrongGame:
    case Codes.OtherConsole:
      return "wrongGame";
    case Codes.WaitingForSave:
      return "waitingForSave";
    case Codes.MemoryReadFailed:
      return "memoryReadFailed";
    case Codes.StateComposeFailed:
      return "stateComposeFailed";
    default:
      return "unknown";
  }
}
