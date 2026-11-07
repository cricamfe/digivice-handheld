/**
 * The position of the player in pixels of the map picture. Only the Android core sends it (`PlayerPositionChanged`),
 * between the full cycles that send the state.
 */
export interface PlayerPositionDTO {
  x: number;
  y: number;
}
