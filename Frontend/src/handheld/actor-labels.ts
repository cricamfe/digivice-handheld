/** Something on the map that carries a name: a character of the game, or a marker, with its picture and its label. */
export interface LabelInput {
  key: string;
  /** Where it stands. */
  x: number;
  y: number;
  /** Where its picture sits against the point, and its size: `[x, y, width, height]`. */
  frame: readonly [number, number, number, number];
  name: string;
  /** Pixels the label takes, frame included. Without them: the letters of the name plus an outline, 12 px high. */
  size?: { width: number; height: number };
  /** Where the label starts against the point when nothing is in the way. Without it: 2 px under the point. */
  top?: number;
}

export interface LabelShift {
  x: number;
  y: number;
}

export interface Box {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

/** A letter of the pixel font is 10 px wide; the outline adds one pixel on each side. */
const LetterWidth = 10;
const LabelHeight = 12;
/** Where the label starts under the feet when nothing is in the way (the CSS `top` of the label). */
const BelowGap = 2;

function overlapArea(a: Box, b: Box): number {
  const width = Math.min(a.right, b.right) - Math.max(a.left, b.left);
  const height = Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top);
  return width > 0 && height > 0 ? width * height : 0;
}

function spriteBox(actor: LabelInput): Box {
  const [x, y, width, height] = actor.frame;
  return {
    left: actor.x + x,
    top: actor.y + y,
    right: actor.x + x + width,
    bottom: actor.y + y + height,
  };
}

/** The box a label takes once `nudgeLabels` has shifted it, in the same pixels as its input. */
export function labelBox(input: LabelInput, shift: LabelShift): Box {
  const width = input.size?.width ?? input.name.length * LetterWidth + 2;
  const height = input.size?.height ?? LabelHeight;
  const left = input.x - width / 2 + shift.x;
  const top = input.y + (input.top ?? BelowGap) + shift.y;
  return { left, top, right: left + width, bottom: top + height };
}

/** The box of the picture of a thing on the map, as `nudgeLabels` keeps the names off it. */
export function pictureBox(input: LabelInput): Box {
  return spriteBox(input);
}

/** The distance between two boxes: zero when they touch or overlap. */
function gapBetween(a: Box, b: Box): number {
  const dx = Math.max(0, a.left - b.right, b.left - a.right);
  const dy = Math.max(0, a.top - b.bottom, b.top - a.bottom);
  return Math.hypot(dx, dy);
}

/** Where the candidate places of a name are tried: a grid this many pixels apart, this far from the default spot. */
const GridStep = 6;
const ReachX = 150;
const ReachY = 110;
/** What a pixel of a name over another name or picture costs, against a pixel of distance from its own point. */
const OverlapWeight = 0.12;
/** The leader of a name is a line of dots this far apart (one dot of 2 px every 4 px), checked against the other names. */
const LeaderStep = 4;
/** A name closer than this to its picture needs no leader. */
const AdjacentGap = 6;

/**
 * Nudges the names on the map so none of them lies over another name or over another picture, and each stays as near to
 * its own point as the others allow. A name starts where it sits by default (under its feet); when that spot is taken,
 * it takes the nearest free spot in any direction (beside it, above it, further below), found by trying the places on a
 * grid around it from the nearest on, and keeping the first that is free or, when none is, the one that costs least
 * (distance plus what it hides). Names are taken from the top of the map to the bottom, the order the game draws in, so
 * the ones in front keep their spot, and are pushed back inside the area when they would leave it. A name far from its
 * point is worse than one that hides a bit of the map's own drawing, which is not taken into account at all: only
 * names, the pictures of what is on the map and `obstacles` (the player's dot, the buttons) are. The result is the
 * shift from the default spot, for each key.
 */
export function nudgeLabels(
  actors: readonly LabelInput[],
  area: { width: number; height: number },
  obstacles: readonly Box[] = [],
): Map<string, LabelShift> {
  const shifts = new Map<string, LabelShift>();
  const sprites = [
    ...actors.map((actor) => ({ key: actor.key, box: spriteBox(actor) })),
    ...obstacles.map((box, index) => ({ key: `obstacle-${index}`, box })),
  ];
  const placed: Box[] = [];

  for (const actor of [...actors].sort((a, b) => a.y - b.y)) {
    const width = actor.size?.width ?? actor.name.length * LetterWidth + 2;
    const height = actor.size?.height ?? LabelHeight;
    const defaultTop = actor.y + (actor.top ?? BelowGap);
    const home = Math.min(Math.max(actor.x - width / 2, 2), Math.max(2, area.width - width - 2));
    const own = spriteBox(actor);
    const belowTop = Math.max(defaultTop, own.bottom + 1);
    const aboveTop = own.top - 1 - height;
    const others = sprites.filter((sprite) => sprite.key !== actor.key).map((sprite) => sprite.box);

    // The tries, nearest first: the grid around the default spot, plus the line just above the head.
    const tops = [
      ...Array.from(
        { length: Math.ceil(ReachY / GridStep) * 2 + 1 },
        (_, i) => belowTop + (i - Math.ceil(ReachY / GridStep)) * GridStep,
      ),
      aboveTop,
    ];
    const tries: { box: Box; base: number }[] = [];
    for (const top of tops) {
      if (top < 0 || top + height > area.height || Math.abs(top - belowTop) > ReachY + height) {
        continue;
      }

      for (
        let step = -Math.floor(ReachX / GridStep);
        step <= Math.floor(ReachX / GridStep);
        step++
      ) {
        const left = home + step * GridStep;
        const box: Box = { left, top, right: left + width, bottom: top + height };
        if (left < 2 || box.right > area.width - 2 || overlapArea(box, own) > 0) {
          continue;
        }

        tries.push({
          box,
          base: gapBetween(box, own) + (top < own.top ? 2 : 0) + Math.abs(step) * 0.3,
        });
      }
    }
    tries.sort((a, b) => a.base - b.base);

    let best: { box: Box; total: number } | null = null;
    for (const { box, base } of tries) {
      if (best !== null && base >= best.total) {
        break;
      }

      const hidden =
        others.reduce((sum, other) => sum + overlapArea(box, other), 0) +
        placed.reduce((sum, other) => sum + overlapArea(box, other), 0) +
        crossed(own, box, placed);
      const total = base + hidden * OverlapWeight;
      if (best === null || total < best.total) {
        best = { box, total };
      }
    }

    const box = best?.box ?? {
      left: home,
      top: belowTop,
      right: home + width,
      bottom: belowTop + height,
    };
    placed.push(box);
    shifts.set(actor.key, { x: box.left - (actor.x - width / 2), y: box.top - defaultTop });
  }

  return shifts;
}

/** The nearest point of a box to another point. */
function nearest(box: Box, x: number, y: number): { x: number; y: number } {
  return {
    x: Math.min(Math.max(x, box.left), box.right),
    y: Math.min(Math.max(y, box.top), box.bottom),
  };
}

/** The ends of the leader between a picture and its name: from the picture's edge to the name's nearest edge. */
function leaderEnds(
  picture: Box,
  label: Box,
): [{ x: number; y: number }, { x: number; y: number }] {
  const end = nearest(
    label,
    (picture.left + picture.right) / 2,
    (picture.top + picture.bottom) / 2,
  );
  return [nearest(picture, end.x, end.y), end];
}

/** How much of the other names a leader from `picture` to `label` would cross (the dots, as 3 px boxes). */
function crossed(picture: Box, label: Box, others: readonly Box[]): number {
  if (gapBetween(picture, label) <= AdjacentGap || others.length === 0) {
    return 0;
  }

  const [from, to] = leaderEnds(picture, label);
  const length = Math.hypot(to.x - from.x, to.y - from.y);
  let hidden = 0;
  for (let at = LeaderStep; at < length - 2; at += LeaderStep) {
    const x = from.x + ((to.x - from.x) * at) / length;
    const y = from.y + ((to.y - from.y) * at) / length;
    const dot: Box = { left: x - 1, top: y - 1, right: x + 2, bottom: y + 2 };
    hidden += others.reduce((sum, other) => sum + overlapArea(dot, other), 0);
  }
  return hidden;
}

/**
 * The dots of the short line that ties a name to its picture, as the top-left of 2 px squares, relative to the point
 * of the input. Null when the name sits next to its picture (no need) or the line would be too short to show. `inset`
 * pulls the start into the picture, for one that does not fill its box. Whole pixels, a dot every 4 px: a dotted step
 * line, not a hairline.
 */
export function leaderDots(
  input: LabelInput,
  shift: LabelShift,
  inset = 0,
): { x: number; y: number }[] | null {
  const label = labelBox(input, shift);
  const picture = spriteBox(input);
  if (gapBetween(picture, label) <= AdjacentGap) {
    return null;
  }

  const [from, to] = leaderEnds(
    {
      left: picture.left + inset,
      top: picture.top + inset,
      right: picture.right - inset,
      bottom: picture.bottom - inset,
    },
    label,
  );
  const length = Math.hypot(to.x - from.x, to.y - from.y);
  const dots: { x: number; y: number }[] = [];
  for (let at = 2; at < length - 1; at += LeaderStep) {
    dots.push({
      x: Math.round(from.x + ((to.x - from.x) * at) / length - input.x) - 1,
      y: Math.round(from.y + ((to.y - from.y) * at) / length - input.y) - 1,
    });
  }
  return dots.length > 0 ? dots : null;
}
