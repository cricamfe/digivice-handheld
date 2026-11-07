import { createLogger } from "@/events/logger";
import type { State } from "@/models";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import { digievolutionLevel } from "./evolution-sheet";
import { englishTechniqueIds } from "./game-techniques";
import {
  allMockCaseNames,
  applyMockCase,
  mockActors,
  isMockConnectionCase,
  mockConnectionCases,
} from "./mock-cases";
import { screenNames, useScreen, type ScreenName, type ScreenParam } from "./router";

const mockLogger = createLogger("Mock", "#9b59b6");
/** A technique by its letters alone: the id `heatCutter` and the name `Heat Cutter` meet as `heatcutter`. */
const letters = (text: string) => text.toLowerCase().replace(/[^a-z0-9]/g, "");

// Golden state snapshots live with the Android core tests. Only imported from
// main.ts behind `import.meta.env.DEV`, so none of this reaches the production bundle.
/** The snapshot last delivered, so a recovery can send it again. */
let lastState: unknown = null;

const snapshots = import.meta.glob("../../../Android/core/src/test/resources/golden/state/*.json", {
  import: "default",
});

/**
 * Loads `?mock=<snapshot>` through the same bridge entry point the native side uses.
 * `?case=<name>` then adjusts it (see mock-cases.ts) to reach the variants of a screen.
 */
export async function loadMockSnapshot(name: string, caseName?: string | null): Promise<void> {
  const key = Object.keys(snapshots).find((path) => path.endsWith(`/${name}.json`));
  if (!key) {
    mockLogger.error(`Unknown mock snapshot: ${name}`, Object.keys(snapshots));
    return;
  }

  const state = structuredClone(await snapshots[key]!()) as Parameters<typeof applyMockCase>[0];
  if (caseName && !applyMockCase(state, caseName)) {
    mockLogger.error(`Unknown mock case: ${caseName}`, allMockCaseNames);
  }

  lastState = state;
  window.__digivice?.receive("InitialState", JSON.stringify(state));
  sendMockTechniques(state as unknown as State);
  startMockActors(caseName);
}

/**
 * Sends what the core reads from the RAM and the snapshots have no record of: the techniques of each partner's forms, those
 * of the database it has learned by its level, by name in the game's table.
 */
function sendMockTechniques(state: State): void {
  const numberOf = new Map(
    [...englishTechniqueIds()].map(([name, number]) => [letters(name), number]),
  );
  const partners = state.party.slots.flatMap((slot) => {
    if (slot.digimon === null || slot.digimonId === null) {
      return [];
    }

    const digimon = slot.digimon;
    const forms = [...digimon.digievolutions, ...digimon.storedDigievolutions].map(
      ({ digievolutionId: id }) => {
        const level = digievolutionLevel(digimon, id as number) ?? 0;
        const skills = DigievolutionRepository.getRawDigievolutionTechniquesById(id as number)
          .filter((entry) => entry.learnAt <= level)
          .map((entry) => numberOf.get(letters(entry.id)) ?? 0);
        return { id, skills };
      },
    );
    return [{ digimonId: slot.digimonId, forms }];
  });
  window.__digivice?.receive("PartnerTechniquesChanged", JSON.stringify({ partners }));
}

let walker: number | null = null;

/**
 * Sends the characters of the case as `FieldActorsChanged`, and has the one on the bridge (0xB2) walk to and fro, the
 * way the core sends a list every 500 ms when it changes.
 */
function startMockActors(caseName?: string | null): void {
  if (walker !== null) {
    window.clearInterval(walker);
    walker = null;
  }

  const actors = caseName ? mockActors(caseName) : null;
  if (actors === null) {
    return;
  }

  const send = () => window.__digivice?.receive("FieldActorsChanged", JSON.stringify({ actors }));
  send();

  const walking = actors.find((actor) => actor.key1 === 0xb2);
  if (walking === undefined) {
    return;
  }

  const startX = walking.x;
  let step = 0;
  walker = window.setInterval(() => {
    // Along the bridge and back: 24 px a step to the left (dir 2), then to the right (dir 6, the mirror of 2).
    const phase = step++ % 20;
    const goingLeft = phase < 10;
    walking.dir = goingLeft ? 2 : 6;
    walking.x = goingLeft ? startX - 24 * (phase + 1) : startX - 24 * (20 - phase);
    send();
  }, 500);
}

function receiveHealth(status: string, errorCode: string | null): void {
  window.__digivice?.receive(
    "HealthChanged",
    JSON.stringify({ status, errorCode, errorDetail: null }),
  );
}

/**
 * Plays the connection side of `?case=<name>` once the screen is open: the game is lost with the code of the case
 * (the state goes), and with `?recover=<ms>` it comes back after that long, in the order the core sends it
 * (`Loading`, the state, `Healthy`).
 */
export function applyMockConnection(
  caseName: string | null,
  recoverMilliseconds: number | null,
): void {
  if (!caseName || !isMockConnectionCase(caseName)) {
    return;
  }

  const lost = mockConnectionCases[caseName];
  if (lost.status === "Loading") {
    // The core only reports `Loading` once the game was lost and the state forgotten: an error comes first.
    receiveHealth("Error", "retroarch_unreachable");
  }
  receiveHealth(lost.status, lost.errorCode);

  if (recoverMilliseconds !== null && lastState !== null) {
    window.setTimeout(() => {
      receiveHealth("Loading", null);
      window.__digivice?.receive("InitialState", JSON.stringify(lastState));
      receiveHealth("Healthy", null);
    }, recoverMilliseconds);
  }
}

/**
 * Opens a screen from `?screen=<name>&param=<json or text>`, so a screen can be reviewed without
 * tapping to it. The parameter is read as JSON when it parses and as plain text otherwise.
 */
export function openMockScreen(name: string, rawParam?: string | null): void {
  if (!(screenNames as readonly string[]).includes(name)) {
    mockLogger.error(`Unknown screen: ${name}`, screenNames);
    return;
  }

  let param: ScreenParam = null;
  if (rawParam) {
    try {
      param = JSON.parse(rawParam) as ScreenParam;
    } catch {
      param = rawParam;
    }
  }

  useScreen().go(name as ScreenName, param);
}
