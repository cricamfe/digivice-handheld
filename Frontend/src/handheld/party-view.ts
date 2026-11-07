import type { Digimon, Vital } from "@/models";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";
import type { DigimonStatus } from "@/types/digimon-status.type";

export interface DigimonVitals {
  hp: Vital;
  mp: Vital;
  condition: number;
  status: DigimonStatus;
}

/** HP, MP and state as the player sees them: in a battle they come from the battle counters. */
export function getVitals(digimon: Digimon, mapId: string): DigimonVitals {
  const isInBattle = ProfilePresenter.isInBattle(mapId, digimon.inBattle);
  const hp = ProfilePresenter.getHp(digimon, isInBattle);
  const mp = ProfilePresenter.getMp(digimon, isInBattle);
  const condition = ProfilePresenter.getCondition(digimon, isInBattle);

  return { hp, mp, condition, status: ProfilePresenter.getStatus(condition, hp) };
}

/** Width of a bar fill. One decimal keeps the edge on the same pixel the design shows. */
export function percentText(current: number, maximum: number): string {
  return `${Math.calculatePercentage(current, maximum).toFixed(1)}%`;
}
