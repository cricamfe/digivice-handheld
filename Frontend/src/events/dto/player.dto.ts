export interface PlayerDTO {
  bits?: number;
  mapId?: string;
  previousMapId?: string;
  seabedRoute?: number | null;
  mapVariant?: number | null;
  positionX?: number;
  positionY?: number;
  extraChapter?: boolean;
}
