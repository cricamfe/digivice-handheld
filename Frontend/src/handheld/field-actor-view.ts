import { englishWord, gameWord } from "./language";
import type { FieldActorDTO } from "@/events/dto/field-actors.dto";
import { actorUrl } from "./assets";
import {
  ActorFrames,
  ActorNames,
  ActorRoles,
  ActorWiki,
  type ActorFrame,
  type ActorRole,
} from "./field-actors";
import type { ScreenName, ScreenParam } from "./router";

/** A character of the map the field is drawing, ready for the Mapa: its picture, where it sits, and its name if the game has one. */
export interface LiveActor {
  /** Stays the same while the character stays, even when another one with the same `key1` comes and goes. */
  key: string;
  /** Pixels of the map picture, where its feet are. */
  x: number;
  y: number;
  src: string;
  /** Where the picture sits against the point, and its size. */
  frame: ActorFrame;
  /** From the game's own talk text on the disc, in the language shown and shortened, or null (none, or not on the disc). */
  name: string | null;
  /** Where the game names it: the talk line with its name (`talk04:879`), the same for everyone it names alike. */
  speaker: string | null;
  /** What it is for, in the game's scripts. */
  role: ActorRole;
  /** The Wiki sheet of the one NPC or tamer with exactly this name, if one has it (`ActorWiki`). */
  target: { screen: ScreenName; param: ScreenParam } | null;
}

/** Names up to this long stay whole. */
const NameLimit = 10;
/** The last word of a long name that says what they do, not who they are: without the rest it would say nothing. */
const RoleWords =
  /^(Chief|Leader|Officer|Reception|Desk|Citizen|Civilian|Service|Login|Manger|Navi|Knight|Master|Wife|\d+)\??$/;

/**
 * A long English name as the Mapa shows it: most of them are a nickname and a name ("Mail Lover Saki", "Rosa from Spain", "Hal the
 * Bully"), and the name is what tells two people apart. The full one stays on the Wiki's sheet.
 */
export function shortName(name: string): string {
  if (name.length <= NameLimit) {
    return name;
  }

  const inFront = /^(\S+) (?:from|the) /.exec(name);
  if (inFront?.[1] !== undefined) {
    return inFront[1];
  }

  const last = name.split(" ").at(-1) ?? name;
  return RoleWords.test(last) ? name : last;
}

/** Names of another language have no such pattern: those up to this long stay whole, and the longer ones keep their last word. */
const OtherNameLimit = 14;

/**
 * The name of the talk line `reference` as the Mapa shows it, in the language shown: the English patterns when it reads
 * as in English, the rule of the other languages when not. Null when the disc's text doesn't have the line.
 */
export function shownName(reference: string): string | null {
  const name = gameWord(reference, "");
  if (name === "") {
    return null;
  }

  if (name === englishWord(reference)) {
    return shortName(name);
  }

  return name.length <= OtherNameLimit ? name : (name.split(" ").at(-1) ?? name);
}

/** The Wiki sheet of a character of the map `mapId`, when one NPC or tamer of the app has exactly its name. */
function wikiTargetOf(mapId: string, character: number): LiveActor["target"] {
  const id = ActorWiki[mapId]?.[character];
  return id === undefined ? null : { screen: "wikiNpc", param: id };
}

/**
 * The characters in `actors` that can be drawn, in the map `mapId` (the game's id, a copy for the second server
 * included). A character without a picture is left out: the game has none for it either (an invisible one).
 */
export function liveActorsOf(actors: readonly FieldActorDTO[], mapId: string): LiveActor[] {
  const seen = new Map<number, number>();
  const live: LiveActor[] = [];
  for (const actor of actors) {
    const occurrence = seen.get(actor.key1) ?? 0;
    seen.set(actor.key1, occurrence + 1);

    const src = actorUrl(actor.key1, actor.dir);
    const frame = ActorFrames[actor.key1]?.[actor.dir];
    if (src === null || frame === undefined) {
      continue;
    }

    const speaker = ActorNames[mapId]?.[actor.key1] ?? null;
    live.push({
      key: `${actor.key1}#${occurrence}`,
      x: actor.x,
      y: actor.y,
      src,
      frame,
      name: speaker === null ? null : shownName(speaker),
      speaker,
      role: ActorRoles[mapId]?.[actor.key1] ?? "talk",
      target: wikiTargetOf(mapId, actor.key1),
    });
  }

  return live;
}
