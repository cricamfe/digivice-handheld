import { ref, shallowRef } from "vue";
import i18n from "@/i18n/handheld";
import { composedMessages } from "./composed-messages";
import { gameAssetUrl } from "./game-assets";

/** The languages of the European game, each with its name in itself. */
export const Languages = [
  { locale: "es-ES", name: "Español" },
  { locale: "en-US", name: "English" },
  { locale: "fr-FR", name: "Français" },
  { locale: "de-DE", name: "Deutsch" },
  { locale: "it-IT", name: "Italiano" },
] as const;

export type Locale = (typeof Languages)[number]["locale"];

interface AppMessages {
  parts: readonly Record<string, unknown>[];
}

/** Each language in a file of its own: the texts of the others cost nothing to a page that starts often. */
const loaders: Readonly<Record<Locale, () => Promise<AppMessages>>> = {
  "en-US": () => Promise.resolve({ parts: [] }),
  "es-ES": () => import("@/i18n/locales/es-ES/handheld-locale"),
  "fr-FR": () => import("@/i18n/locales/fr-FR"),
  "de-DE": () => import("@/i18n/locales/de-DE"),
  "it-IT": () => import("@/i18n/locales/it-IT"),
};

const StorageKey = "digivice.locale";

function isLocale(value: string | null): value is Locale {
  return Languages.some((language) => language.locale === value);
}

function savedLocale(): string | null {
  try {
    return window.localStorage.getItem(StorageKey);
  } catch {
    return null;
  }
}

/** The language chosen, or the one of the device when it is one of the game's, or else English. */
function firstLocale(): Locale {
  const saved = savedLocale();
  if (isLocale(saved)) {
    return saved;
  }

  const prefix = navigator.language.slice(0, 2);
  return Languages.find((language) => language.locale.startsWith(prefix))?.locale ?? "en-US";
}

/** The language shown. */
export const language = ref<Locale>("en-US");

const words = shallowRef<ReadonlyMap<string, string>>(new Map());
let englishWords: ReadonlyMap<string, string> = new Map();
let englishReferences = new Map<string, string>();

/** Resolves a disc table reference, with compatibility for the remaining English-based catalogs. */
export function gameWord(reference: string, fallback = reference): string {
  const key = englishReferences.get(reference) ?? reference;
  return words.value.get(key) ?? englishWords.get(key) ?? fallback;
}

/** The English of a disc table reference, from the English text extracted from the disc; null when it has none. */
export function englishWord(reference: string): string | null {
  return englishWords.get(reference) ?? null;
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function messageTree(value: unknown, depth = 0): value is Record<string, unknown> {
  return (
    record(value) &&
    depth < 8 &&
    Object.entries(value).every(
      ([key, child]) =>
        key !== "__proto__" &&
        key !== "constructor" &&
        key !== "prototype" &&
        (typeof child === "string" || messageTree(child, depth + 1)),
    )
  );
}

async function loadDiscMessages(locale: Locale): Promise<ReadonlyMap<string, string>> {
  const url = gameAssetUrl("texts", locale);
  if (url !== null) {
    try {
      const response = await fetch(url);
      if (!response.ok) return new Map();
      const data: unknown = await response.json();
      if (record(data) && messageTree(data.messages) && record(data.words)) {
        const entries = Object.entries(data.words);
        if (entries.every((entry): entry is [string, string] => typeof entry[1] === "string")) {
          (i18n.global.mergeLocaleMessage as (name: string, part: unknown) => void)(
            locale,
            data.messages,
          );
          return new Map(entries);
        }
      }
    } catch {
      // An unavailable or interrupted extraction still leaves the app messages usable.
    }
  }
  return new Map();
}

const loaded = new Map<Locale, Promise<ReadonlyMap<string, string>>>();
let languageRequest = 0;

function loadLocale(locale: Locale): Promise<ReadonlyMap<string, string>> {
  const cached = loaded.get(locale);
  if (cached !== undefined) return cached;
  const pending = Promise.all([loaders[locale](), loadDiscMessages(locale)]).then(
    ([files, discWords]) => {
      const merge = i18n.global.mergeLocaleMessage as (name: string, part: unknown) => void;
      for (const part of files.parts) {
        // The same top-level keys can hold messages from both the disc and the app.
        merge(locale, part);
      }
      // Names the app composes from the disc's words with its own rules.
      const t = i18n.global.t as (
        key: string,
        named: unknown,
        options: { locale: string },
      ) => string;
      merge(
        locale,
        composedMessages(discWords, (key, named) => t(key, named, { locale })),
      );
      if (locale === "en-US") {
        englishWords = discWords;
        englishReferences = new Map([...discWords].map(([key, value]) => [value, key]));
      }
      return discWords;
    },
  );
  loaded.set(locale, pending);
  return pending;
}

/** Brings a language in and shows it. Its choice is kept across restarts when the storage lets it. */
export async function setLanguage(locale: Locale): Promise<boolean> {
  const request = ++languageRequest;
  const [, discWords] = await Promise.all([loadLocale("en-US"), loadLocale(locale)]);
  if (request !== languageRequest) return false;
  words.value = discWords;
  (i18n.global.locale as { value: string }).value = locale;
  language.value = locale;
  return true;
}

/** Picks a language from the panel, and keeps it. */
export async function chooseLanguage(locale: Locale): Promise<void> {
  if (!(await setLanguage(locale))) return;
  try {
    window.localStorage.setItem(StorageKey, locale);
  } catch {
    // Not kept: it holds until the app closes.
  }
}

/** The language to start in, ready before anything is drawn. */
export async function startLanguage(): Promise<void> {
  await setLanguage(firstLocale());
}
