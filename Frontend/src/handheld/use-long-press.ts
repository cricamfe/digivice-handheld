import { onBeforeUnmount } from "vue";

/** Holding this long counts as a long press. */
const HoldMilliseconds = 1000;
/** A finger that travels farther than this is scrolling or aiming elsewhere, not holding. */
const MoveTolerance = 10;

/**
 * A long press read from pointer events (the browser must not start a pan, zoom or selection: give the element
 * `touch-action: none`). Bind `onPointerDown` and `onPointerMove` on the element and `cancel` to up, cancel and leave.
 * The press ends if the finger leaves the element or travels too far; leaving the page mid-press cancels it.
 */
export function useLongPress(onLongPress: () => void) {
  let timer: number | null = null;
  let pointer: number | null = null;
  let startX = 0;
  let startY = 0;
  // Set when the hold fired, so the click that follows the release can be told apart.
  let held = false;

  function cancel(): void {
    if (timer !== null) {
      window.clearTimeout(timer);
      timer = null;
    }
    pointer = null;
  }

  function onPointerDown(event: PointerEvent): void {
    if (!event.isPrimary || (event.pointerType === "mouse" && event.button !== 0)) {
      return;
    }

    cancel();
    held = false;
    pointer = event.pointerId;
    startX = event.clientX;
    startY = event.clientY;
    timer = window.setTimeout(() => {
      timer = null;
      held = true;
      onLongPress();
    }, HoldMilliseconds);
  }

  function onPointerMove(event: PointerEvent): void {
    if (timer === null || event.pointerId !== pointer) {
      return;
    }

    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const outside =
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom;
    const moved = Math.hypot(event.clientX - startX, event.clientY - startY) > MoveTolerance;
    if (outside || moved) {
      cancel();
    }
  }

  /** Whether the press that just ended was a long one. Reads once: the next call is false until another long press. */
  function consumeHeld(): boolean {
    const was = held;
    held = false;
    return was;
  }

  onBeforeUnmount(cancel);

  return { onPointerDown, onPointerMove, cancel, consumeHeld };
}
