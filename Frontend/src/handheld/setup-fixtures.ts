// Development only (`?launcher&setup=<state>`): sample states of the preparation page without the setup activity.
import type { SetupPage, SetupProblem } from "./setup-view";

const file = "Digimon World 2003 (Europe).chd";

/** `state` is a phase (`choose`, `checking`, `working`, ...) or `failed`; `p` sets the progress, `problem` the error. */
export function setupFixture(state: string, query: URLSearchParams): SetupPage {
  const p = query.get("p");
  const progress = p === null ? 0.63 : Number(p) / 100;
  const base: SetupPage = {
    phase: "choose",
    action: "choose",
    enabled: true,
    file: null,
    piece: null,
    done: 0,
    total: 0,
    progress: null,
    problem: null,
    language: query.get("lang"),
  };
  switch (state) {
    case "checking":
      return { ...base, phase: "checking", action: "pause", file };
    case "working": {
      const piece = query.get("piece") ?? "Models";
      const total = Number(query.get("total") ?? 418);
      return {
        ...base,
        phase: "working",
        action: "pause",
        file,
        piece,
        done: 263,
        total,
        progress,
      };
    }
    case "waiting":
      return { ...base, phase: "waiting", action: "pause", file, progress };
    case "pausing":
      return { ...base, phase: "pausing", action: "pause", enabled: false, file, progress };
    case "paused":
      return { ...base, phase: "paused", action: "resume", file, progress };
    case "failed": {
      const problem = (query.get("problem") ?? "WrongGame") as SetupProblem;
      const retry = problem === "NoSpace" || problem === "Interrupted";
      return { ...base, phase: "failed", action: retry ? "retry" : "chooseOther", file, problem };
    }
    case "ready":
      return { ...base, phase: "ready", action: "continue", file, progress: 1 };
    default:
      return base;
  }
}
