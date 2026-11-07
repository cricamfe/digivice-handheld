import { ImageCatalog } from "@/catalogs/image.catalog";
import type { DigimonCondition } from "@/constants/digimon-condition.constant";
import type { PartnerTechniquesPartnerDTO } from "@/events/dto/partner-techniques.dto";
import type { State } from "@/models";
import { DigimonBattlePresenter } from "@/presenters/map/digimon-battle.presenter";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { FieldPresenter } from "@/presenters/map/field.presenter";
import type { EnemyConditionViewModel } from "@/viewmodels/enemy/enemy-condition.viewmodel";
import type { EnemyStatViewModel } from "@/viewmodels/enemy/enemy-stat.viewmodel";
import {
  buildAffinities,
  buildBattleAttacks,
  type BattleAffinity,
  type BattleAttacks,
} from "./battle-attacks";
import { statDetail, type DetailRow, type FigureDetail } from "./figure-detail";
import { gameAssetUrl } from "./game-assets";
import { percentText } from "./party-view";
import { gameText, type Texts } from "./map-view";

export interface BattleStat {
  key: string;
  label: string;
  value: number;
  /** Change of the battle over the base value, 0 when there is none. */
  delta: number;
  detail: FigureDetail;
}

export interface BattleCondition {
  key: DigimonCondition;
  label: string;
  /** What the figure says: a percentage, Si or No. */
  text: string;
  /** Whether the party can inflict it. */
  can: boolean;
  /** Poison, paralysis, confusion, sleep and KO have a chance; the others are only Si or No. */
  hasChance: boolean;
  detail: FigureDetail;
}

/** What a field does to an element: the one it strengthens and the one it weakens. */
export interface BattleFieldEffect {
  element: string;
  /** `+50%` or `-25%`. */
  text: string;
  up: boolean;
}

export interface BattleField {
  name: string;
  /** The field's element, which names its icon and frame: `neutral` for a field without one. */
  kind: string;
  effects: BattleFieldEffect[];
}

export interface BattleSheet {
  /** False when the game has no enemy in memory, so only the field can be drawn. */
  hasEnemy: boolean;
  enemyId: string | null;
  name: string;
  isBoss: boolean;
  level: number | null;
  speciesIcon: string;
  speciesLabel: string | null;
  hp: { text: string; percent: string };
  /** The game's own picture of the fight stage, or null when there is none (the screen then fills flat). */
  stageImageUrl: string | null;
  enemyImageUrl: string | null;
  enemyAnimationUrl: string | null;
  field: BattleField;
  /** The elements the enemy is not neutral to, most hurtful first. */
  affinities: BattleAffinity[];
  attributes: BattleStat[];
  elements: BattleStat[];
  conditions: BattleCondition[];
  /** What the partner out in the fight can do to this enemy, best first. */
  attacks: BattleAttacks;
}

/** Icons of the kit for the species that have one; the rest use the generic enemy mark. */
const speciesIcons: Record<string, string> = { ghoul: "ghoul", machine: "machine" };
const GenericSpeciesIcon = "battle";

/** Kit icon for a species, or the boss mark for a boss. */
export function speciesIconOf(species: string | null, isBoss: boolean): string {
  return isBoss ? "boss" : (speciesIcons[species ?? ""] ?? GenericSpeciesIcon);
}

export function toStat(stat: EnemyStatViewModel, texts: Texts): BattleStat {
  const label = texts.t(`stat.${stat.statKey}`);
  const delta = stat.delta ?? 0;
  const base = stat.baseValue ?? stat.value;

  return {
    key: stat.statKey,
    label,
    value: stat.value,
    delta,
    detail: statDetail(texts.t, {
      key: stat.statKey,
      label,
      base,
      total: stat.value,
      changes: [{ label: texts.t("handheld.figure.fromBattle"), amount: delta }],
    }),
  };
}

export function toCondition(condition: EnemyConditionViewModel, texts: Texts): BattleCondition {
  const key = condition.conditionKey as DigimonCondition;
  const label = texts.t(`conditions.${key}.name`);
  // Poison, paralysis, confusion, sleep and KO have a chance; the others are Si or No.
  const hasChance = "value" in condition;
  const hasPercent =
    hasChance && condition.can && condition.value !== undefined && Number(condition.value) >= 0;
  const can = hasChance ? hasPercent : condition.can;
  const text = hasPercent
    ? `${condition.value}%`
    : texts.t(can ? "conditions.yes" : "conditions.no");

  const meaning = can
    ? texts.t(`conditions.${key}.canSuffer`)
    : texts.t(`handheld.condition.cannot.${key}`);
  const rows: DetailRow[] = hasPercent
    ? [{ label: texts.t("handheld.condition.chance"), value: text, tone: "" }]
    : [];

  return {
    key,
    label,
    text,
    can,
    hasChance,
    detail: { key, label, meaning, rows, verdict: can },
  };
}

export function buildBattleSheet(
  state: State,
  texts: Texts,
  mapId: string,
  partners: readonly PartnerTechniquesPartnerDTO[],
  slotIndex?: number,
  selectedForm?: number,
): BattleSheet {
  const { field: fieldId, enemy } = state.digimonBattle;
  const view = DigimonBattlePresenter.getViewModel(enemy);
  const field = FieldPresenter.getFieldViewModel(fieldId);
  const enemyRaw =
    enemy === null ? null : EnemyRepository.getEnemyByMemoryIdAndGroupId(enemy.id, enemy.groupId);

  // The percentages come from the field texts, which write them after the element.
  const percent = (key: string) => texts.t(key, { element: "" }).trim();
  const effects: BattleFieldEffect[] = [
    ...(field.strengthen === null
      ? []
      : [{ element: field.strengthen, text: percent("field.strengthen"), up: true }]),
    ...(field.weaken === null
      ? []
      : [{ element: field.weaken, text: percent("field.weaken"), up: false }]),
  ];

  const speciesIcon = speciesIconOf(view.species, view.isBoss);

  return {
    hasEnemy: enemy !== null,
    enemyId: view.enemyId,
    name: view.enemyName,
    isBoss: view.isBoss,
    level: view.level,
    speciesIcon,
    speciesLabel: view.species === null ? null : texts.t(`species.${view.species}`),
    hp: {
      text: `${view.hp.current}/${view.hp.max}`,
      percent: percentText(view.hp.current, view.hp.max),
    },
    stageImageUrl: ImageCatalog.getBattleStageImageUrl(state.digimonBattle.stage),
    enemyImageUrl: view.enemyImageUrl,
    enemyAnimationUrl: enemy === null ? null : gameAssetUrl("battle-enemies", String(enemy.id)),
    field: {
      // The field's name is the disc's (composed-messages.ts); without its line, the app's own word.
      name: gameText(texts, `field.${field.name}`) ?? texts.t("handheld.battle.field"),
      kind: field.name,
      effects,
    },
    affinities: enemyRaw === null ? [] : buildAffinities(enemyRaw),
    attributes: view.attributes.map((stat) => toStat(stat, texts)),
    elements: view.elements.map((stat) => toStat(stat, texts)),
    conditions: view.conditions.map((condition) => toCondition(condition, texts)),
    attacks: buildBattleAttacks(state, texts, mapId, partners, slotIndex, selectedForm),
  };
}
