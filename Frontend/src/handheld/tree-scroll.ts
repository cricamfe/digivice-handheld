/** A box along the row of a family of the evolution tree, in the row's own pixels (from its first column). */
export interface Span {
  left: number;
  right: number;
}

/** Where to scroll a row of the tree to, and the empty room its end needs so that scroll can be reached. */
export interface RowScroll {
  scrollLeft: number;
  endRoom: number;
}

/**
 * How to scroll a row of the evolution tree so that `node` is whole on screen with a column on the left edge, never one
 * cut in half: the first column when the node is already whole from there, else the leftmost column that leaves the node
 * whole. The last columns can only start the view with some room after them, which `endRoom` says. A node wider than
 * the view starts it.
 */
export function rowScrollFor(
  columns: readonly number[],
  node: Span,
  viewWidth: number,
  contentWidth: number,
): RowScroll {
  const starts = [...new Set(columns)].sort((a, b) => a - b);
  const first = starts[0] ?? 0;
  const scrollLeft =
    node.right - first <= viewWidth
      ? first
      : (starts.find((start) => start <= node.left && node.right - start <= viewWidth) ??
        node.left);

  return { scrollLeft, endRoom: Math.max(0, scrollLeft + viewWidth - contentWidth) };
}
