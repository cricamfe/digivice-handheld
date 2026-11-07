import { computed, ref } from "vue";

/**
 * Draws a long list a piece at a time. The lists of the Wiki have up to 600 entries (a single letter
 * in the search matches that many) and drawing them all at once stalls the first frame on a handheld;
 * the first piece is enough to fill the screen and the rest is added as the player scrolls down.
 */
export function useGrowing(total: () => number, chunk: number) {
  const limit = ref(chunk);
  const shown = computed(() => Math.min(limit.value, total()));

  /** Adds the next piece when the scroll is near the end of what is drawn. */
  function onScroll(scroller: HTMLElement): void {
    const isNearEnd = scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 160;
    if (isNearEnd && limit.value < total()) {
      limit.value += chunk;
    }
  }

  /** Starts again from the first piece (another list, another word). */
  function reset(): void {
    limit.value = chunk;
  }

  /** Makes sure at least `count` entries are drawn, to scroll back to where the player was. */
  function ensure(count: number): void {
    if (count > limit.value) {
      limit.value = count;
    }
  }

  return { shown, onScroll, reset, ensure };
}
