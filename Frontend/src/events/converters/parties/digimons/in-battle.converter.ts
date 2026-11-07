import type { DeepRequired } from "@/events/dto/deep-required";
import type { InBattleDTO } from "@/events/dto/parties/digimons/in-battle.dto";
import type { InBattle } from "@/models";
import { VitalConverter } from "./vital.converter";

export class InBattleConverter {
  public static convert(inBattleDto: DeepRequired<InBattleDTO> | undefined): InBattle {
    // The native bridge has no battle overlay yet; unknown values must not look like real zero stats.
    if (!inBattleDto) {
      return {
        formId: NaN,
        condition: NaN,
        strength: NaN,
        defense: NaN,
        speed: NaN,
        hp: { current: NaN, max: NaN },
        mp: { current: NaN, max: NaN },
      };
    }
    return {
      formId: inBattleDto.formId,
      condition: inBattleDto.condition,
      strength: inBattleDto.strength,
      defense: inBattleDto.defense,
      speed: inBattleDto.speed,
      hp: VitalConverter.convert(inBattleDto.hp),
      mp: VitalConverter.convert(inBattleDto.mp),
    };
  }
}
