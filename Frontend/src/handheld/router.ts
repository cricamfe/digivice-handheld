import { readonly, ref, watch } from "vue";

/**
 * Every screen of the handheld UI. Wiki screens share the `wiki` prefix. The waiting screen (`EsperaScreen`) is not
 * one of them: it is not somewhere the player goes but what stands in for all of them while the game sends no state,
 * so the screen the player was on is still here when the connection returns.
 */
export const screenNames = [
  "equipo",
  "mapa",
  "batalla",
  "batallaCartas",
  "seabed",
  "seabedRuta",
  "mobius",
  "wikiMobius",
  "diario",
  "mision",
  "subastas",
  "evoluciones",
  "tecnicas",
  "wikiIndice",
  "wikiLista",
  "wikiBuscar",
  "wikiEnemigo",
  "wikiCarta",
  "wikiSobre",
  "wikiTienda",
  "wikiEquipo",
  "wikiObjeto",
  "wikiNpc",
] as const;

export type ScreenName = (typeof screenNames)[number];

/** Identifies what a screen shows: an id, a key or a few of them. */
export type ScreenParam = number | string | Readonly<Record<string, number | string>> | null;

/** The five sections of the nav bar. */
export type NavSection = "equipo" | "diario" | "mapa" | "evoluciones" | "wiki";

export const navSections: readonly NavSection[] = [
  "equipo",
  "diario",
  "mapa",
  "evoluciones",
  "wiki",
];

/** Screen each nav button opens. */
export const navTargets: Record<NavSection, ScreenName> = {
  equipo: "equipo",
  diario: "diario",
  mapa: "mapa",
  evoluciones: "evoluciones",
  wiki: "wikiIndice",
};

/** Which nav button stays lit on each screen (as in the design). */
const sectionOfScreen: Record<ScreenName, NavSection> = {
  equipo: "equipo",
  diario: "diario",
  mision: "diario",
  subastas: "diario",
  mapa: "mapa",
  batalla: "mapa",
  batallaCartas: "mapa",
  seabed: "wiki",
  seabedRuta: "mapa",
  mobius: "mapa",
  wikiMobius: "wiki",
  evoluciones: "evoluciones",
  tecnicas: "evoluciones",
  wikiIndice: "wiki",
  wikiLista: "wiki",
  wikiBuscar: "wiki",
  wikiEnemigo: "wiki",
  wikiCarta: "wiki",
  wikiSobre: "wiki",
  wikiTienda: "wiki",
  wikiEquipo: "wiki",
  wikiObjeto: "wiki",
  wikiNpc: "wiki",
};

export function navSectionOf(screen: ScreenName): NavSection {
  return sectionOfScreen[screen];
}

// Module state: one screen at a time, shared by every component that calls useScreen().
const current = ref<ScreenName>("equipo");
const param = ref<ScreenParam>(null);

/** A screen the player was on before opening a wiki sheet, so Back returns to it. */
interface TrailEntry {
  name: ScreenName;
  param: ScreenParam;
}

const trail: TrailEntry[] = [];
const TrailLimit = 40;

/**
 * Whether going to this screen is a step into the Wiki that Back can undo: a list, the search, a
 * sheet, or the Mapa tab asked to show a place. The index and every other screen are roots.
 */
function isTrailStep(name: ScreenName, nextParam: ScreenParam): boolean {
  if (name === "mapa") {
    return nextParam !== null;
  }

  return name.startsWith("wiki") && name !== "wikiIndice";
}

/** The screens the Mapa tab picks by itself from the place the player is in (see `mapKindOf`). */
const chosenByPlace: readonly ScreenName[] = [
  "mapa",
  "batalla",
  "batallaCartas",
  "seabedRuta",
  "mobius",
];

/** Whether an entry of the trail is one of those, without a param (a place asked for by id is kept). */
function isChosenByPlace(entry: TrailEntry): boolean {
  return entry.param === null && chosenByPlace.includes(entry.name);
}

/**
 * The screen stays across a reload: hiding the window and showing it loads the page again, and the player expects the
 * screen they left. The storage can fail, and then it opens on the first one.
 */
const SavedScreenKey = "digivice.screen";
try {
  const saved = JSON.parse(window.localStorage.getItem(SavedScreenKey) ?? "null") as {
    name?: ScreenName;
    param?: ScreenParam;
  } | null;
  if (saved?.name !== undefined && saved.name in sectionOfScreen) {
    const entry = { name: saved.name, param: saved.param ?? null };
    current.value = isChosenByPlace(entry) ? "mapa" : entry.name;
    param.value = entry.param;
  }
} catch {
  // Starts on the first screen.
}
watch([current, param], ([name, nextParam]) => {
  try {
    window.localStorage.setItem(SavedScreenKey, JSON.stringify({ name, param: nextParam }));
  } catch {
    // Not kept: only a reload would notice.
  }
});

/** Tiny navigation: no router library, the handheld has no URL to speak of. */
export function useScreen() {
  function go(name: ScreenName, nextParam: ScreenParam = null): void {
    if (isTrailStep(name, nextParam)) {
      trail.push({ name: current.value, param: param.value as ScreenParam });
      if (trail.length > TrailLimit) {
        trail.shift();
      }
    } else {
      trail.length = 0;
    }

    current.value = name;
    param.value = nextParam;
  }

  /**
   * Returns to the screen the player came from, or to `fallback` when there is none. A screen the
   * Mapa tab chose by the place the player was in (map, battle, card battle, route, desert) is not
   * restored as it was: the game may have moved on meanwhile, so the tab chooses again.
   */
  function back(fallback: ScreenName = "wikiIndice"): void {
    const previous = trail.pop();
    const restored = previous !== undefined && isChosenByPlace(previous) ? "mapa" : previous?.name;
    current.value = restored ?? fallback;
    param.value = previous?.param ?? null;
  }

  /** Changes what the current screen shows without a step in the trail (a tab inside a screen). */
  function replaceParam(nextParam: ScreenParam): void {
    param.value = nextParam;
  }

  return { current: readonly(current), param: readonly(param), go, back, replaceParam };
}
