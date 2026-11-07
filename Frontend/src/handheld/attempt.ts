import { createLogger } from "@/events/logger";

const logger = createLogger("Handheld", "#e67e22");

/** Messages already logged: a lookup that fails keeps failing on every recompute of its screen. */
const reported = new Set<string>();

/**
 * Runs a lookup of the databases; one that fails leaves the screen without that part and says so
 * in the log, once for each distinct error (a screen recomputes whenever the game state changes,
 * so the same failure would otherwise be logged several times a second).
 */
export function attempt<T>(lookup: () => T, fallback: T): T {
  try {
    return lookup();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (!reported.has(message)) {
      reported.add(message);
      logger.warn(`Lookup failed, the screen goes without it: ${message}`);
    }

    return fallback;
  }
}
