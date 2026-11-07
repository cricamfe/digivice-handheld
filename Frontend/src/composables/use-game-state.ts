import { computed, shallowRef } from "vue";
import type { State } from "@/models";
import { useGameStore } from "@/stores/use-game-store";

/**
 * The last state the game sent, kept by the handheld's App.vue while the state is gone for a moment (the core clears it on
 * every read error): the screens go on drawing it instead of throwing, until the waiting screen takes over. Nothing sets it
 * on the other apps, so for them `useGameState` is exactly the store's state.
 */
const held = shallowRef<State | null>(null);

export function holdGameState(state: State | null): void {
  held.value = state;
}

// Only valid inside the main tree, which App.vue renders exclusively while currentState exists (or is held).
export function useGameState() {
  const store = useGameStore();

  return computed(() => {
    const state = store.currentState ?? held.value;
    if (!state) {
      throw new Error("Game state accessed before InitialState.");
    }

    return state;
  });
}
