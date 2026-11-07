import { describe, expect, it } from "vitest";
import { decimal, times } from "../format";

describe("decimal", () => {
  it("writes the decimals the way the app language does", () => {
    expect(decimal(0.8, "en-US")).toBe("0.8");
    expect(decimal(0.8, "es-ES")).toBe("0,8");
    expect(decimal(2.2, "de-DE")).toBe("2,2");
  });

  it("keeps the digits it is asked for", () => {
    expect(decimal(2, "en-US")).toBe("2.0");
    expect(decimal(1.25, "en-US", 2)).toBe("1.25");
  });
});

describe("times", () => {
  it("puts the multiplier sign before the number", () => {
    expect(times(2.2, "en-US")).toBe("×2.2");
    expect(times(0.8, "it-IT")).toBe("×0,8");
  });
});
