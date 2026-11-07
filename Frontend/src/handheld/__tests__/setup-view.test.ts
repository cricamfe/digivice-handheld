import { describe, expect, it } from "vitest";
import enHandheld from "@/i18n/locales/en-US/handheld.json";
import esHandheld from "@/i18n/locales/es-ES/handheld.json";
import frUi from "@/i18n/locales/fr-FR/ui.json";
import deUi from "@/i18n/locales/de-DE/ui.json";
import itUi from "@/i18n/locales/it-IT/ui.json";
import {
  matchLocale,
  parseSetupPage,
  setupView,
  SetupActions,
  SetupPhases,
  SetupPieces,
  SetupProblems,
  type SetupPage,
} from "../setup-view";

type Tree = { [key: string]: string | Tree };

const locales: Record<string, Tree> = {
  "en-US": enHandheld as unknown as Tree,
  "es-ES": esHandheld as unknown as Tree,
  "fr-FR": frUi as unknown as Tree,
  "de-DE": deUi as unknown as Tree,
  "it-IT": itUi as unknown as Tree,
};

function translator(tree: Tree): (key: string) => string {
  return (key) => {
    let node: string | Tree | undefined = tree;
    for (const part of key.split(".")) {
      node = typeof node === "object" ? node[part] : undefined;
    }
    if (typeof node !== "string") throw new Error(`missing ${key}`);
    return node;
  };
}

const es = translator(locales["es-ES"]!);

function page(overrides: Partial<SetupPage>): SetupPage {
  return {
    phase: "choose",
    action: "choose",
    enabled: true,
    file: null,
    piece: null,
    done: 0,
    total: 0,
    progress: null,
    problem: null,
    language: null,
    ...overrides,
  };
}

const file = "Digimon World 2003 (Europe).chd";

describe("preparation page view", () => {
  it("asks for a file with the accepted formats and what happens", () => {
    const view = setupView(page({}), es);
    expect(view.title).toBe("Preparar Digivice");
    expect(view.file).toBeNull();
    expect(view.note).toContain("Nada sale de la consola");
    expect(view.step).toBeNull();
    expect(view.meter).toBeNull();
    expect(view.button).toEqual({
      action: "choose",
      label: "Elegir archivo",
      icon: "disc",
      disabled: false,
    });
    expect(view.egg).toBe(0);
  });

  it("sweeps the bar while it checks the file", () => {
    const view = setupView(page({ phase: "checking", action: "pause", file }), es);
    expect(view.file).toBe(file);
    expect(view.step).toEqual({ text: "Comprobando el archivo…", count: "" });
    expect(view.meter).toEqual({ mode: "sweep", percent: null, moving: true });
    expect(view.button.label).toBe("Pausar preparación");
  });

  it("shows the piece in full, its counts and the overall progress while extracting", () => {
    const view = setupView(
      page({
        phase: "working",
        action: "pause",
        file,
        piece: "Models",
        done: 263,
        total: 418,
        progress: 0.637,
      }),
      es,
    );
    expect(view.title).toBe("Preparar Digivice");
    expect(view.step).toEqual({ text: "Modelos de combate y evolución", count: "263/418" });
    expect(view.meter).toEqual({ mode: "value", percent: 63, moving: true });
    expect(view.button).toMatchObject({ action: "pause", icon: "pause" });
    expect(view.egg).toBe(2);
  });

  it("names an unknown piece as game data", () => {
    const view = setupView(page({ phase: "working", action: "pause", piece: "Sound" }), es);
    expect(view.step?.text).toBe("Datos del juego");
  });

  it("explains it waits for the game to close and keeps the bar", () => {
    const view = setupView(page({ phase: "waiting", action: "pause", progress: 0.5 }), es);
    expect(view.step?.text).toContain("RetroArch");
    expect(view.meter).toEqual({ mode: "value", percent: 50, moving: false });
    expect(view.button.action).toBe("pause");
  });

  it("disables the button while pausing", () => {
    const view = setupView(page({ phase: "pausing", action: "pause", enabled: false }), es);
    expect(view.button.disabled).toBe(true);
    expect(view.meter).toEqual({ mode: "idle", percent: null, moving: false });
  });

  it("resumes a pause with Seguir", () => {
    const view = setupView(page({ phase: "paused", action: "resume", progress: 0.2 }), es);
    expect(view.step?.text).toBe("Preparación en pausa");
    expect(view.button).toMatchObject({ action: "resume", label: "Seguir", icon: "arrow" });
  });

  it("warns with the problem and the recovery", () => {
    const wrong = setupView(
      page({ phase: "failed", action: "chooseOther", file, problem: "WrongGame" }),
      es,
    );
    expect(wrong.warning).toBe(true);
    expect(wrong.note).toContain("SLES-03936");
    expect(wrong.meter).toBeNull();
    expect(wrong.button).toMatchObject({ action: "chooseOther", label: "Elegir otro archivo" });

    const space = setupView(page({ phase: "failed", action: "retry", problem: "NoSpace" }), es);
    expect(space.note).toContain("espacio");
    expect(space.button).toMatchObject({ action: "retry", label: "Reintentar" });
  });

  it("hatches the egg and continues when ready", () => {
    const view = setupView(page({ phase: "ready", action: "continue", progress: 1 }), es);
    expect(view.title).toBe("Digivice listo");
    expect(view.finished).toBe(true);
    expect(view.egg).toBe(4);
    expect(view.meter?.percent).toBe(100);
    expect(view.button).toMatchObject({ action: "continue", label: "Continuar", icon: "arrow" });
  });

  it("has every text in the five languages", () => {
    for (const [locale, tree] of Object.entries(locales)) {
      const t = translator(tree);
      for (const phase of SetupPhases) {
        for (const action of SetupActions) {
          for (const problem of SetupProblems) {
            const view = setupView(page({ phase, action, problem, piece: "Maps" }), t);
            expect(view.title, locale).not.toBe("");
            expect(view.button.label, locale).not.toBe("");
          }
        }
      }
      for (const piece of [...SetupPieces, "other"]) {
        expect(t(`handheld.setup.pieces.${piece}`), `${locale} ${piece}`).not.toBe("");
      }
    }
  });

  it("fits each piece and its counts on one step line in every language", () => {
    // The step line holds 40 characters of the 10 px pixel font after its chevrons.
    const largestTotals: Record<string, number> = {
      Text: 99,
      Cards: 999,
      Sprites: 9999,
      Icons: 99,
      World: 99,
      Maps: 999,
      Models: 999,
    };
    for (const [locale, tree] of Object.entries(locales)) {
      const t = translator(tree);
      for (const piece of SetupPieces) {
        const total = largestTotals[piece]!;
        const count = `${total}/${total}`;
        const text = t(`handheld.setup.pieces.${piece}`);
        expect(text.length + count.length, `${locale} ${text}`).toBeLessThanOrEqual(40);
      }
      for (const line of ["checking", "waiting", "pausing", "paused", "done"]) {
        expect(t(`handheld.setup.${line}`).length, `${locale} ${line}`).toBeLessThanOrEqual(40);
      }
    }
  });
});

describe("pushed preparation state", () => {
  it("reads what SetupFlow writes", () => {
    const json =
      '{"phase":"working","action":"pause","enabled":true,"file":"a.chd","piece":"Maps",' +
      '"done":10,"total":293,"progress":0.4,"problem":null,"language":"es-ES"}';
    expect(parseSetupPage(json)).toEqual({
      phase: "working",
      action: "pause",
      enabled: true,
      file: "a.chd",
      piece: "Maps",
      done: 10,
      total: 293,
      progress: 0.4,
      problem: null,
      language: "es-ES",
    });
  });

  it("refuses what it does not know and clamps the rest", () => {
    expect(parseSetupPage("nope")).toBeNull();
    expect(parseSetupPage('{"phase":"dance","action":"pause"}')).toBeNull();
    expect(parseSetupPage('{"phase":"ready","action":"jump"}')).toBeNull();
    expect(
      parseSetupPage('{"phase":"failed","action":"retry","problem":"Boom","progress":3,"done":-2}'),
    ).toMatchObject({ problem: null, progress: 1, done: 0, enabled: true, file: null });
  });

  it("matches the Android language to one of the game's", () => {
    expect(matchLocale("es-ES")).toBe("es-ES");
    expect(matchLocale("de-AT")).toBe("de-DE");
    expect(matchLocale("fr")).toBe("fr-FR");
    expect(matchLocale("ja-JP")).toBeNull();
    expect(matchLocale(null)).toBeNull();
  });
});
