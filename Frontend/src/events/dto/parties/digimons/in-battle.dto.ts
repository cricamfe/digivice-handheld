import type { VitalDTO } from "./vital.dto";

export interface InBattleDTO {
  formId?: number;
  condition?: number;
  strength?: number;
  defense?: number;
  speed?: number;
  hp?: VitalDTO;
  mp?: VitalDTO;
}
