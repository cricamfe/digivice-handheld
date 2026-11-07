import { describe, expect, it } from "vitest";
import english from "@/i18n/locales/en-US/handheld-locale";
import { parts as spanishParts } from "@/i18n/locales/es-ES/handheld-locale";

/** The sources of the handheld page, as text: every screen, component and module but the tests. */
const sources = import.meta.glob<string>(["../**/*.ts", "../**/*.vue", "!./**"], {
  query: "?raw",
  import: "default",
  eager: true,
});

const Key = String.raw`[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+`;
/** `t("a.b")`, `$t('a.b')`, `te("a.b")`, `texts.t("a.b", …)`: a call whose first argument is a whole key. */
const LiteralCall = new RegExp(
  String.raw`(?<![\w$])(?:\$t|t|te)\(\s*(["'\`])(${Key})\1\s*[,)]`,
  "g",
);
/** `t(cond ? "a.b" : "c.d")`: both keys of a choice written in the call. */
const ChoiceCall = new RegExp(
  String.raw`(?<![\w$])(?:\$t|t|te)\([^()"'\`]*\?\s*(["'])(${Key})\1\s*:\s*(["'])(${Key})\3\s*\)`,
  "g",
);

function literalKeys(): Map<string, string> {
  const keys = new Map<string, string>();
  for (const [file, text] of Object.entries(sources)) {
    for (const match of text.matchAll(LiteralCall)) keys.set(match[2]!, file);
    for (const match of text.matchAll(ChoiceCall)) {
      keys.set(match[2]!, file);
      keys.set(match[4]!, file);
    }
  }
  return keys;
}

function leaves(tree: unknown, path: string[] = [], into = new Set<string>()): Set<string> {
  if (typeof tree === "string") {
    into.add(path.join("."));
  } else if (typeof tree === "object" && tree !== null) {
    for (const [key, child] of Object.entries(tree)) leaves(child, [...path, key], into);
  }
  return into;
}

const englishKeys = leaves(english);
const spanishKeys = new Set(spanishParts.flatMap((part) => [...leaves(part)]));

describe("the handheld's message keys", () => {
  const keys = literalKeys();

  it("finds the literal keys of the page", () => {
    expect(keys.size).toBeGreaterThan(200);
    expect(keys.get("auction.cardActiveSubtitle")).toBe("../screens/DiarioScreen.vue");
  });

  it("has every literal key of a t() call in en-US", () => {
    expect([...keys.keys()].filter((key) => !englishKeys.has(key))).toEqual([]);
  });

  it("has every literal key of a t() call in es-ES", () => {
    expect([...keys.keys()].filter((key) => !spanishKeys.has(key))).toEqual([]);
  });
});
