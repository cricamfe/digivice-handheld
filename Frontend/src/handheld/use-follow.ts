import { onBeforeUnmount, ref, watch, type WatchSource } from "vue";

/**
 * How far behind the readings it draws, in ms. The core reads the player's position every 100 ms, also during its full
 * cycle: the gaps measured with a frame of latency are 101 to 127 ms, and one lost reading adds its 60 ms of patience. A
 * delay over the usual gap always has a next reading to head for, so the speed stays even; this leaves 20 ms over it.
 */
const DefaultDelayMs = 150;

/** The game itself moves at about this rate; a screen drawn at 60 is the most of what this page costs while walking. */
const PlayerFramesPerSecond = 30;

export interface Point {
  x: number;
  y: number;
}

interface Reading extends Point {
  time: number;
}

/**
 * A point that follows a target that arrives in uneven steps at an even speed, drawn every frame while it moves. It
 * starts on the target when the target appears, and each time `snap` changes. `delayMs` must exceed the longest gap
 * between readings: those of the player come as above, those of the other characters every 500 ms (the delay they pass).
 */
export function useFollow(
  target: () => Point | null,
  snap: WatchSource<unknown>,
  delayMs = DefaultDelayMs,
  framesPerSecond = PlayerFramesPerSecond,
) {
  const frameGapMs = 1000 / framesPerSecond;
  let lastDrawn = 0;

  /** A pause longer than this is the character standing still, not a slow step. */
  const pauseMs = delayMs * 1.5;
  const point = ref<Point | null>(null);
  let readings: Reading[] = [];
  let frame: number | null = null;

  function place(goal: Point | null): void {
    readings = goal === null ? [] : [{ ...goal, time: performance.now() }];
    point.value = goal;
  }

  function step(now: number): void {
    // A frame not drawn costs nothing: the map's layers are recomposed only when something moved. Half a frame of leeway
    // keeps it from skipping two in a row by a hair.
    if (now - lastDrawn < frameGapMs - 8) {
      frame = requestAnimationFrame(step);
      return;
    }

    lastDrawn = now;
    frame = null;
    const time = now - delayMs;
    while (readings.length > 1 && readings[1]!.time <= time) {
      readings.shift();
    }

    const from = readings[0];
    const to = readings[1];
    if (from === undefined) {
      return;
    }

    if (to === undefined) {
      point.value = { x: from.x, y: from.y };
      return;
    }

    const ratio = Math.max(0, (time - from.time) / (to.time - from.time));
    point.value = { x: from.x + (to.x - from.x) * ratio, y: from.y + (to.y - from.y) * ratio };
    frame = requestAnimationFrame(step);
  }

  watch(
    target,
    (goal) => {
      const last = readings[readings.length - 1];
      if (goal === null || last === undefined) {
        place(goal);
        return;
      }

      // Its screen was drawn again with the same place: nothing to follow, and no frames for it. Opening a panel kept the
      // page at the screen's rate for most of a second, for the player and for every character (measured on the console:
      // 377 frames in 10 s of opening and closing the filters, against 33 with this).
      if (goal.x === last.x && goal.y === last.y) {
        return;
      }

      const now = performance.now();
      if (now - last.time > pauseMs) {
        // It stood still until now: it sets off from where it was, not sliding through the whole pause.
        readings.push({ x: last.x, y: last.y, time: now - 100 });
      }
      readings.push({ ...goal, time: now });
      frame ??= requestAnimationFrame(step);
    },
    { immediate: true },
  );

  watch(snap, () => {
    place(target());
  });

  onBeforeUnmount(() => {
    if (frame !== null) {
      cancelAnimationFrame(frame);
    }
  });

  return point;
}
