import { computed, onMounted, ref, watch, type Ref } from "vue";
import type { Point } from "./use-follow";

/** The size of the picture shown and of the area it is shown in, which every placement below starts from. */
function useMeasure(box: Ref<HTMLElement | null>, source: () => string | null) {
  const natural = ref<{ width: number; height: number } | null>(null);
  const area = ref<{ width: number; height: number } | null>(null);

  function measure(): void {
    if (box.value) {
      area.value = { width: box.value.clientWidth, height: box.value.clientHeight };
    }
  }

  function onLoad(event: Event): void {
    const image = event.target as HTMLImageElement;
    if (image.naturalWidth > 0) {
      natural.value = { width: image.naturalWidth, height: image.naturalHeight };
    }
    measure();
  }

  onMounted(measure);
  watch(source, () => {
    natural.value = null;
  });

  return { natural, area, onLoad };
}

/**
 * Pins that follow an image drawn with `object-fit: cover`.
 *
 * The database stores positions as a percentage of the image. When the box and the image have
 * different proportions, `cover` crops the image, so the same percentage of the box lands somewhere
 * else. This turns a percentage of the image into the position inside the box.
 * Until the image has loaded, it falls back to a percentage of the box.
 * A point given in pixels of the image (`placePixels`) has no such fallback: without the size it is not drawn.
 *
 * `box` is the element the image is stretched over (its padding box is the covered area).
 * `source` changes when another image is shown.
 */
export function useCover(box: Ref<HTMLElement | null>, source: () => string | null) {
  const { natural, area, onLoad } = useMeasure(box, source);

  /** Where the point `x`, `y` (percent of the image) lands inside the box, or null until both sizes are known. */
  function project(x: number, y: number): { x: number; y: number } | null {
    if (!natural.value || !area.value || area.value.width === 0) {
      return null;
    }

    const scale = Math.max(
      area.value.width / natural.value.width,
      area.value.height / natural.value.height,
    );
    const shownWidth = natural.value.width * scale;
    const shownHeight = natural.value.height * scale;
    const offsetX = (area.value.width - shownWidth) / 2;
    const offsetY = (area.value.height - shownHeight) / 2;

    return { x: offsetX + (shownWidth * x) / 100, y: offsetY + (shownHeight * y) / 100 };
  }

  /** Position inside the box of the point `x`, `y` (percent of the image), for `left` and `top`. */
  function place(x: number, y: number): { left: string; top: string } {
    const spot = project(x, y);
    return spot === null
      ? { left: `${x}%`, top: `${y}%` }
      : { left: `${spot.x}px`, top: `${spot.y}px` };
  }

  /**
   * Position inside the box of the pixel `x`, `y` of the image (its natural size), for `left` and `top`.
   * Null when it cannot be told or must not be drawn: the image has not loaded, the pixel is outside the image,
   * or `cover` crops it out of the box. A pin like that is hidden, not pushed to the edge.
   */
  function placePixels(x: number, y: number): { left: string; top: string } | null {
    if (!natural.value || !area.value) {
      return null;
    }

    const { width, height } = natural.value;
    if (!(x >= 0 && x < width && y >= 0 && y < height)) {
      return null;
    }

    const spot = project((x / width) * 100, (y / height) * 100);
    if (spot === null || spot.x < 0 || spot.x > area.value.width) {
      return null;
    }
    if (spot.y < 0 || spot.y > area.value.height) {
      return null;
    }

    // Whole pixels, so a sprite drawn there keeps its pixels crisp.
    return { left: `${Math.round(spot.x)}px`, top: `${Math.round(spot.y)}px` };
  }

  return { onLoad, place, placePixels, area };
}

/**
 * Whether the player chose to see the whole picture of the map instead of a window that follows them. It lives here, at
 * module level, so the choice survives the screen being closed and opened again for as long as the app runs.
 */
const showAll = ref(false);

export function useShowAll(): Ref<boolean> {
  return showAll;
}

/**
 * A picture that overflows the panel by less than this (in pixels, on its longer side) is not worth a button nor a
 * drag: both views are the same picture, give or take a few rows of pixels.
 */
const NoPanPixels = 16;

export interface PictureSize {
  width: number;
  height: number;
}

/**
 * Rounds a length in CSS pixels to a whole number of device pixels (`up` rounds up). The WebView draws at about 2.3 device
 * pixels per CSS pixel: a layer or a sprite placed between two device pixels is resampled by the compositor, and one that
 * slides is resampled differently every frame, which shows as a shimmer on pixel art.
 */
export function toDevice(value: number, up = false): number {
  const scale = window.devicePixelRatio > 0 ? window.devicePixelRatio : 1;
  return (up ? Math.ceil(value * scale - 1e-6) : Math.round(value * scale)) / scale;
}

const decoded = new Map<string, Promise<PictureSize | null>>();

/**
 * Loads and decodes a picture before it is shown, so showing it later takes no time: it resolves with its size, or null
 * when it cannot be loaded. The result is kept, so asking again for the same picture does not wait.
 */
export function decodePicture(url: string): Promise<PictureSize | null> {
  const known = decoded.get(url);
  if (known !== undefined) {
    return known;
  }

  const image = new Image();
  image.src = url;
  const result = image.decode().then(
    (): PictureSize | null => ({ width: image.naturalWidth, height: image.naturalHeight }),
    (): null => {
      // A failure is not remembered: the next ask tries again.
      decoded.delete(url);
      return null;
    },
  );
  decoded.set(url, result);
  return result;
}

/**
 * A layer for the picture and everything that hangs from it (markers, characters, the dot), and the pins that follow it.
 * The Mapa screen uses it; the other screens, whose pictures only fill their frame, use `useCover`.
 *
 * The database stores positions as a percentage of the image, and the game gives pixels of it. The layer is the picture
 * itself, scaled: `cover` (it fills the panel and overflows on one axis) or `contain` (all of it, with bars around). Every
 * pin is placed in the layer in pixels, so none of them changes when the layer is moved. The view is the layer's
 * translation (`offsetFor`): centred on a point and clamped at the edges of the picture when it overflows the panel,
 * centred when it does not. Sizes and translations are whole device pixels (`toDevice`).
 * Until the size of the picture is known, the layer is the whole panel and a pin falls back to a percentage of it.
 * A point given in pixels of the image (`pointOf`) has no such fallback: without the size it is not drawn.
 *
 * `box` is the element the layer sits in (its padding box is the area). `size` is the natural size of the picture shown,
 * which the screen gets by decoding it first (`decodePicture`) and hands over together with the picture, so the layer never
 * has a picture and the size of another. `contain` says whether the whole picture is wanted instead of the filled panel.
 */
/**
 * The window that follows the player does not shrink a picture past this. Asuka City (1280 x 896) has the panel's
 * proportions and would fit whole at under half its size, with nothing to drag; the whole picture is a button away.
 */
const MinFollowScale = 0.75;
/** How far a pinch brings a picture in: past this its pixels say nothing more, and the layer's texture is only bigger. */
const MaxScale = 1.5;
/** The scale the player pinched to; the same size of things on every map, while the app runs. */
const pinchedScale = ref<number | null>(null);

export function useMapLayer(
  box: Ref<HTMLElement | null>,
  size: () => PictureSize | null,
  contain: () => boolean,
) {
  const area = ref<PictureSize | null>(null);
  onMounted(() => {
    if (box.value) {
      area.value = { width: box.value.clientWidth, height: box.value.clientHeight };
    }
  });

  /** Scale at which the picture fills the panel (`cover`) or fits in it (`contain`); null until both sizes are known. */
  function scaleOf(fit: (a: number, b: number) => number): number | null {
    const natural = size();
    if (!natural || !area.value || area.value.width === 0 || area.value.height === 0) {
      return null;
    }

    return fit(area.value.width / natural.width, area.value.height / natural.height);
  }

  /** Scale of the window that follows the player: it fills the panel, and a big map is not shrunk past reading it. */
  function followScale(): number | null {
    const fit = scaleOf(Math.min);
    const scale = scaleOf(Math.max);
    if (fit === null || scale === null) {
      return null;
    }

    return Math.min(Math.max(pinchedScale.value ?? Math.max(scale, MinFollowScale), fit), MaxScale);
  }

  /** The scale the layer has now, or null until it can be told. */
  function currentScale(): number | null {
    return contain() ? scaleOf(Math.min) : followScale();
  }

  function pinchTo(scale: number): void {
    const fit = scaleOf(Math.min);
    pinchedScale.value = fit === null ? scale : Math.min(Math.max(scale, fit), MaxScale);
  }

  /** The picture scaled as the view wants it, or null until it can be told. */
  const layout = computed((): PictureSize | null => {
    const scale = currentScale();
    const natural = size();
    if (scale === null || !natural) {
      return null;
    }

    return {
      width: toDevice(natural.width * scale, true),
      height: toDevice(natural.height * scale, true),
    };
  });

  /** The picture as it is when all of it fits in the panel, its smallest: what is clear there is clear at any scale. */
  const wholeLayout = computed((): PictureSize | null => {
    const scale = scaleOf(Math.min);
    const natural = size();
    if (scale === null || !natural) {
      return null;
    }

    return { width: natural.width * scale, height: natural.height * scale };
  });

  /** How far the layer overflows the panel, on the axis that overflows most. */
  const overflow = computed((): number => {
    if (!layout.value || !area.value) {
      return 0;
    }

    return Math.max(layout.value.width - area.value.width, layout.value.height - area.value.height);
  });

  /** Whether the view shows less than the picture, enough for a drag to show something else. */
  const overflows = computed(() => overflow.value > NoPanPixels);

  /** Whether the picture, filling the panel, overflows it enough for a window that pans to show something else. */
  const canPan = computed((): boolean => {
    const scale = followScale();
    const natural = size();
    if (scale === null || !natural || !area.value) {
      return false;
    }

    const overflowX = natural.width * scale - area.value.width;
    const overflowY = natural.height * scale - area.value.height;
    return Math.max(overflowX, overflowY) > NoPanPixels;
  });

  /** One axis of the translation: centred when the picture is not longer than the panel, else on `focus`, clamped. */
  function axis(shown: number, view: number, focus: number | null): number {
    if (shown <= view) {
      return toDevice((view - shown) / 2);
    }

    const centre = focus ?? shown / 2;
    return -toDevice(Math.min(Math.max(centre - view / 2, 0), shown - view));
  }

  /**
   * Where the layer sits in the panel (a translation) when the view is centred on `focus`, a point of the layer. Without
   * focus, or when the picture fits the panel on an axis, that axis is centred.
   */
  function offsetFor(focus: Point | null): Point {
    if (!layout.value || !area.value) {
      return { x: 0, y: 0 };
    }

    return {
      x: axis(layout.value.width, area.value.width, focus?.x ?? null),
      y: axis(layout.value.height, area.value.height, focus?.y ?? null),
    };
  }

  /** The point of the layer a translation has at the centre of the panel (the inverse of `offsetFor`). */
  function centreAt(offset: Point): Point {
    return {
      x: (area.value?.width ?? 0) / 2 - offset.x,
      y: (area.value?.height ?? 0) / 2 - offset.y,
    };
  }

  /** A focus kept where the view can be centred: not beyond what shows the edge of the picture. */
  function clampFocus(focus: Point): Point {
    if (!layout.value || !area.value) {
      return focus;
    }

    const limit = (shown: number, view: number, value: number): number =>
      shown <= view ? shown / 2 : Math.min(Math.max(value, view / 2), shown - view / 2);
    return {
      x: limit(layout.value.width, area.value.width, focus.x),
      y: limit(layout.value.height, area.value.height, focus.y),
    };
  }

  /** Where the point `x`, `y` (percent of the image) lands inside the layer, or null until the size is known. */
  function project(x: number, y: number): Point | null {
    if (!layout.value) {
      return null;
    }

    return { x: (layout.value.width * x) / 100, y: (layout.value.height * y) / 100 };
  }

  /** Position inside the layer of the point `x`, `y` (percent of the image), for `left` and `top`. */
  function place(x: number, y: number): { left: string; top: string } {
    const spot = project(x, y);
    return spot === null
      ? { left: `${x}%`, top: `${y}%` }
      : { left: `${spot.x}px`, top: `${spot.y}px` };
  }

  /**
   * Position inside the layer of the pixel `x`, `y` of the image (its natural size).
   * Null when it cannot be told or must not be drawn: the size is not known, or the pixel is outside the image. A pin
   * like that is hidden, not pushed to the edge. What the view crops is not hidden: it is out of view, but still there.
   */
  function pointOf(x: number, y: number): Point | null {
    const natural = size();
    if (!natural) {
      return null;
    }

    const { width, height } = natural;
    if (!(x >= 0 && x < width && y >= 0 && y < height)) {
      return null;
    }

    const spot = project((x / width) * 100, (y / height) * 100);
    // Whole pixels, so a sprite drawn there keeps its pixels crisp.
    return spot === null ? null : { x: Math.round(spot.x), y: Math.round(spot.y) };
  }

  return {
    currentScale,
    pinchTo,
    place,
    pointOf,
    offsetFor,
    centreAt,
    clampFocus,
    layout,
    wholeLayout,
    canPan,
    overflows,
    area,
  };
}
