import { ref } from "vue";
import type { PanelAnchor } from "./components/FloatingPanel.vue";

/**
 * The element a screen's floating panels live in (it must be positioned: `position: relative`), and the box of
 * anything inside it in the host's own pixels, which is what `FloatingPanel` places itself by.
 */
export function usePanelHost() {
  const host = ref<HTMLElement | null>(null);
  const hostSize = ref({ width: 0, height: 0 });

  /** The box of `element` in the host, measured now; null when the host is not drawn. */
  function anchorOf(element: unknown): PanelAnchor | null {
    const stage = host.value;
    if (stage === null || !(element instanceof HTMLElement)) {
      return null;
    }

    const box = stage.getBoundingClientRect();
    const rect = element.getBoundingClientRect();
    const left = box.left + stage.clientLeft;
    const top = box.top + stage.clientTop;
    hostSize.value = { width: stage.clientWidth, height: stage.clientHeight };
    return {
      top: rect.top - top,
      bottom: rect.bottom - top,
      left: rect.left - left,
      right: rect.right - left,
    };
  }

  return { host, hostSize, anchorOf };
}
