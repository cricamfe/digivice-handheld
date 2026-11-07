import { ref, watch, type Ref } from "vue";
import type { ActorRole } from "./field-actors";

/**
 * What the Mapa draws besides the picture, and the player's choice of it: the exits, what the quests ask for, the game's characters, and of them the
 * ones of each role. All are on until the player turns them off, and the choice is kept across restarts. The storage can
 * fail (a private window, blocked data) and then the choice only lasts while the app runs.
 */
const Keys = {
  exits: "digivice.map.exits",
  objectives: "digivice.map.objectives",
  characters: "digivice.map.characters",
} as const;

function load(key: string): boolean {
  try {
    return window.localStorage.getItem(key) !== "0";
  } catch {
    return true;
  }
}

function keep(key: string): Ref<boolean> {
  const option = ref(load(key));
  watch(option, (value) => {
    try {
      window.localStorage.setItem(key, value ? "1" : "0");
    } catch {
      // Not kept: the choice still holds until the app closes.
    }
  });
  return option;
}

/** The roles, in the order the filters show them: what a character is for comes before who only chats. */
export const ActorRoleOrder: readonly ActorRole[] = [
  "shop",
  "service",
  "battle",
  "story",
  "chest",
  "talk",
];

const exits = keep(Keys.exits);
const objectives = keep(Keys.objectives);
const characters = keep(Keys.characters);
const roles = Object.fromEntries(
  ActorRoleOrder.map((role) => [role, keep(`digivice.map.role.${role}`)]),
) as Record<ActorRole, Ref<boolean>>;

export function useMapOptions() {
  return { exits, objectives, characters, roles };
}
