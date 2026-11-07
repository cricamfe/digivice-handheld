import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const gameAssetUrl = vi.hoisted(() => vi.fn());
vi.mock("../game-assets", () => ({ gameAssetUrl }));

function readyDisc() {
  gameAssetUrl.mockImplementation((kind: string, locale: string) => `/game/${kind}/${locale}.json`);
}

const english = {
  messages: { disc: { greeting: "Hello", englishOnly: "Original" } },
  words: { "test:0": "First", "test:1": "Second" },
};

const spanish = {
  messages: { disc: { greeting: "Hola" } },
  words: { "test:0": "Primero" },
};

function response(value: unknown) {
  return { ok: true, json: async () => value };
}

const fetchGame = vi.fn(async (url: string) => {
  switch (url) {
    case "/game/texts/en-US.json":
      return response(english);
    case "/game/texts/es-ES.json":
      return response(spanish);
    default:
      return { ok: false };
  }
});

describe("handheld disc languages", () => {
  beforeEach(() => {
    vi.resetModules();
    fetchGame.mockClear();
    gameAssetUrl.mockReturnValue(null);
    vi.stubGlobal("fetch", fetchGame);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("reads the manifest language and falls back to English by stable reference", async () => {
    readyDisc();
    const { setLanguage, gameWord } = await import("../language");
    const { default: i18n } = await import("@/i18n/handheld");

    await setLanguage("es-ES");
    expect(i18n.global.t("disc.greeting")).toBe("Hola");
    expect(i18n.global.t("disc.englishOnly")).toBe("Original");
    expect(gameWord("test:0")).toBe("Primero");
    expect(gameWord("First")).toBe("Primero");
    expect(gameWord("test:1")).toBe("Second");
    expect(gameWord("test:2", "Fallback")).toBe("Fallback");

    await setLanguage("en-US");
    expect(i18n.global.t("disc.greeting")).toBe("Hello");
    expect(gameWord("First")).toBe("First");
    expect(fetchGame).toHaveBeenCalledTimes(2);
  });

  it("loads the launcher's app text with no disc requests", async () => {
    const { setLanguage, gameWord, language } = await import("../language");
    const { default: i18n } = await import("@/i18n/handheld");
    await setLanguage("es-ES");
    expect(language.value).toBe("es-ES");
    expect(i18n.global.t("handheld.wait.inicio.action")).toBe("Abrir ajustes");
    expect(gameWord("Unmapped")).toBe("Unmapped");
    expect(fetchGame).not.toHaveBeenCalled();
  });

  it("rejects unsafe message paths and keeps the English fallback", async () => {
    readyDisc();
    const { setLanguage, gameWord } = await import("../language");
    const { default: i18n } = await import("@/i18n/handheld");
    fetchGame.mockImplementationOnce(async () => response(english));
    fetchGame.mockImplementationOnce(async () =>
      response({
        messages: JSON.parse('{"__proto__":{"polluted":"true"}}'),
        words: spanish.words,
      }),
    );
    await setLanguage("es-ES");
    expect(i18n.global.t("disc.greeting")).toBe("Hello");
    expect(gameWord("test:0")).toBe("First");
    expect(Object.prototype).not.toHaveProperty("polluted");
  });
});
