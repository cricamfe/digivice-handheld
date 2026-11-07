/**
 * The `errorCode` values of the health events the Android core sends: the RetroArch ones (Android/core
 * `ConnectionCodes.kt`, read over UDP) and the native game's (`NativeGameLoop.kt`).
 */
export const EmulatorConnectionErrorCodes = {
  MemoryReadFailed: "memory_read_failed",
  StateComposeFailed: "state_compose_failed",
  RetroArchUnreachable: "retroarch_unreachable",
  NoContent: "no_content",
  MemoryUnavailable: "memory_unavailable",
  WrongGame: "wrong_game",
  OtherConsole: "other_console",
  WaitingForSave: "waiting_for_save",
} as const;

export type EmulatorConnectionErrorCode =
  (typeof EmulatorConnectionErrorCodes)[keyof typeof EmulatorConnectionErrorCodes];

const knownErrorCodes = new Set<string>(Object.values(EmulatorConnectionErrorCodes));

export function isKnownEmulatorConnectionErrorCode(
  value: string,
): value is EmulatorConnectionErrorCode {
  return knownErrorCodes.has(value);
}
