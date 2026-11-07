/**
 * The game is closing a field or has not opened the next one, with the map it leaves for when known. Only the Android core
 * sends it.
 */
export interface FieldChangingDTO {
  changing: boolean;
  to: string | null;
}
