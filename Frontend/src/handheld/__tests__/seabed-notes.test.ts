import { describe, expect, it } from "vitest";
import textKeys from "../../../../Android/extractor/src/main/resources/extractor/text-keys.json";
import { SeabedRouteNotes } from "../seabed-docks";

const known = new Set(textKeys.words.map((word) => word.key));
const reference = /^(itmnam|talk04):\d+$/;

describe("the notes of a Seabed route", () => {
  const notes = Object.values(SeabedRouteNotes).flat();

  it("name items and smiths by reference to the game's text, not by English text", () => {
    for (const note of notes) {
      const names =
        note.kind === "chest"
          ? [note.item]
          : note.kind === "forge"
            ? [note.smith, note.from, note.with, note.to]
            : [];
      for (const name of names) {
        expect(name).toMatch(reference);
        expect(known.has(name)).toBe(true);
      }
    }
  });

  it("name the partner of a forge by its id, whose name comes from the disc", () => {
    const forges = notes.flatMap((note) => (note.kind === "forge" ? [note] : []));
    expect(forges.map((forge) => forge.partner).sort()).toEqual([1, 3, 4, 5]);
  });

  it("has chests and a forge to name", () => {
    expect(notes.some((note) => note.kind === "chest")).toBe(true);
    expect(notes.some((note) => note.kind === "forge")).toBe(true);
  });
});
