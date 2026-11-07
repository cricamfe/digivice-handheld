import { describe, expect, it } from "vitest";
import { MapExits } from "../map-exits";
import { originalOf } from "../map-view";
import { nextPlacesTowards } from "../quest-paths";

const neighbours = (place: string): string[] =>
  (MapExits[place] ?? []).map((exit) => originalOf(exit.to));

describe("nextPlacesTowards", () => {
  it("marks the exit to a place next door", () => {
    const [next] = neighbours("0200");
    expect(next).toBeDefined();
    expect([...nextPlacesTowards("0200", [next!])]).toContain(next);
  });

  it("marks the first exit of a way that takes more than one", () => {
    const [next] = neighbours("0226");
    const further = neighbours(next!).find(
      (place) => place !== "0226" && !neighbours("0226").includes(place),
    );
    expect(further).toBeDefined();
    const marked = nextPlacesTowards("0226", [further!]);
    expect(marked.has(next!)).toBe(true);
    expect(marked.has(further!)).toBe(false);
  });

  it("marks nothing for where the player already is, or for what no exit reaches", () => {
    expect(nextPlacesTowards("0200", ["0200"]).size).toBe(0);
    expect(nextPlacesTowards("0200", ["FFFF"]).size).toBe(0);
  });
});
