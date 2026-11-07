/** A character of the map the field draws: `key1` says who, `x` and `y` are pixels of the map picture, `dir` is 0 to 7. */
export interface FieldActorDTO {
  key1: number;
  x: number;
  y: number;
  dir: number;
}

/**
 * The characters of the map the field draws besides the party, the whole list each time (empty out of the field). Only
 * the Android core sends it (`FieldActorsChanged`), with the full cycle, when the list changes.
 */
export interface FieldActorsDTO {
  actors: FieldActorDTO[];
}
