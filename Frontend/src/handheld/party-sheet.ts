import type { DigimonSlot } from "@/models";
import { DigimonAttributes, type DigimonAttribute } from "@/constants/digimon-attribute.constant";
import { DigimonElements, type DigimonElement } from "@/constants/digimon-element.constant";
import { DvexpBarSegments } from "@/constants/digievolution.constant";
import { DigievolutionPresenter } from "@/presenters/digievolution/digievolution.presenter";
import { DigievolutionDvexpPresenter } from "@/presenters/digievolution/digievolution-dvexp.presenter";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import { EquipmentsPresenter } from "@/presenters/party/digimon/equipments.presenter";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";
import { BlastProgressBarPresenter } from "@/presenters/party/digimon/profile/progress-bar/blast-progress-bar.presenter";
import { ExperienceProgressBarPresenter } from "@/presenters/party/digimon/profile/progress-bar/experience-progress-bar.presenter";
import { StatsPresenter } from "@/presenters/party/digimon/stats.presenter";
import type { EquipmentSlotViewModel } from "@/viewmodels/digimon/equipment-slot.viewmodel";
import type { StatViewModel } from "@/viewmodels/digimon/stat.viewmodel";
import type { DigimonStat } from "@/types/digimon-stat.type";
import type { EquipmentSlot } from "@/types/equipment-slot.type";
import { DigimonRepository } from "@/repositories/digimon.repository";
import { EquipmentRepository } from "@/repositories/equipment.repository";
import { attempt } from "./attempt";
import { statDetail, type FigureDetail } from "./figure-detail";
import { getVitals, percentText } from "./party-view";
import { equipmentKindWord, slotWord } from "./game-words";
import { itemIcon } from "./wiki-common";

export type Translate = (key: string, named?: Record<string, unknown>) => string;

export interface SheetBar {
  kind: "exp" | "hp" | "mp" | "blast";
  label: string;
  text: string;
  percent: string;
}

export interface SheetDigievolution {
  key: number;
  digievolutionId: number | null;
  name: string;
  level: number | null;
  /**
   * Lit: in a battle, the form the fighter is in; out of one, the form the partner starts a battle in (the game's own
   * status screen marks that one, nothing more).
   */
  isActive: boolean;
  /** Out of a battle, the lit one is only the form a battle starts in: the row says so. */
  startsBattle: boolean;
  /** Filled segments out of `DvexpBarSegments`. */
  filled: number;
}

/** What a floating panel says about a piece of equipment a partner wears. */
export interface EquipmentDetail {
  id: number;
  name: string;
  icon: string;
  typeLabel: string;
  attributes: { key: string; label: string; value: string; isPenalty: boolean }[];
  /** Names of the partners that can wear it, empty when `forAll`. */
  equipable: string[];
  /** Every partner can wear it. */
  forAll: boolean;
  /** The Wiki has a sheet for it (`wikiEquipo` with `id`). */
  hasWiki: boolean;
}

export interface SheetEquipment {
  key: EquipmentSlot;
  /** The name of the equipment, or the name of the slot when it is empty. */
  label: string;
  isEquipped: boolean;
  /** Null for an empty slot. */
  detail: EquipmentDetail | null;
}

export interface SheetStat {
  key: DigimonStat;
  label: string;
  value: number;
  /** Digievolution bonus, 0 when there is none. */
  bonus: number;
  detail: FigureDetail;
}

export interface StatusRow {
  /** Kit icon: the check, the alert and the KO mark of the state, or the icon of the alteration. */
  icon: string;
  text: string;
  tone: "up" | "down" | "";
}

export interface Sheet {
  name: string;
  level: number;
  tp: number;
  /** What the partner's state says, one line each: its state, or every alteration it suffers. */
  statusRows: StatusRow[];
  bars: SheetBar[];
  digievolutions: SheetDigievolution[];
  equipment: SheetEquipment[];
  attributes: SheetStat[];
  elements: SheetStat[];
}

const equipmentSlots: readonly EquipmentSlot[] = [
  "head",
  "body",
  "right",
  "left",
  "accessory1",
  "accessory2",
];

const stateMarks = {
  healthy: { icon: "check", tone: "up" },
  injured: { icon: "alert", tone: "" },
  ko: { icon: "ko", tone: "down" },
} as const;

/** The state of a Digimon in words: the alterations when it has any, the plain state otherwise. */
export function statusRows(slot: DigimonSlot, mapId: string, t: Translate): StatusRow[] {
  const { status, condition } = getVitals(slot.digimon!, mapId);
  if (status !== "debuffed") {
    return [{ ...stateMarks[status], text: t(`digimon.status.${status}`) }];
  }

  return ProfilePresenter.getActiveDebuffs(condition).map((debuff) => ({
    icon: debuff,
    text: t(`digimon.debuff.${debuff}.affected`),
    tone: "down",
  }));
}

function buildBars(slot: DigimonSlot, mapId: string, t: Translate): SheetBar[] {
  const digimon = slot.digimon!;
  const { hp, mp } = getVitals(digimon, mapId);
  const experience = ExperienceProgressBarPresenter.getCalculatedExperienceValues(
    slot.digimonId!,
    digimon.level,
    digimon.experience,
  );
  const maxBlast = BlastProgressBarPresenter.MaxBlast;

  return [
    {
      kind: "exp",
      label: t("handheld.bar.exp"),
      // At the maximum level there is no next threshold, so only the total is shown.
      text:
        experience.maxValue === 0
          ? `${digimon.experience}`
          : `${digimon.experience}/${experience.maxValue}`,
      percent: `${experience.percentage.toFixed(1)}%`,
    },
    {
      kind: "hp",
      label: t("handheld.bar.hp"),
      text: `${hp.current}/${hp.max}`,
      percent: percentText(hp.current, hp.max),
    },
    {
      kind: "mp",
      label: t("handheld.bar.mp"),
      text: `${mp.current}/${mp.max}`,
      percent: percentText(mp.current, mp.max),
    },
    {
      kind: "blast",
      label: t("handheld.bar.blast"),
      text: `${digimon.blast}/${maxBlast}`,
      percent: percentText(digimon.blast, maxBlast),
    },
  ];
}

function buildDigievolutions(slot: DigimonSlot, isInBattle: boolean): SheetDigievolution[] {
  const digimon = slot.digimon!;
  // In a battle the form is where the fighter is now, which is not the setting once it digivolved; none is lit when it
  // fights in its own form.
  const litId = isInBattle ? digimon.inBattle.formId : digimon.activeDigievolutionId;

  return digimon.digievolutions.map((entry) => {
    if (entry.digievolutionId === null || entry.digievolution === null) {
      return {
        key: entry.index,
        digievolutionId: null,
        name: "",
        level: null,
        isActive: false,
        startsBattle: false,
        filled: 0,
      };
    }

    return {
      key: entry.index,
      digievolutionId: entry.digievolutionId,
      name: DigievolutionPresenter.getNameById(entry.digievolutionId),
      level: entry.digievolution.level,
      isActive: entry.digievolutionId === litId,
      startsBattle: !isInBattle && entry.digievolutionId === litId,
      filled: Math.min(
        DvexpBarSegments,
        DigievolutionDvexpPresenter.getFilledSegments(
          entry.digievolutionId,
          entry.digievolution.level,
          entry.digievolution.dvexp,
        ),
      ),
    };
  });
}

function equipmentSlotViewModels(slot: DigimonSlot): EquipmentSlotViewModel[] {
  try {
    return EquipmentsPresenter.getEquipmentsViewModel(slot.digimon!.equipments);
  } catch {
    // An equipment missing from the database must not blank the whole screen.
    return equipmentSlots.map((slotKey) => ({ slotKey, equipment: null }));
  }
}

function buildEquipment(slot: DigimonSlot, t: Translate): SheetEquipment[] {
  return equipmentSlotViewModels(slot).map(({ slotKey, equipment }) => {
    if (equipment === null) {
      return {
        key: slotKey,
        label: slotWord(t, slotKey),
        isEquipped: false,
        detail: null,
      };
    }

    const label = t(`equipments.${equipment.id}.name`);
    return {
      key: slotKey,
      label,
      isEquipped: true,
      detail: {
        id: equipment.id,
        name: label,
        icon: itemIcon(equipment.id),
        typeLabel: equipmentKindWord(t, equipment.type),
        attributes: equipment.attributes.map((attribute) => ({
          key: attribute.attribute,
          label: t(`stat.${attribute.attribute.toLowerCase()}`),
          value: `${attribute.type}${attribute.value}`,
          isPenalty: attribute.type !== "+",
        })),
        equipable: equipment.equipableDigimonNames,
        forAll: equipment.equipableDigimonIds.length === DigimonRepository.getIds().length,
        hasWiki: attempt(() => EquipmentRepository.getById(equipment.id) !== undefined, false),
      },
    };
  });
}

function buildStat(key: DigimonStat, stat: StatViewModel, t: Translate): SheetStat {
  const label = t(`stat.${key}`);
  const battleDelta = stat.fromBattle ?? 0;
  const value = stat.sumBetweenDigimonAndEquipaments + battleDelta;

  return {
    key,
    label,
    value,
    bonus: stat.fromDigievolution,
    detail: statDetail(t, {
      key,
      label,
      base: stat.fromDigimon,
      total: value,
      changes: [
        { label: t("handheld.figure.fromEquipment"), amount: stat.fromEquipaments },
        { label: t("handheld.figure.fromBattle"), amount: battleDelta },
      ],
      digievolution: stat.fromDigievolution,
    }),
  };
}

/**
 * The partner's name, or in a battle the name of the form the fighter is in, which is how the game names it when it
 * digivolves (its message uses the form's name). A form the data does not know leaves the partner's own name.
 */
function nameOf(slot: DigimonSlot, isInBattle: boolean): string {
  const form = isInBattle
    ? DigievolutionRepository.findRawDigievolutionById(slot.digimon!.inBattle.formId)
    : undefined;
  return form === undefined
    ? ProfilePresenter.getName(slot.digimonId!)
    : DigievolutionRepository.getNameById(slot.digimon!.inBattle.formId);
}

/** Everything the Equipo screen draws for one party Digimon, from the existing presenters. */
export function buildSheet(slot: DigimonSlot, mapId: string, t: Translate): Sheet {
  const digimon = slot.digimon!;
  const stats = StatsPresenter.getStatsViewModel(digimon, mapId);
  const isInBattle = ProfilePresenter.isInBattle(mapId, digimon.inBattle);

  return {
    name: nameOf(slot, isInBattle),
    level: digimon.level,
    tp: digimon.tp,
    statusRows: statusRows(slot, mapId, t),
    bars: buildBars(slot, mapId, t),
    digievolutions: buildDigievolutions(slot, isInBattle),
    equipment: buildEquipment(slot, t),
    attributes: DigimonAttributes.map((key: DigimonAttribute) =>
      buildStat(key, stats.attributes[key], t),
    ),
    elements: DigimonElements.map((key: DigimonElement) => buildStat(key, stats.elements[key], t)),
  };
}
