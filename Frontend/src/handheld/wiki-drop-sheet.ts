import type { State } from "@/models";
import { WikiModalPresenter } from "@/presenters/map/wiki-modal.presenter";
import { WikiEnemyResumePresenter } from "@/presenters/map/wiki-modal/wiki-enemy-resume.presenter";
import { WikiDropBoosterPresenter } from "@/presenters/map/wiki-modal/wiki-drop-booster.presenter";
import { WikiDropConsumableItemPresenter } from "@/presenters/map/wiki-modal/wiki-drop-consumable-item.presenter";
import { WikiDropEquipmentPresenter } from "@/presenters/map/wiki-modal/wiki-drop-equipment.presenter";
import { WikiDropsPanelPresenter } from "@/presenters/map/wiki-modal/wiki-drops-panel.presenter";
import { DigimonRepository } from "@/repositories/digimon.repository";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { EquipmentRepository } from "@/repositories/equipment.repository";
import type { DropType } from "@/repositories/tables/raws/drop/drop-type";
import { attempt } from "./attempt";
import { equipmentKindWord } from "./game-words";
import { gameText, locationName, type Texts } from "./map-view";
import { portraitUrl } from "./assets";
import { cardRef, type WikiCardRef } from "./wiki-card-sheet";
import { enemyTarget, itemIcon, npcTarget, type WikiTarget } from "./wiki-common";

/** Something that gives the drop: a Digimon (wild, boss, a tamer's) or a tamer or a duel island. */
export interface DropSource {
  key: string;
  name: string;
  /** Picture of the enemy or of the NPC, null when the game files have none. */
  imageUrl: string | null;
  /** The level of the enemy, null for an NPC. */
  level: number | null;
  hp: number | null;
  isBoss: boolean;
  /** "Solo en {place}" when it drops the thing only there. */
  only: string | null;
  /** First place it can be found at in the story so far, null when none is known. */
  place: string | null;
  target: WikiTarget;
}

/** Who gives the drop, from the original presenter, with what the sheets need to draw each one. */
function sourcesOf(id: number, type: DropType, state: State, texts: Texts): DropSource[] {
  const view = WikiDropsPanelPresenter.getViewModel(String(id), type);

  return view.sources.map((source): DropSource => {
    const key = `${source.kind}-${source.sourceId}-${source.locationId ?? ""}`;
    const only =
      source.locationId === undefined
        ? null
        : texts.t("enemy.locationOnly", {
            location: locationName(texts, source.locationId) ?? source.locationId,
          });

    if (source.kind !== "enemy") {
      return {
        key,
        name: gameText(texts, source.labelKey ?? "") ?? source.sourceId,
        imageUrl: source.iconUrl,
        level: null,
        hp: null,
        isBoss: false,
        only,
        place: null,
        target: npcTarget(source.sourceId),
      };
    }

    const enemy = WikiModalPresenter.getEnemyById(source.sourceId);
    const places = WikiEnemyResumePresenter.getAvailableEnemyLocations(
      enemy.locations,
      state.journal.mainQuest,
    );
    const first = places[0];

    return {
      key,
      name: source.label ?? enemy.name,
      imageUrl: source.iconUrl,
      level: enemy.level,
      hp: enemy.hp,
      isBoss: EnemyRepository.getEnemyById(source.sourceId).boss === true,
      only,
      place: first === undefined ? null : (locationName(texts, first.id) ?? first.id),
      target: enemyTarget(source.sourceId),
    };
  });
}

export interface EquipmentAttribute {
  key: string;
  label: string;
  /** Like `+12` or `-4`. */
  value: string;
  isPenalty: boolean;
}

export interface WikiEquipmentSheet {
  id: number;
  name: string;
  icon: string;
  typeLabel: string;
  /** What the game says about it, null when the data has no note. */
  note: string | null;
  attributes: EquipmentAttribute[];
  /** Names of the partners that can wear it, with the portrait of each. */
  equipable: { id: number; name: string; portrait: string | null }[];
  /** Every partner can wear it (the original wiki says "all Digimon" then). */
  forAll: boolean;
  sources: DropSource[];
}

/** The sheet of a piece of equipment, or null when the id is not in the data. */
export function buildEquipmentSheet(
  id: number,
  state: State,
  texts: Texts,
): WikiEquipmentSheet | null {
  if (EquipmentRepository.getById(id) === undefined) {
    return null;
  }

  const view = WikiDropEquipmentPresenter.getViewModel(id);
  const note = gameText(texts, `equipments.${id}.note`);

  return {
    id,
    name: gameText(texts, `equipments.${id}.name`) ?? `#${id}`,
    icon: itemIcon(id),
    typeLabel: equipmentKindWord((key) => texts.t(key), view.type),
    note: note === null || note === "" ? null : note,
    attributes: view.attributes.map((attribute) => ({
      key: attribute.attribute,
      label: texts.t(`stat.${attribute.attribute.toLowerCase()}`),
      value: `${attribute.type}${attribute.value}`,
      isPenalty: attribute.type !== "+",
    })),
    equipable: view.equipableDigimonIds.map((id) => ({
      id: Number(id),
      name: DigimonRepository.getNameById(Number(id)),
      portrait: portraitUrl(Number(id)),
    })),
    forAll: view.equipableDigimonIds.length === DigimonRepository.getIds().length,
    sources: sourcesOf(id, "equipment", state, texts),
  };
}

export interface WikiItemSheet {
  id: number;
  name: string;
  icon: string;
  resale: number;
  soldInStore: boolean;
  /** What the game says about it, null when the data has none (it has none for any object today). */
  note: string | null;
  sources: DropSource[];
}

/** The sheet of an object (chip, charge...), or null when the id is not in the data. */
export function buildItemSheet(id: number, state: State, texts: Texts): WikiItemSheet | null {
  const view = attempt(() => WikiDropConsumableItemPresenter.getViewModel(id), null);
  if (view === null) {
    return null;
  }

  const name = gameText(texts, view.nameKey) ?? `#${id}`;
  const parts = [
    gameText(texts, view.noteKey),
    gameText(texts, view.additionalInformationKey),
  ].filter((part): part is string => part !== null && part !== "");

  return {
    id,
    name,
    icon: itemIcon(id),
    resale: view.resaleValue,
    soldInStore: view.soldInStore,
    note: parts.length === 0 ? null : parts.join(" "),
    sources: sourcesOf(id, "consumableItem", state, texts),
  };
}

export interface WikiBoosterSheet {
  id: number;
  name: string;
  cards: WikiCardRef[];
  sources: DropSource[];
}

/** The sheet of a booster, or null when it has no cards in the data. */
export function buildBoosterSheet(id: number, state: State, texts: Texts): WikiBoosterSheet | null {
  const cards = WikiDropBoosterPresenter.getViewModel(id);
  const name = gameText(texts, `boosters.${id}.name`);
  if (cards.length === 0 && name === null) {
    return null;
  }

  return {
    id,
    name: name ?? `#${id}`,
    cards: cards.map((card) => cardRef(card.cardId, texts)),
    sources: sourcesOf(id, "booster", state, texts),
  };
}
