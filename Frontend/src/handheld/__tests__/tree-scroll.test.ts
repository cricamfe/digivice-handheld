import { describe, expect, it } from "vitest";
import { rowScrollFor } from "../tree-scroll";

// A family as the 620 px screen draws it: 216 px nodes, 238 px apart in a line and 240 px after a fork, in a 564 px row.
const View = 564;
const forked = [0, 240, 478, 716];
const node = (left: number) => ({ left, right: left + 216 });

describe("the row of the chosen digievolution scrolls to a column edge", () => {
  it("stays on the first column when the node is whole from there", () => {
    expect(rowScrollFor([0, 238, 476], node(238), View, 692)).toEqual({
      scrollLeft: 0,
      endRoom: 0,
    });
  });

  it("starts at a column, never cutting one in half as scrollIntoView did", () => {
    // Agumon's tree on the device: the last form (Omnimon) ends at 932, which scrollIntoView left at 368, inside the
    // column that starts at 240.
    const scroll = rowScrollFor(forked, node(716), View, 932);
    expect(forked).toContain(scroll.scrollLeft);
    expect(scroll.scrollLeft).toBe(478);
    expect(scroll.scrollLeft + View).toBeGreaterThanOrEqual(932);
  });

  it("asks for the room after the last column that scrolling to it needs", () => {
    expect(rowScrollFor(forked, node(716), View, 932).endRoom).toBe(478 + View - 932);
  });

  it("picks the leftmost column that leaves the node whole", () => {
    const line = [0, 238, 476, 714, 952];
    expect(rowScrollFor(line, node(714), View, 1168)).toEqual({ scrollLeft: 476, endRoom: 0 });
  });

  it("starts at the node when it is wider than the row", () => {
    expect(rowScrollFor([0, 238], { left: 238, right: 900 }, View, 900).scrollLeft).toBe(238);
  });
});
