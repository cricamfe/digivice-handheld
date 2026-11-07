import type { Digimon } from "@/models";
import {
  AttributesConverter,
  type AttributesEquipmentBonuses,
} from "@/presenters/converter/attributes.converter";
import {
  ElementsConverter,
  type ElementsEquipmentBonuses,
} from "@/presenters/converter/elements.converter";
import { EquipmentService } from "@/services/equipment.service";
import { DigimonBattleSelector } from "@/stores/selectors/digimon-battle.selector";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import { EquipmentRepository } from "@/repositories/equipment.repository";
import type { EquipmentRaw } from "@/repositories/tables/raws/equipment/equipment.raw";
import type { DigievolutionRaw } from "@/repositories/tables/raws/digievolution/digievolution.raw";
import type { AttributesViewModel } from "@/viewmodels/digimon/attributes.viewmodel";
import type { DigimonStatsViewModel } from "@/viewmodels/digimon/digimon-stats.viewmodel";
import type { DigievolutionViewModel } from "@/viewmodels/digievolution/digievolution.viewmodel";
import { digimonName } from "@/handheld/digimon-names";

export class StatsPresenter {
  public static getStatsViewModel(digimon: Digimon, location: string): DigimonStatsViewModel {
    const isInBattle = DigimonBattleSelector.isInBattle(location, digimon.inBattle);
    // The game adds a form's stats and resistances only in a battle, for the form the fighter is in (FIGHTSTG_computeStats):
    // its status screen (computeStats) shows the partner's own stats with its equipment. `activeDigievolutionId` is only
    // the form a battle starts in (Partner.battleDigivolve), so it adds nothing by itself.
    const form = isInBattle
      ? DigievolutionRepository.findRawDigievolutionById(digimon.inBattle.formId)
      : undefined;
    const activeDigievolution = form === undefined ? null : this.toViewModel(form);
    const equipmentIds = EquipmentService.getEquipmentIds(digimon.equipments);
    const rawEquipments = EquipmentRepository.getEquipmentsByIds(equipmentIds);
    const attributes = AttributesConverter.convert(
      digimon.attributes,
      activeDigievolution?.attributes ?? null,
      this.getAttributesEquipmentBonuses(rawEquipments),
    );

    return {
      attributes: this.applyBattleDeltas(attributes, digimon.inBattle, isInBattle),
      elements: ElementsConverter.convert(
        digimon.elements,
        activeDigievolution?.elements ?? null,
        this.getElementsEquipmentBonuses(rawEquipments),
      ),
    };
  }

  private static applyBattleDeltas(
    attributes: AttributesViewModel,
    inBattle: Digimon["inBattle"],
    isInBattle: boolean,
  ): AttributesViewModel {
    return {
      ...attributes,
      strength: { ...attributes.strength, fromBattle: isInBattle ? inBattle.strength : 0 },
      defense: { ...attributes.defense, fromBattle: isInBattle ? inBattle.defense : 0 },
      speed: { ...attributes.speed, fromBattle: isInBattle ? inBattle.speed : 0 },
    };
  }

  private static getAttributesEquipmentBonuses(
    rawEquipments: EquipmentRaw[],
  ): AttributesEquipmentBonuses {
    return {
      strength: EquipmentService.calculateBonus("strength", rawEquipments),
      defense: EquipmentService.calculateBonus("defense", rawEquipments),
      spirit: EquipmentService.calculateBonus("spirit", rawEquipments),
      wisdom: EquipmentService.calculateBonus("wisdom", rawEquipments),
      speed: EquipmentService.calculateBonus("speed", rawEquipments),
      charisma: EquipmentService.calculateBonus("charisma", rawEquipments),
    };
  }

  private static getElementsEquipmentBonuses(
    rawEquipments: EquipmentRaw[],
  ): ElementsEquipmentBonuses {
    return {
      fire: EquipmentService.calculateBonus("fire", rawEquipments),
      water: EquipmentService.calculateBonus("water", rawEquipments),
      ice: EquipmentService.calculateBonus("ice", rawEquipments),
      wind: EquipmentService.calculateBonus("wind", rawEquipments),
      thunder: EquipmentService.calculateBonus("thunder", rawEquipments),
      machine: EquipmentService.calculateBonus("machine", rawEquipments),
      dark: EquipmentService.calculateBonus("dark", rawEquipments),
    };
  }

  private static toViewModel(digievolutionRaw: DigievolutionRaw): DigievolutionViewModel {
    return {
      name: digimonName(digievolutionRaw.nameId),
      attributes: digievolutionRaw.attributes,
      elements: digievolutionRaw.elements,
      resistances: digievolutionRaw.resistances,
    };
  }
}
