import { gameWord } from "./language";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { MobiusDesertAreasMapRepository } from "@/repositories/mobius-desert-areas-map.repository";
import { DesertNeighborHelper } from "@/presenters/helper/desert-neighbor.helper";
import { MobiusDesertAreaDetailsPresenter } from "@/presenters/mobius-desert-modal/mobius-desert-area-details.presenter";
import { MobiusDesertAreasPresenter } from "@/presenters/mobius-desert-modal/mobius-desert-areas.presenter";
import type { GameServer } from "@/types/game-server.type";
import type { DesertAreaViewModel } from "@/viewmodels/desert/desert-area.viewmodel";
import { actorUrl, genericNpcUrl, type TileName } from "./assets";
import { ActorFrames, ActorNames } from "./field-actors";
import { gameText, serverCopyOf, type Texts } from "./map-view";

export interface MobiusCell {
  key: string;
  /** Name of the area as the grid shows it (`A3`, `NDS`). */
  label: string;
  /** Spoken name: the label and the note, if the area has one. */
  name: string;
  tile: TileName;
  /** Only the areas of the desert can be picked; the border and the two towers cannot. */
  isPlayable: boolean;
  isPlayer: boolean;
  note: string | null;
  isDim: boolean;
  /** The label sits at the foot of the cell when something is drawn above it. */
  labelAtFoot: boolean;
  /** Picture of the character of the area, when the game has one for it. */
  sprite: string | null;
}

export interface MobiusGrid {
  size: number;
  cells: MobiusCell[];
  /** Label of the area the player is in, null when the game is not in the desert. */
  playerLabel: string | null;
}

export interface MobiusExit {
  /** What the compass prints: `A4`, `NDS`... */
  label: string;
  /** The area it leads to when it can be picked, so a tap moves the selection there. */
  area: string | null;
}

/** A picture of a character and where it sits against the point it stands on, at twice the game's size, in pixels. */
export interface AgentPicture {
  url: string;
  left: number;
  top: number;
  width: number;
  height: number;
}

export interface MobiusZone {
  label: string;
  north: MobiusExit;
  south: MobiusExit;
  east: MobiusExit;
  west: MobiusExit;
  imageUrl: string | null;
  /** The character of the area: where it stands on the picture (percent of it), its note and how it is drawn. */
  agent: { x: number; y: number; note: string | null; picture: AgentPicture } | null;
}

/** The three sand tiles alternate over the grid, as the design lays them. */
const SandTiles: readonly TileName[] = ["sand-0", "sand-1", "sand-2"];

/** Short names the grid can fit in a 54 px cell. */
function abbreviation(label: string, texts: Texts): string {
  if (label === "noiseDesertS" || label === "mirageTower") {
    return texts.t(`handheld.mobius.${label}`);
  }

  return label;
}

function tileOf(area: DesertAreaViewModel, row: number, column: number): TileName {
  if (area.type === "border") {
    return "wrap";
  }

  if (area.type === "noiseDesertS") {
    return "noise";
  }

  if (area.type === "mirageTower") {
    return "mirage";
  }

  return SandTiles[(row + column * 2) % SandTiles.length]!;
}

export function buildMobiusGrid(
  mapId: string,
  mapVariant: number | null,
  texts: Texts,
  server: GameServer = "asuka",
): MobiusGrid {
  const { gridSize, areas } = MobiusDesertAreasPresenter.getAreas(server);
  const playerLabel = MobiusDesertAreasPresenter.getCurrentAreaLabel(mapId, mapVariant);

  const cells = areas.flatMap((row, rowIndex) => {
    return row.map((area, columnIndex): MobiusCell => {
      const isPlayable = area.type === "normal";
      const isPlayer = isPlayable && area.label === playerLabel;
      const label = abbreviation(area.label, texts);
      const note = noteOf(area.note);

      return {
        key: `${rowIndex}-${columnIndex}`,
        label,
        name: note === null ? label : `${label}, ${note}`,
        tile: tileOf(area, rowIndex, columnIndex),
        isPlayable,
        isPlayer,
        note,
        isDim: area.type === "border",
        labelAtFoot: isPlayer || note !== null,
        sprite: spriteUrlOf(area.note ?? null, server),
      };
    });
  });

  return { size: gridSize, cells, playerLabel };
}

/** The area with this label that can be picked, or null for the border, the towers and unknown labels. */
function playableArea(label: string, server: GameServer): DesertAreaViewModel | null {
  const { areas } = MobiusDesertAreasPresenter.getAreas(server);
  return areas.flat().find((area) => area.type === "normal" && area.label === label) ?? null;
}

function exitOf(label: string, texts: Texts, server: GameServer): MobiusExit {
  return {
    label: abbreviation(label, texts),
    area: playableArea(label, server)?.label ?? null,
  };
}

/** The person of the design, 19 x 34 drawn twice as big, for a character that has no picture here. */
const GenericAgent: Omit<AgentPicture, "url"> = { left: -19, top: -68, width: 38, height: 68 };

/**
 * What a note says: the talk lines that name the characters of an area (`talk07:848`, as `ActorNames` has them; the DRI
 * agents of the Asuka server by a line of their talk), read from the disc. Null when there is no note, or when the
 * extracted text lacks every line of it.
 */
function noteOf(note: string | undefined): string | null {
  if (note === undefined) {
    return null;
  }

  const names = note.split("/").flatMap((reference) => gameWord(reference, "") || []);
  return names.length === 0 ? null : names.join("/");
}

/**
 * The character of the game a note names. The DRI agents of the Asuka server are drawn as the design's person; the
 * characters of the Amaterasu server (map 02C1) are Digimon, and the game has a picture of each: the character of
 * `ActorNames` whose talk line is the note's first.
 */
function characterOf(note: string | null, server: GameServer): number | null {
  if (note === null || server !== "amaterasu") {
    return null;
  }

  const first = note.split("/")[0];
  const key = Object.entries(ActorNames["02C1"] ?? {}).find(
    ([, reference]) => reference === first,
  )?.[0];
  return key === undefined ? null : Number(key);
}

function spriteUrlOf(note: string | null, server: GameServer): string | null {
  const key = characterOf(note, server);
  return key === null ? null : actorUrl(key, 0);
}

function agentPicture(note: string | null, server: GameServer): AgentPicture {
  const key = characterOf(note, server);
  const frame = key === null ? undefined : ActorFrames[key]?.[0];
  const url = spriteUrlOf(note, server);
  if (frame === undefined || url === null) {
    return { url: genericNpcUrl(), ...GenericAgent };
  }

  return { url, left: frame[0] * 2, top: frame[1] * 2, width: frame[2] * 2, height: frame[3] * 2 };
}

/** The picture of a map of the Amaterasu server, which has pictures of its own for the maps it repeats. */
function copyPictureOf(copyId: string): string | null {
  return serverCopyOf(copyId) === null ? null : ImageCatalog.getLocationImageUrl(copyId);
}

/** The compass and the picture of an area; null when the data has nothing for that label. */
export function buildMobiusZone(
  label: string,
  texts: Texts,
  server: GameServer = "asuka",
): MobiusZone | null {
  const found = MobiusDesertAreasMapRepository.findByLabel(label, server);
  const area = playableArea(label, server);
  if (found === null || area === null) {
    return null;
  }

  const details = MobiusDesertAreaDetailsPresenter.getAreaDetails(area, server);
  const coordinates = details?.coordinates ?? null;
  // `found.locationId` is the map of the server (02C1 on Amaterasu), whose picture is the copy's own.
  const copyPicture = server === "amaterasu" ? copyPictureOf(found.locationId) : null;

  return {
    label,
    north: exitOf(found.cell.north, texts, server),
    south: exitOf(found.cell.south, texts, server),
    east: exitOf(found.cell.east, texts, server),
    west: exitOf(found.cell.west, texts, server),
    imageUrl: copyPicture ?? details?.imageUrl ?? null,
    agent:
      coordinates === null
        ? null
        : {
            x: coordinates.x,
            y: coordinates.y,
            note: noteOf(area.note),
            picture: agentPicture(area.note ?? null, server),
          },
  };
}

export interface MobiusLegendEntry {
  tile: TileName;
  label: string;
}

/** The terrain key under the grid. The two towers carry the names the map gives them. */
export function buildMobiusLegend(texts: Texts): MobiusLegendEntry[] {
  const nameOf = (neighbor: string): string => {
    const resolved = DesertNeighborHelper.resolveNeighborName(neighbor);
    return resolved.kind === "i18n"
      ? (gameText(texts, resolved.key) ?? neighbor)
      : gameWord(resolved.value);
  };

  return [
    { tile: "sand-0", label: texts.t("handheld.mobius.sand") },
    { tile: "wrap", label: texts.t("handheld.mobius.wrap") },
    { tile: "noise", label: nameOf("noiseDesertS") },
    { tile: "mirage", label: nameOf("mirageTower") },
  ];
}
