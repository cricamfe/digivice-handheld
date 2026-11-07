import { MapExits } from "./map-exits";
import { originalOf } from "./map-view";

/** The places each place leads back from: an exit of `a` to `b` is an edge from `b` to `a` here. */
const cameFrom = new Map<string, Set<string>>();
for (const [place, exits] of Object.entries(MapExits)) {
  for (const exit of exits) {
    const to = originalOf(exit.to);
    let sources = cameFrom.get(to);
    if (sources === undefined) {
      sources = new Set();
      cameFrom.set(to, sources);
    }
    sources.add(originalOf(place));
  }
}

/** How many exits each place is from `goal`; a place that does not lead there is not in it. */
function stepsTo(goal: string): Map<string, number> {
  const steps = new Map([[goal, 0]]);
  const queue = [goal];
  for (const place of queue) {
    const next = (steps.get(place) ?? 0) + 1;
    for (const source of cameFrom.get(place) ?? []) {
      if (!steps.has(source)) {
        steps.set(source, next);
        queue.push(source);
      }
    }
  }
  return steps;
}

/**
 * The places next to `from` that are on a shortest way to any of `goals`, by the exits the maps have. A goal that only a
 * warp reaches (the Seabed, a story jump) has no way here, and marks nothing.
 */
export function nextPlacesTowards(from: string, goals: Iterable<string>): Set<string> {
  const here = originalOf(from);
  const next = new Set<string>();
  for (const goal of new Set([...goals].map(originalOf))) {
    if (goal === here) {
      continue;
    }

    const steps = stepsTo(goal);
    const mine = steps.get(here);
    if (mine === undefined) {
      continue;
    }

    for (const exit of MapExits[here] ?? []) {
      const to = originalOf(exit.to);
      if (steps.get(to) === mine - 1) {
        next.add(to);
      }
    }
  }
  return next;
}
