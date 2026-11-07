<script setup lang="ts">
import { useTexts } from "../texts";
import { placeWord } from "../place-words";
import { computed, onBeforeUnmount, ref, shallowRef, watch, watchEffect } from "vue";
import { useI18n } from "vue-i18n";
import FieldActor from "../components/FieldActor.vue";
import { type PanelAnchor } from "../components/FloatingPanel.vue";
import LabelLeader from "../components/LabelLeader.vue";
import MapFilters from "../components/MapFilters.vue";
import MapImage from "../components/MapImage.vue";
import StatusBar from "../components/StatusBar.vue";
import { genericNpcUrl, iconUrl } from "../assets";
import { buildMapSheet, type MapMarker } from "../map-sheet";
import {
  labelBox,
  leaderDots,
  nudgeLabels,
  pictureBox,
  type Box,
  type LabelInput,
  type LabelShift,
} from "../actor-labels";
import { liveActorsOf, type LiveActor } from "../field-actor-view";
import { ActorNames, type ActorRole } from "../field-actors";
import { MapExits, type MapExit } from "../map-exits";
import { useMapOptions } from "../map-options";
import { isScene, mapKindOf, mapName, originalOf, placeOf, shownPlaceOf } from "../map-view";
import { nextPlacesTowards } from "../quest-paths";
import { questKindIcon } from "../quest-offers";
import { useScreen } from "../router";
import { decodePicture, toDevice, useMapLayer, useShowAll, type PictureSize } from "../use-cover";
import { useFollow, type Point } from "../use-follow";
import { usePanelHost } from "../use-panel-host";
import { useGameState } from "@/composables/use-game-state";
import { useGameStore } from "@/stores/use-game-store";

const { t } = useI18n();
const texts = useTexts();
const { go, back, param } = useScreen();
const gameState = useGameState();
const store = useGameStore();

const mapId = computed(() => gameState.value.player.mapId);
/** A place the Wiki or a link asked to see, by id; null shows the place the player is in. */
const askedPlace = computed(() => (typeof param.value === "string" ? param.value : null));
const liveSheet = computed(() =>
  buildMapSheet(gameState.value, texts, askedPlace.value ?? undefined, store.questOffers),
);

/**
 * What is drawn: `liveSheet`, except that when the place changes it keeps the old one until the picture of the new place
 * is loaded and decoded, and then swaps all at once (the picture, its size, the markers, the title). Without this, the new
 * data came first and was drawn for a few frames over the old picture, stretched to the panel, with no dot.
 */
const sheet = shallowRef(liveSheet.value);
/** Natural size of the picture of `sheet`, once it is known. */
const size = shallowRef<PictureSize | null>(null);
/** The picture of `sheet`; undefined before the first one. */
const shownUrl = shallowRef<string | null | undefined>(undefined);
/** Whether the game is changing field: what the core says of the player and the characters is not of a place. */
const leaving = computed(() => askedPlace.value === null && store.fieldChange !== null);
/** The game is in a menu or a shop opened over the place: the field is closed, and the map stays as it was left. */
const overScene = computed(() => askedPlace.value === null && isScene(mapId.value));
/**
 * Whether what is live (the player, the characters) is not of the picture drawn. A computed, not set by a watcher, so it is
 * already true for the sync effect that reads the core's next reading.
 */
const swapping = computed(
  () => leaving.value || overScene.value || liveSheet.value.imageUrl !== shownUrl.value,
);
let swapToken = 0;

/** A place drawn on this screen, as far as the game has said where it goes: not a battle, a whole-screen scene and the like. */
function goesToPlace(to: string | null): boolean {
  return to === null || (!isScene(to) && mapKindOf(placeOf(to)) === "mapa");
}

/**
 * Whether the panel is under the fade that goes from one place to the next, the game's own black: from when the core says the
 * game starts to close the field, to the frame with the new place in it.
 */
const fading = ref(leaving.value);
/** Each fade that ended: what glides starts on its point, wherever it was. */
const fades = ref(0);
watch(leaving, (isLeaving) => {
  if (isLeaving && goesToPlace(store.fieldChange?.to ?? null)) {
    fading.value = true;
  }
});
watch(
  () => store.fieldChange?.to,
  (to) => {
    if (leaving.value && to !== undefined && !goesToPlace(to)) {
      fading.value = false;
    }
  },
);

watch(
  [liveSheet, leaving],
  async ([next, isLeaving]) => {
    if (isLeaving) {
      return;
    }

    const mine = ++swapToken;
    if (next.imageUrl !== shownUrl.value) {
      const decoded = next.imageUrl === null ? null : await decodePicture(next.imageUrl);
      if (mine !== swapToken) {
        return;
      }

      size.value = decoded;
    }

    sheet.value = next;
    shownUrl.value = next.imageUrl;
    if (fading.value) {
      fades.value++;
      // After the frame with the new place in it, so the fade opens on it.
      requestAnimationFrame(() => {
        if (!leaving.value) {
          fading.value = false;
        }
      });
    }
  },
  { immediate: true },
);

const title = computed(() => sheet.value.name ?? t("handheld.map.unknown"));

/** What the player chose to see over the picture (the filters have the switches). */
const {
  exits: showExits,
  objectives: showObjectives,
  characters: showCharacters,
  roles,
} = useMapOptions();

/** The filters' panel hangs from the button that opened it; null while it is closed. */
const filtersAnchor = ref<PanelAnchor | null>(null);

function toggleFilters(event: MouseEvent): void {
  filtersAnchor.value = filtersAnchor.value === null ? anchorOf(event.currentTarget) : null;
}

/** What a marker of the data is, for the filters. */
const markerRoles: Record<MapMarker["kind"], ActorRole> = {
  npc: "story",
  boss: "battle",
  cardShop: "shop",
};

/** Whether the game has a character in the place drawn that the talk line `giver` names: then the map shows them, not a point of the data. */
function hasCharacterOf(giver: string): boolean {
  return Object.values(ActorNames[sheet.value.placeId] ?? {}).includes(giver);
}

/** The markers drawn: what the filters leave, and of what a quest offers only what no character of the game stands for. */
const markers = computed(() =>
  sheet.value.markers.filter((marker) => {
    if (marker.offer?.giver && hasCharacterOf(marker.offer.giver)) {
      return false;
    }

    return marker.isObjective
      ? showObjectives.value
      : (marker.kind !== "npc" || showCharacters.value) && roles[markerRoles[marker.kind]].value;
  }),
);

/** The desert button is only for when the player is in the desert: anywhere else it has nothing of theirs to show. */
const inDesert = computed(() => mapKindOf(placeOf(mapId.value)) === "mobius");

/**
 * Whether there is a player to follow: in their own place, not over a whole-screen scene, and with the game having them
 * on the field (the core sends (0, 0) when it does not: a battle, the title). Without one the view has nothing to
 * pan to, so it shows the whole picture: the Wiki's view of a place, a scene.
 */
const followable = computed(() => {
  if (swapping.value) {
    // On its way to the new place the core has no position: the old view stays as it is, not the whole picture.
    return heldFollowable;
  }

  const { positionX, positionY } = gameState.value.player;
  heldFollowable =
    askedPlace.value === null &&
    !isScene(sheet.value.mapId) &&
    !(positionX === 0 && positionY === 0);
  return heldFollowable;
});
let heldFollowable = false;

const { host: stage, hostSize: stageSize, anchorOf } = usePanelHost();
/** The player's choice (kept while the app runs) to see the whole picture instead of a window that follows them. */
const showAll = useShowAll();
/** The Wiki's view of a place has its own choice: it opens on the whole picture. */
const AskedStartsWhole = true;
const askedAll = ref(AskedStartsWhole);
const {
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
  currentScale,
  pinchTo,
} = useMapLayer(
  stage,
  () => size.value,
  () => contain.value,
);
/** A picture that needs no pan has no use for the button, and is always drawn filling the panel. */
const canToggle = computed(() => (followable.value || askedPlace.value !== null) && canPan.value);
/**
 * The whole picture fits in the panel, with a flat fill around it; otherwise it fills the panel, and follows the player
 * when there is one. A whole-screen scene, or a place the game has no player in, is always whole.
 */
const contain = computed(() => {
  if (askedPlace.value !== null) {
    return askedAll.value && canPan.value;
  }

  return !followable.value || (showAll.value && canPan.value);
});

function toggleAll(): void {
  if (askedPlace.value !== null) {
    askedAll.value = !askedAll.value;
  } else {
    showAll.value = !showAll.value;
  }
}

function markerStyle(marker: MapMarker): Record<string, string> {
  const spot = place(marker.x, marker.y);
  // The sprite stands on the point; the box is centred on it, as the design draws it.
  return { left: `calc(${spot.left} - 44px)`, top: `calc(${spot.top} - 22px)` };
}

/**
 * The core sends the position about every 100 ms. The dot slides between two of them, but a step that no walk makes
 * (a warp, a launch, a new map whose position arrives before its picture) must not be crossed: it is a new dot. About
 * 24 px is what a walk takes in 100 ms (4 px a frame), so this lets pass four times that.
 */
const JumpPixels = 96;
const jumps = ref(0);
watch(
  () => [gameState.value.player.positionX, gameState.value.player.positionY],
  ([x = 0, y = 0], [beforeX = 0, beforeY = 0]) => {
    if (Math.abs(x - beforeX) > JumpPixels || Math.abs(y - beforeY) > JumpPixels) {
      jumps.value++;
    }
  },
);

/**
 * Where the player stands on the layer of the picture. Only in the player's own place, and not over a whole-screen
 * scene. The game gives pixels of the picture; outside the picture there is no marker.
 */
const playerSpot = computed((): Point | null => {
  if (swapping.value) {
    // The picture of the new place is still on its way: the dot stays where it is, so the old view does not change (it
    // would jump to the centre and back) until the whole of the new one is swapped in at once.
    return heldSpot;
  }

  const { positionX, positionY } = gameState.value.player;
  heldSpot = followable.value ? pointOf(positionX, positionY) : null;
  return heldSpot;
});
let heldSpot: Point | null = null;

/**
 * The scale of the layer, which changes with the view (the whole picture or the window that follows) and with the panel.
 * Everything that glides to a point must start on it again when this changes: its target moved because the picture did,
 * not because it walked.
 */
const viewKey = computed(() =>
  layout.value ? `${layout.value.width}x${layout.value.height}` : "",
);

const playerPoint = useFollow(
  () => playerSpot.value,
  () => `${sheet.value.imageUrl}-${jumps.value}-${viewKey.value}-${fades.value}`,
);

/**
 * The layer that holds the picture and all that hangs from it, and the dot. `playerPoint` changes every frame while the
 * player walks, so the template must not read it (the whole screen would patch at the screen's rate): it only asks whether
 * there is a dot, and these two elements are moved from here, in the same step the point changes, so the map and the dot
 * never disagree by a frame. A jump of the point (a warp, another picture) moves both at once, as the point does.
 */
const hasPlayer = computed(() => playerPoint.value !== null);
const world = ref<HTMLElement | null>(null);
const dot = ref<HTMLElement | null>(null);
/** The dot is a 22 px square centred on the point. */
const DotHalf = 11;
/** Where the player looks, a point of the layer at the centre of the panel, while they drag the map; null follows. */
const look = ref<Point | null>(null);
const looking = computed(() => look.value !== null);
/** The translation the layer has now, to start a drag from where the view is. */
let applied: Point = { x: 0, y: 0 };
watchEffect(
  () => {
    const point = playerPoint.value;
    const focus = contain.value ? null : (look.value ?? point);
    const offset = offsetFor(focus);
    applied = offset;
    if (world.value) {
      world.value.style.transform = `translate3d(${offset.x}px, ${offset.y}px, 0)`;
    }
    if (dot.value && point) {
      // Whole device pixels, as the layer is: a sprite between two of them is resampled and shimmers as it moves.
      dot.value.style.transform = `translate3d(${toDevice(point.x - DotHalf)}px, ${toDevice(point.y - DotHalf)}px, 0)`;
    }
  },
  { flush: "sync" },
);

// A new place, or the other view, starts again on the player (or the centre).
watch([() => sheet.value.imageUrl, contain], () => {
  look.value = null;
});

/**
 * Dragging the map. A touch that moves more than `DragPixels` pans the view and stops it following; the touch that does
 * not move is a tap on whatever is under it (a marker, a character), and a drag never ends in one: its click is swallowed.
 * The view goes back to the player with their next movement, once `ReleaseMs` have passed since the last touch, or with the
 * button.
 */
const DragPixels = 8;
const ReleaseMs = 4000;
let touch: { id: number; x: number; y: number; from: Point; moved: boolean } | null = null;
/** The fingers on the map, and the pinch when there are two. */
const pointers = new Map<number, Point>();
let pinch: { distance: number; scale: number; look: Point | null } | null = null;
let pinchFrame: number | null = null;
let lastTouch = 0;
let swallowClick = false;

function onPointerDown(event: PointerEvent): void {
  // The first finger of a touch: nothing else is down, whatever was seen to leave or not, and a pinch that ended left no click.
  if (event.isPrimary) {
    pointers.clear();
    swallowClick = false;
  }
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
  if (pointers.size === 2) {
    startPinch();
    return;
  }

  if (!overflows.value || touch !== null || (event.pointerType === "mouse" && event.button !== 0)) {
    return;
  }

  touch = {
    id: event.pointerId,
    x: event.clientX,
    y: event.clientY,
    from: look.value ?? centreAt(applied),
    moved: false,
  };
}

function fingersApart(): number {
  const [first, second] = [...pointers.values()];
  return first && second ? Math.hypot(first.x - second.x, first.y - second.y) : 0;
}

/** Two fingers: the map scales around the middle of the view, which stays on the player while it follows them. */
function startPinch(): void {
  const scale = currentScale();
  if (scale === null) {
    return;
  }

  touch = null;
  swallowClick = true;
  // The whole picture is left for a window of the same size, which the fingers then change.
  if (contain.value) {
    if (askedPlace.value !== null) {
      askedAll.value = false;
    } else {
      showAll.value = false;
    }
  }
  pinchTo(scale);
  pinch = { distance: Math.max(1, fingersApart()), scale, look: look.value };
  for (const id of pointers.keys()) {
    world.value?.setPointerCapture(id);
  }
}

/** Once a frame: each scale lays out the map and the names over it again. */
function applyPinch(): void {
  pinchFrame = null;
  if (pinch === null) {
    return;
  }

  const scale = (pinch.scale * fingersApart()) / pinch.distance;
  pinchTo(scale);
  const reached = currentScale() ?? scale;
  if (pinch.look !== null) {
    const ratio = reached / pinch.scale;
    look.value = clampFocus({ x: pinch.look.x * ratio, y: pinch.look.y * ratio });
  }
}

function onPointerMove(event: PointerEvent): void {
  if (pointers.has(event.pointerId)) {
    pointers.set(event.pointerId, { x: event.clientX, y: event.clientY });
  }

  if (pinch !== null) {
    pinchFrame ??= requestAnimationFrame(applyPinch);
    lastTouch = performance.now();
    return;
  }

  if (touch === null || event.pointerId !== touch.id) {
    return;
  }

  const dx = event.clientX - touch.x;
  const dy = event.clientY - touch.y;
  if (!touch.moved) {
    if (Math.hypot(dx, dy) < DragPixels) {
      return;
    }

    touch.moved = true;
    swallowClick = true;
    world.value?.setPointerCapture(event.pointerId);
  }

  look.value = clampFocus({ x: touch.from.x - dx, y: touch.from.y - dy });
  lastTouch = performance.now();
}

function onPointerUp(event: PointerEvent): void {
  pointers.delete(event.pointerId);
  if (pointers.size < 2) {
    pinch = null;
  }

  if (touch !== null && event.pointerId === touch.id) {
    lastTouch = performance.now();
    touch = null;
  }
}

function onClickCapture(event: MouseEvent): void {
  if (swallowClick) {
    swallowClick = false;
    event.stopPropagation();
    event.preventDefault();
  }
}

watch(
  () => playerSpot.value,
  () => {
    if (look.value !== null && touch === null && performance.now() - lastTouch >= ReleaseMs) {
      look.value = null;
    }
  },
);

/**
 * The dot blinks every half second by swapping a class from here. A CSS animation of `filter` kept the compositor drawing
 * at the screen's rate for as long as the screen was open, even with the player standing still (measured in Chrome: 180
 * frames in 3 s against none), with the big map layer under it.
 */
const blink = window.setInterval(() => {
  dot.value?.classList.toggle("lit");
  world.value?.classList.toggle("pulse");
}, 500);
onBeforeUnmount(() => window.clearInterval(blink));

// On the window: a finger lifted off the layer before it counted as a drag, over the bars of a map narrower than the panel,
// never reaches the layer, and the drag would stay open.
window.addEventListener("pointerup", onPointerUp);
window.addEventListener("pointercancel", onPointerUp);
onBeforeUnmount(() => {
  window.removeEventListener("pointerup", onPointerUp);
  window.removeEventListener("pointercancel", onPointerUp);
});

/** The layer has the size of the scaled picture; until it is known, that of the panel. */
const worldSize = computed(() =>
  layout.value
    ? { width: `${layout.value.width}px`, height: `${layout.value.height}px` }
    : { width: "100%", height: "100%" },
);

/** How near a quest's point a character stands to be who the quest is about: the data's points are rounded, by hand. */
const ObjectiveReach = 40;

/** The filters never hide who a quest under way asks for. */
function standsOnObjective(actor: LiveActor): boolean {
  const spot = pointOf(actor.x, actor.y);
  return (
    showObjectives.value &&
    spot !== null &&
    sheet.value.markers.some((marker) => {
      if (!marker.isObjective || marker.offer !== null) {
        return false;
      }

      const point = place(marker.x, marker.y);
      return (
        Math.hypot(parseFloat(point.left) - spot.x, parseFloat(point.top) - spot.y) <=
        ObjectiveReach
      );
    })
  );
}

/**
 * The characters the game has on the field right now, at their real points: only in the player's own place, and not
 * over a whole-screen scene, like the dot. The Wiki's view of another place has no live data.
 */
const liveActors = computed((): LiveActor[] => {
  if (swapping.value) {
    // Held while the picture of the new place loads, like the dot: nothing moves until everything swaps at once.
    return heldActors;
  }

  heldActors = charactersHere.value.filter(
    (actor) =>
      (showCharacters.value && roles[actor.role].value) ||
      standsOnObjective(actor) ||
      actorOffers.value.has(actor.key),
  );
  return heldActors;
});
let heldActors: LiveActor[] = [];

/**
 * The names are worked out once for a place, with the characters where they stood on arriving: one that walks takes its
 * name along. The full cycle reads them every 500 ms, so after this two readings have passed, and none is left of the place
 * before. A character that comes later counts from where it came.
 */
const SettleMs = 1000;
let settled: { place: string; since: number; spots: Map<string, { x: number; y: number }> } | null =
  null;

/** The characters of the place, where the game has them. */
const charactersHere = computed((): LiveActor[] => {
  if (swapping.value || askedPlace.value !== null || isScene(sheet.value.mapId)) {
    return [];
  }

  const place = shownPlaceOf(mapId.value);
  const now = performance.now();
  if (settled?.place !== place) {
    settled = { place, since: now, spots: new Map() };
  }

  const known = settled;
  const fresh = now - known.since < SettleMs;
  const actors = liveActorsOf(store.fieldActors, place);
  known.spots = new Map(
    actors.map((actor) => [
      actor.key,
      fresh
        ? { x: actor.x, y: actor.y }
        : (known.spots.get(actor.key) ?? { x: actor.x, y: actor.y }),
    ]),
  );
  return actors;
});

/** Where a character stood on arriving, which is what the names keep clear of. */
function settledSpot(actor: LiveActor): Point | null {
  const spot = settled?.spots.get(actor.key);
  return spot === undefined ? actorSpot(actor) : pointOf(spot.x, spot.y);
}

/** Who offers a quest, of the characters here: the one the giver's talk line names nearest to where the quest says. */
const actorOffers = computed(() => {
  const found = new Map<string, MapMarker>();
  if (!showObjectives.value) {
    return found;
  }

  for (const marker of sheet.value.markers) {
    if (!marker.offer?.giver) {
      continue;
    }

    const giver = marker.offer.giver;
    const point = place(marker.x, marker.y);
    const distance = (actor: LiveActor): number => {
      const spot = actorSpot(actor);
      return spot === null
        ? Infinity
        : Math.hypot(parseFloat(point.left) - spot.x, parseFloat(point.top) - spot.y);
    };
    const nearest = charactersHere.value
      .filter((actor) => actor.speaker === giver)
      .sort((left, right) => distance(left) - distance(right))[0];
    if (nearest !== undefined) {
      found.set(nearest.key, marker);
    }
  }

  return found;
});

function actorBadge(actor: LiveActor): string | null {
  const offer = actorOffers.value.get(actor.key)?.offer;
  return offer ? questKindIcon(offer.kind) : null;
}

/** What tapping a character opens: the quest they offer, or their sheet of the Wiki. */
function actorTarget(actor: LiveActor): MapMarker["target"] | null {
  return actorOffers.value.get(actor.key)?.target ?? actor.target;
}

/** Where the character's feet are on the layer of the map, or null when they are outside the picture. */
function actorSpot(actor: LiveActor): Point | null {
  return pointOf(actor.x, actor.y);
}

/** Where the picture of a marker sits against its point (about the size of a sprite of the game), and its label. */
const MarkerPicture = [-20, -22, 40, 38] as const;
const MarkerLabelTop = 17;
/** An exit of the place as drawn: where it is on the layer, from the data of the game (`map-exits.ts`). */
interface PlacedExit {
  key: string;
  x: number;
  y: number;
  name: string;
  to: string;
  /** A quest under way asks to go there. */
  hasQuest: boolean;
}

/**
 * The exits to blink: the way to what the quests under way ask for, however far, and a place next door where one can be
 * started. Those are many and all over the map, and the way to each would blink every exit.
 */
const questPlaces = computed(() =>
  !showObjectives.value
    ? new Set<string>()
    : new Set([
        ...nextPlacesTowards(sheet.value.mapId, sheet.value.questPlaces),
        ...sheet.value.offerPlaces.map(originalOf),
      ]),
);

const placedExits = computed((): PlacedExit[] => {
  if (!showExits.value || isScene(sheet.value.mapId)) {
    return [];
  }

  return (MapExits[sheet.value.placeId] ?? []).flatMap((exit: MapExit): PlacedExit[] => {
    const spot = pointOf(exit.x, exit.y);
    return spot === null
      ? []
      : [
          {
            key: `${exit.to}:${exit.x}:${exit.y}`,
            ...spot,
            // The name of the place it leads to, as the map is named; a question mark until the disc has been read.
            name: mapName(texts, exit.to) ?? "?",
            to: exit.to,
            hasQuest: questPlaces.value.has(originalOf(exit.to)),
          },
        ];
  });
});

/** The picture of an exit, a 22 px dot centred on its point, and its label below (a plate: letters of 10 px with 4 px each side). */
const ExitPicture = [-11, -11, 22, 22] as const;
const ExitLabelTop = 10;
/** Names up to this long stay on one line. */
const ExitLineLength = 12;

/** A long name in two lines, split at the space that leaves them most even. */
function exitLines(name: string): string[] {
  if (name.length <= ExitLineLength) {
    return [name];
  }

  const words = name.split(" ");
  const splits = words
    .slice(1)
    .map((_, index) => [words.slice(0, index + 1).join(" "), words.slice(index + 1).join(" ")]);
  const longest = (lines: string[]) => Math.max(...lines.map((line) => line.length));
  return splits.reduce<string[]>(
    (best, lines) => (longest(lines) < longest(best) ? lines : best),
    [name],
  );
}

function exitInput(exit: PlacedExit): LabelInput {
  const lines = exitLines(exit.name);
  return {
    key: `exit:${exit.key}`,
    x: exit.x,
    y: exit.y,
    frame: ExitPicture,
    name: exit.name,
    size: {
      width: Math.max(...lines.map((line) => line.length)) * 10 + 16,
      height: lines.length * 12 + 8,
    },
    top: ExitLabelTop,
  };
}

/** A marker as `nudgeLabels` sees it, or null while its point is not on the layer. */
function markerInput(marker: MapMarker): LabelInput | null {
  const spot = place(marker.x, marker.y);
  if (!spot.left.endsWith("px")) {
    return null;
  }

  return {
    key: `marker:${marker.key}`,
    x: parseFloat(spot.left),
    y: parseFloat(spot.top),
    frame: MarkerPicture,
    name: marker.label,
    // The plate: letters of 10 px, 4 px of margin and 4 px of frame on each side, and a line of 10 px.
    size: { width: marker.label.length * 10 + 16, height: 20 },
    top: MarkerLabelTop,
  };
}

/** The buttons of the dock: 44 px each with 4 px between. */
const buttonCount = computed(() => 1 + (inDesert.value ? 1 : 0) + (canToggle.value ? 1 : 0));
const toolsWidth = computed(() => buttonCount.value * 44 + (buttonCount.value - 1) * 4);

/**
 * Where each name moves so it stays legible: no name over another name or over another character. A simple rule (see
 * `nudgeLabels`), worked out once for what is on the map: only what is drawn changes it, not where the view is, not the
 * player, and not a pinch. It is worked out with the whole picture in the panel, the smallest it is ever drawn, where
 * things are closest: at any other scale they are further apart, so it holds. The title and the buttons are fixed in the
 * panel; they are taken into account there, which is where the layer stays still under them.
 */
let shiftsFor = "";
let shiftsDone = new Map<string, LabelShift>();
const labelShifts = computed(() => {
  const box = layout.value;
  const whole = wholeLayout.value;
  const panel = area.value;
  if (box === null || whole === null || panel === null) {
    return new Map<string, LabelShift>();
  }

  const key = JSON.stringify([
    sheet.value.placeId,
    whole,
    panel,
    toolsWidth.value,
    title.value,
    askedPlace.value,
    // Where they stood on arriving counts, but not where they walk to afterwards.
    liveActors.value.map((actor) => [actor.key, actor.name, settledSpot(actor)]),
    markers.value.map((marker) => [marker.key, marker.label]),
    placedExits.value.map((exit) => [exit.key, exit.name]),
  ]);
  if (key === shiftsFor) {
    return shiftsDone;
  }

  // A box of the panel, in the pixels of the layer when all of it fits, centred.
  const shiftX = (panel.width - whole.width) / 2;
  const shiftY = (panel.height - whole.height) / 2;
  const inLayer = (left: number, top: number, right: number, bottom: number): Box => ({
    left: left - shiftX,
    top: top - shiftY,
    right: right - shiftX,
    bottom: bottom - shiftY,
  });
  const obstacles: Box[] = [
    inLayer(
      panel.width - 4 - toolsWidth.value,
      panel.height - 4 - 44,
      panel.width - 4,
      panel.height - 4,
    ),
  ];
  if (sheet.value.name) {
    // The title plate: letters of 12 px with 16 px of margin and frame on each side, 34 px high, centred at the top.
    const width = title.value.length * 12 + 32;
    obstacles.push(inLayer((panel.width - width) / 2, 4, (panel.width + width) / 2, 4 + 34));
  }
  if (askedPlace.value !== null) {
    obstacles.push(inLayer(4, 4, 4 + 44, 4 + 44));
  }

  const ratio = whole.width / box.width;
  const atWhole = (input: LabelInput): LabelInput => ({
    ...input,
    x: input.x * ratio,
    y: input.y * ratio,
  });

  const inputs: LabelInput[] = [];
  for (const actor of liveActors.value) {
    const spot = settledSpot(actor);
    if (spot !== null && actor.name !== null) {
      inputs.push(
        atWhole({ key: actor.key, x: spot.x, y: spot.y, frame: actor.frame, name: actor.name }),
      );
    }
  }
  // The markers of the place: the box of a marker is centred on its point (88 px wide, 38 px of picture over the label).
  for (const marker of markers.value) {
    const input = markerInput(marker);
    if (input !== null) {
      inputs.push(atWhole(input));
    }
  }

  // The exits come first: they are few and long-named, and they are where the player is headed, so they take their spot
  // and the names of the characters and markers move around them, never the other way. An exit's label keeps clear of
  // the pictures of the characters and markers (it is placed with them in mind), and what is left of the names goes
  // around the exit's picture and label.
  const exitInputs = placedExits.value.map(exitInput).map(atWhole);
  const shifts = nudgeLabels(exitInputs, whole, [...obstacles, ...inputs.map(pictureBox)]);
  const taken: Box[] = exitInputs.flatMap((input) => [
    pictureBox(input),
    labelBox(input, shifts.get(input.key) ?? { x: 0, y: 0 }),
  ]);
  for (const [name, shift] of nudgeLabels(inputs, whole, [...obstacles, ...taken])) {
    shifts.set(name, shift);
  }

  shiftsFor = key;
  shiftsDone = shifts;
  return shifts;
});

/** The sprite of the marker, the kit icon of its kind when the game has no picture, or the signpost of a quest waypoint. */
function markerImage(marker: MapMarker): string {
  if (marker.offer) {
    return questKindIcon(marker.offer.kind);
  }

  if (marker.isWaypoint) {
    return iconUrl("signpost");
  }

  if (marker.imageUrl !== null) {
    return marker.imageUrl;
  }

  const icons = { boss: "boss", cardShop: "shop" } as const;
  return marker.kind === "npc" ? genericNpcUrl() : iconUrl(icons[marker.kind]);
}

/** Moves the label of an exit as `labelShifts` says: from under its point, centred. */
function exitShift(exit: PlacedExit): Record<string, string> | undefined {
  const shift = labelShifts.value.get(`exit:${exit.key}`);
  return shift ? { transform: `translate(calc(-50% + ${shift.x}px), ${shift.y}px)` } : undefined;
}

/** Moves the label of a marker as `labelShifts` says. */
function markerShift(marker: MapMarker): Record<string, string> | undefined {
  const shift = labelShifts.value.get(`marker:${marker.key}`);
  return shift ? { transform: `translate(${shift.x}px, ${shift.y}px)` } : undefined;
}

/** The colour of the plate of each kind of name, for the line that ties it to its point. */
const ExitLeaderColor = "#f0b020";
const ObjectiveLeaderColor = "#f0b020";
const MarkerLeaderColor = "#7cc0ff";
/** The dot of an exit does not fill its box: its line starts inside the box. */
const ExitDotInset = 3;

/** The dots of the line from an exit's point to its label, when the label was moved away from it. */
function exitLeader(exit: PlacedExit): { x: number; y: number }[] | null {
  const shift = labelShifts.value.get(`exit:${exit.key}`);
  return shift ? leaderDots(exitInput(exit), shift, ExitDotInset) : null;
}

/** The same for a marker, from the centre of its box (the point is 44 px in and 22 px down, see `markerStyle`). */
function markerLeader(marker: MapMarker): { x: number; y: number }[] | null {
  const shift = labelShifts.value.get(`marker:${marker.key}`);
  const input = markerInput(marker);
  return shift && input ? leaderDots(input, shift) : null;
}

function openActor(actor: LiveActor): void {
  const target = actorTarget(actor);
  if (target !== null) {
    go(target.screen, target.param);
  }
}

function openMarker(marker: MapMarker): void {
  go(marker.target.screen, marker.target.param);
}
</script>

<template>
  <div class="view">
    <section class="mapa">
      <div ref="stage" class="win stage">
        <!-- The picture and all that hangs from it are one layer, moved from the script (`world`). The title, the back
             button and the dock stay where they are, over it. -->
        <div
          ref="world"
          class="world"
          :style="worldSize"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @click.capture="onClickCapture"
        >
          <MapImage
            :src="sheet.imageUrl"
            :alt="title"
            :detail="sheet.mapId"
            :whole="contain"
            sync
          />

          <!-- The exits: a door on the point, with the name of where it leads, under it. The layer holds them still, as it
               does the names of the characters. A tap opens the place in the Wiki's view. -->
          <button
            v-for="exit in placedExits"
            :key="exit.key"
            type="button"
            class="exit"
            :class="{ 'has-quest': exit.hasQuest }"
            :style="{ left: `${exit.x}px`, top: `${exit.y}px` }"
            :aria-label="exit.name"
            @click="go('mapa', exit.to)"
          >
            <LabelLeader
              v-if="exitLeader(exit)"
              :dots="exitLeader(exit)!"
              :color="ExitLeaderColor"
            />
            <img :src="iconUrl('exit')" alt="" class="exit-icon" />
            <span class="exit-label thin-plate" :style="exitShift(exit)"
              ><span v-for="line in exitLines(exit.name)" :key="line" class="exit-line">{{
                line
              }}</span></span
            >
          </button>

          <button
            v-for="marker in markers"
            :key="marker.key"
            type="button"
            class="marker"
            :style="markerStyle(marker)"
            :aria-label="
              marker.isObjective
                ? t('handheld.map.objective', { name: marker.label })
                : marker.label
            "
            @click="openMarker(marker)"
          >
            <!-- The box of a marker is centred on its point, 44 px in and 22 px down. -->
            <span v-if="markerLeader(marker)" class="marker-leader">
              <LabelLeader
                :dots="markerLeader(marker)!"
                :color="marker.isObjective ? ObjectiveLeaderColor : MarkerLeaderColor"
              />
            </span>
            <img
              v-if="marker.isObjective && !marker.isWaypoint"
              :src="iconUrl('signpost')"
              alt=""
              class="objective"
            />
            <span class="sprite-box">
              <img :src="markerImage(marker)" alt="" class="sprite" />
            </span>
            <span
              class="marker-label"
              :class="marker.isObjective ? 'thin-plate gold' : 'thin ice'"
              :style="markerShift(marker)"
            >
              {{ marker.label }}
            </span>
          </button>

          <!-- The game's own characters, in the order the game draws them (by y), over the static markers and under the
               dot. A wrapper of its own keeps their z-indexes inside it. -->
          <div class="actors">
            <FieldActor
              v-for="actor in liveActors"
              :key="actor.key"
              :spot="actorSpot(actor)"
              :snap="`${sheet.imageUrl ?? ''}-${viewKey}`"
              :src="actor.src"
              :frame="actor.frame"
              :name="actor.name"
              :role="actor.role"
              :label-shift="labelShifts.get(actor.key) ?? null"
              :badge="actorBadge(actor)"
              :tappable="actorTarget(actor) !== null"
              @open="openActor(actor)"
            />
          </div>

          <!-- After the markers, so it is drawn over them. A new key per picture, and a dot that shows again is a new
               one, so it never slides from where it was: across maps or out of hiding (a warp inside a picture is
               `useFollow`'s own snap). The script moves it. -->
          <img
            v-if="hasPlayer"
            ref="dot"
            :key="sheet.imageUrl ?? ''"
            :src="iconUrl('here')"
            :alt="t('handheld.map.here')"
            class="player"
          />
        </div>

        <!-- The game's own screen goes black between two places: this does the same over the map, under what is not of a place. -->
        <div class="fade" :class="{ on: fading }"></div>

        <div v-if="sheet.name" class="plate title">{{ title }}</div>

        <button
          v-if="askedPlace !== null"
          type="button"
          class="btn back"
          :aria-label="t('handheld.back')"
          @click="back()"
        >
          <img :src="iconUrl('back')" alt="" class="icon" />
        </button>

        <div class="dock">
          <div class="buttons">
            <!-- Back to the player, after the map was dragged somewhere else. -->
            <button
              v-if="looking && followable"
              type="button"
              class="btn tool"
              :aria-label="t('handheld.map.follow')"
              @click="look = null"
            >
              <img :src="iconUrl('here')" alt="" class="icon" />
            </button>
            <!-- What is drawn over the picture. -->
            <button
              type="button"
              class="tool"
              :class="filtersAnchor ? 'btn-on' : 'btn'"
              data-panel-anchor
              :aria-pressed="filtersAnchor !== null"
              :aria-label="t('handheld.mapFilters.title')"
              @click="toggleFilters"
            >
              <img :src="iconUrl('filter')" alt="" class="icon" />
            </button>
            <!-- Follow the player or see all of the picture. Lit while the whole picture is shown. -->
            <button
              v-if="canToggle"
              type="button"
              class="tool"
              :class="contain ? 'btn-on' : 'btn'"
              :aria-pressed="contain"
              :aria-label="t('handheld.map.whole')"
              @click="toggleAll()"
            >
              <img :src="iconUrl('map')" alt="" class="icon" />
            </button>
            <button
              v-if="inDesert"
              type="button"
              class="btn tool"
              :aria-label="placeWord(texts, 'mobiusDesert')"
              @click="go('mobius')"
            >
              <img :src="iconUrl('desert')" alt="" class="icon" />
            </button>
          </div>
        </div>

        <MapFilters
          v-if="filtersAnchor"
          :anchor="filtersAnchor"
          :host-width="stageSize.width"
          :host-height="stageSize.height"
          @close="filtersAnchor = null"
        />
      </div>
    </section>

    <StatusBar />
  </div>
</template>

<style scoped>
.mapa {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.stage {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* In steps, as the game's own fade is seen. It closes as the game's view shrinks, and opens with the new field. */
.fade {
  position: absolute;
  inset: 0;
  background: #000;
  opacity: 0;
  pointer-events: none;
  transition: opacity 200ms steps(5);
}

.fade.on {
  opacity: 1;
  transition: opacity 400ms steps(6);
}

/* The picture and what hangs from it. Its size is the scaled picture's; `world` moves it, so it gets a layer of its own. */
.world {
  position: absolute;
  left: 0;
  top: 0;
  will-change: transform;
}

/* The layer takes every touch for itself: nothing around it scrolls, and two fingers scale it even when it all fits. */
.world {
  touch-action: none;
}

.title {
  position: absolute;
  left: 50%;
  top: 4px;
  transform: translateX(-50%);
  height: 34px;
  display: flex;
  align-items: center;
  white-space: nowrap;
  font-size: 12px;
  color: #ffd52e;
  text-shadow: 2px 2px 0 #4f2606;
  text-transform: uppercase;
}

.back {
  position: absolute;
  left: 4px;
  top: 4px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* The box is the point itself: the door and the label hang from it, as a character's do. */
.exit {
  position: absolute;
  width: 0;
  height: 0;
  padding: 0;
  border: 0;
  background: none;
}

/* The pictures and the names are over the dotted lines (of any of them), which are at the layer's own level. */
.exit-icon {
  z-index: 1;
  position: absolute;
  left: -11px;
  top: -11px;
  width: 22px;
  height: 22px;
  max-width: none;
}

/* A place with a quest: its door breathes, on a layer of its own so the map under it is not drawn again. */
.exit.has-quest .exit-icon {
  will-change: opacity;
}

.world.pulse .exit.has-quest .exit-icon {
  opacity: 0.45;
}

.exit-label {
  z-index: 1;
  position: absolute;
  left: 0;
  top: 10px;
  transform: translateX(-50%);
  padding: 1px 4px;
  font-size: 10px;
  line-height: 12px;
  white-space: nowrap;
  text-align: center;
}

.exit-line {
  display: block;
}

.marker-leader {
  position: absolute;
  left: 44px;
  top: 22px;
}

.marker {
  position: absolute;
  width: 88px;
  height: 62px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}

/* The characters hang from their own points; the wrapper only holds their stacking order. */
.actors {
  position: absolute;
  inset: 0;
  isolation: isolate;
  pointer-events: none;
}

/* Taps go through to the markers below. The script moves it, with the point `useFollow` glides, and makes it blink. */
.player {
  position: absolute;
  left: 0;
  top: 0;
  width: 22px;
  height: 22px;
  pointer-events: none;
}

/* The blink: `lit` is toggled from the script. */
.player.lit {
  filter: brightness(1.4);
}

.objective {
  position: absolute;
  top: -16px;
  width: 22px;
  height: 22px;
}

/* Sprites keep their own size (about 38 px high); bigger pictures shrink to fit. */
.sprite-box {
  position: relative;
  z-index: 1;
  height: 38px;
  display: flex;
  align-items: flex-end;
}

.sprite {
  max-height: 38px;
  max-width: 80px;
}

.marker-label {
  position: relative;
  z-index: 1;
  padding: 1px 4px;
  font-size: 10px;
  white-space: nowrap;
}

.dock {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 4px;
  display: flex;
  align-items: flex-end;
  gap: 4px;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.buttons {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.tool {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
