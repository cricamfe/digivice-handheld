import { readFileSync } from "node:fs";
import { gunzipSync } from "node:zlib";
import { describe, expect, it } from "vitest";
import { fieldMapImageKey } from "../field-image-keys";

describe("numeric field mode to extracted map image", () => {
  it("resolves RAM mode numbers to WSTAG keys, rather than interpreting them as stage numbers", () => {
    expect(fieldMapImageKey(0x200)).toBe("maps/200");
    expect(fieldMapImageKey(0x201)).toBe("maps/202");
    expect(fieldMapImageKey(0x211)).toBe("maps/270");
    expect(fieldMapImageKey(0x21d)).toBe("maps/330");
    expect(fieldMapImageKey(200)).toBeNull();
  });

  it("resolves modes whose main and extra chapter stages share a map", () => {
    expect(fieldMapImageKey(736)).toBe("maps/825");
    expect(fieldMapImageKey(737)).toBe("maps/830");
    expect(fieldMapImageKey(750)).toBe("maps/895");
  });

  it("resolves the two stages whose maps are drawn by their overlay", () => {
    expect(fieldMapImageKey(557)).toBe("maps/415");
    expect(fieldMapImageKey(734)).toBe("maps/815");
    // The reserved mode uses the global story event handler, with no stage.
    expect(fieldMapImageKey(528)).toBeNull();
  });

  it("does not guess when the main and extra chapter load different maps for the same mode", () => {
    expect(fieldMapImageKey(624)).toBeNull();
    expect(fieldMapImageKey(625)).toBeNull();
    expect(fieldMapImageKey(671)).toBeNull();
  });

  it.each([
    [0x270, 201, 920],
    [0x278, 233, 927],
    [0x27f, 261, 934],
    [0x2e8, 865, 968],
  ])("selects the exact stage for %s from the current chapter", (mode, main, extra) => {
    expect(fieldMapImageKey(mode, false)).toBe(`maps/${main}`);
    expect(fieldMapImageKey(mode, true)).toBe(`maps/${extra}`);
  });

  it("uses the main table when the extra chapter does not override the stage", () => {
    expect(fieldMapImageKey(0x22d, true)).toBe("maps/415");
    expect(fieldMapImageKey(0x2de, true)).toBe("maps/815");
  });

  it("returns null for unknown, fractional and non-finite modes", () => {
    for (const mode of [-1, 0, 0x1000, 0xffff, 512.5, NaN, Infinity, -Infinity]) {
      expect(fieldMapImageKey(mode)).toBeNull();
    }
  });

  it("resolves all 238 stage-backed field modes in both chapters", () => {
    const source = new URL(
      "../../../../Android/extractor/src/main/resources/extractor/stages.json.gz",
      import.meta.url,
    );
    const stages = JSON.parse(gunzipSync(readFileSync(source)).toString("utf8")) as Record<
      string,
      { mapFile: number | null }
    >;
    for (const extra of [false, true]) {
      let count = 0;
      for (let mode = 0; mode <= 0xffff; mode++) {
        const key = fieldMapImageKey(mode, extra);
        if (key === null) continue;
        expect(key).toMatch(/^maps\/[0-9]+$/);
        const stage = key.slice(5);
        if (stage !== "415" && stage !== "815") {
          expect(stages[stage]?.mapFile).toEqual(expect.any(Number));
        }
        count++;
      }
      expect(count).toBe(238);
    }
  });
});
