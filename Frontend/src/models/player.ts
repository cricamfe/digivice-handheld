export interface Player {
  bits: number;
  mapId: string;
  previousMapId: string;
  seabedRoute: number | null;
  mapVariant: number | null;
  /** Where the player stands, in pixels of the map picture; (0, 0) when the game has no player on the field. */
  positionX: number;
  positionY: number;
  extraChapter?: boolean;
}
