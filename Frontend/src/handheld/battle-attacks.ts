import { DigimonElements, type DigimonElement } from "@/constants/digimon-element.constant";
import type { PartnerTechniquesPartnerDTO } from "@/events/dto/partner-techniques.dto";
import type { Digimon, State } from "@/models";
import { StatsPresenter } from "@/presenters/party/digimon/stats.presenter";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { FieldRepository } from "@/repositories/field.repository";
import type { EnemyRaw } from "@/repositories/tables/raws/enemy/enemy.raw";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";
import type { StatViewModel } from "@/viewmodels/digimon/stat.viewmodel";
import { gameText, type Texts } from "./map-view";
import { battleForm, battleTechnique, ownBattleForm } from "./game-techniques";

/**
 * How well an element works against the enemy, as in Pokemon: from the element and the enemy's resistance to it (and
 * the field's boost of that element), never from the attack's power.
 */
export type AttackGrade = "veryEffective" | "effective" | "normal" | "weak" | "veryWeak";

/**
 * The cut points, best first: a grade starts at `from`, the element's multiplier next to a neutral enemy
 * (resistance 100). They come from the multipliers the game's magic formula gives (`FIGHTSTG_computeMagicDamage`)
 * for the 2,100 element values of `enemy.json`: 60 for 14 % of them (2.20); 90 to 117 for 68 % (1.30 down to 0.96, one
 * continuous cluster, all Normal); 160 to 260 for 12 % (0.85 to 0.60); 265 to 435 for 6 % (0.59 to 0.44). The two real
 * gaps are between 0.85 and 0.96 and between 1.30 and 2.20, so Normal starts at 0.9 and the top of the cluster is 1.3:
 * Muy eficaz starts at 1.75 and Eficaz at 1.4, which no enemy reaches alone (the field's boost can: 1.0 with half a
 * boost is 1.5). The split of the resisted at 0.6 is a round number, not a gap. No value in the game gives no damage
 * (the lowest multiplier is 0.44), so there is no grade for it.
 */
export const GradeCuts: readonly { grade: AttackGrade; from: number }[] = [
  { grade: "veryEffective", from: 1.75 },
  { grade: "effective", from: 1.4 },
  { grade: "normal", from: 0.9 },
  { grade: "weak", from: 0.6 },
  { grade: "veryWeak", from: 0 },
];

/** The order of the grades, best first, for sorting. */
const gradeRank = (grade: AttackGrade): number =>
  GradeCuts.length - GradeCuts.findIndex((cut) => cut.grade === grade);

export function gradeOf(multiplier: number): AttackGrade {
  return GradeCuts.find((cut) => multiplier >= cut.from)?.grade ?? "veryWeak";
}

/**
 * What an element does against an enemy resistance, next to a neutral 100, with the field's boost of the element
 * (`amount` in 128ths, 0 when it is not the boosted one): the game's magic multiplier.
 */
export function elementScale(resist: number | null, boost = 0): number {
  if (resist === null) {
    return 1;
  }

  return (magicFactor(Math.max(1, resist)) / magicFactor(100)) * (1 + boost / 128);
}

/** One attack of the partner the player has out, with what it would do to the enemy on the screen. */
export interface BattleAttack {
  key: string;
  name: string;
  /** Null for a technique without an element. */
  element: DigimonElement | null;
  mp: number;
  /** The partner has the MP for it now (the game greys out the rest of its menu). */
  usable: boolean;
  /** Damage of one hit, from the game's formulas (see `estimateDamage`). */
  damage: number;
  /** What its element does against this enemy and the field, next to a neutral enemy: 1 for no element. */
  multiplier: number;
  grade: AttackGrade;
  /** The best the partner can use now. */
  isBest: boolean;
  type: "physical" | "magical";
  power: number;
  /** What the game's text says the technique does, when it has any. */
  description: string | null;
}

export interface BattleAttacks {
  /** The partner the list is for (the form it fights as, or its own name), or null when there is none. */
  fighterName: string | null;
  /** The party slot (`index`) the list is for. */
  slotIndex: number;
  formId: number | null;
  forms: { id: number; name: string; live: boolean }[];
  stats: { key: string; value: number }[];
  /** Best first; the ones it has not the MP for come last. */
  attacks: BattleAttack[];
}

function statTotal(stat: StatViewModel): number {
  return stat.sumBetweenDigimonAndEquipaments + stat.fromDigievolution + (stat.fromBattle ?? 0);
}

/**
 * The damage of one hit, as the game works it out (FIGHTSTG_computeDamage, FIGHTSTG_computeMagicDamage and
 * FIGHTSTG_adjustDamage, in dw3_decomp, ReGame-Labs, MIT), with its integer divisions. It leaves out what is not known
 * from here: the critical hit, the family bonus, the charge, a weapon's or accessory's element and the field's
 * weakening (the game's own table of which element weakens which does not agree with the app's).
 */
export function estimateDamage(input: {
  type: "physical" | "magical";
  power: number;
  elementPower: number;
  attack: number;
  spirit: number;
  enemyDefense: number;
  enemySpirit: number;
  /** The enemy's resistance to the technique's element, or null for a technique without one. */
  resist: number | null;
  /** The field boosts the technique's element, by this much in 128ths (0 when it does not). */
  boost: number;
}): number {
  const { type, power } = input;
  const base =
    type === "physical"
      ? Math.trunc((power * input.attack) / Math.max(1, input.enemyDefense))
      : magicPower(power, input.spirit, input.enemySpirit);

  let result = base;
  result += Math.trunc((result * input.boost) / 128);
  if (input.resist !== null) {
    const resist = Math.max(1, input.resist);
    if (type === "physical") {
      result += Math.trunc((result * input.elementPower * 2) / resist);
    } else {
      result = Math.trunc((result * magicFactor(resist)) / 100);
    }
  }

  // At most five times the hit's base value (the physical value, or the magic power scaled by spirit), and 9999 in
  // this version.
  result = Math.min(result, base * 5, 9999);
  return Math.max(0, result);
}

/** What a magic technique's element does to the damage, in hundredths, against a resistance. */
function magicFactor(resist: number): number {
  if (resist < 100) {
    return 400 - resist * 3;
  }

  return resist >= 300 ? 65 - Math.trunc(resist / 20) : 125 - Math.trunc(resist / 4);
}

/** A magic technique's power from the user's spirit against the target's: from half to twice as much. */
function magicPower(power: number, spirit: number, enemySpirit: number): number {
  const scaled = Math.trunc(
    (power * (Math.trunc((spirit * 50) / Math.max(1, enemySpirit)) + 50)) / 100,
  );
  return Math.min(power * 2, Math.max(Math.trunc(power / 2), scaled));
}

function elementOfTechnique(element: string): DigimonElement | null {
  return DigimonElements.find((candidate) => candidate === element) ?? null;
}

/** What one element does to the enemy on the screen, on the same scale as an attack. */
export interface BattleAffinity {
  element: DigimonElement;
  /** What the element does next to a neutral enemy. */
  multiplier: number;
  grade: AttackGrade;
}

/**
 * The elements the enemy is not neutral to, from the most to the least they hurt it, on the same scale as the
 * attacks (`elementScale`).
 */
export function buildAffinities(enemy: EnemyRaw): BattleAffinity[] {
  return DigimonElements.flatMap((element): BattleAffinity[] => {
    const multiplier = elementScale(enemy[element]);
    const grade = gradeOf(multiplier);
    if (grade === "normal") {
      return [];
    }

    return [{ element, multiplier, grade }];
  }).sort((a, b) => b.multiplier - a.multiplier);
}

/** The partner in a party slot (`index`, from 1), or null when the slot holds none. */
function partyDigimon(
  state: State,
  slotIndex: number,
): { digimon: Digimon; digimonId: number } | null {
  const slot = state.party.slots.find((entry) => entry.index === slotIndex);
  return slot?.digimon == null || slot.digimonId === null
    ? null
    : { digimon: slot.digimon, digimonId: slot.digimonId };
}

const SkillId = 0x1fff;
const SkillMarked = 0x4000;

/**
 * The numbers of the techniques a partner has in a form, as the game's battle menu lists them (`FIGHTSTG_updateTechMenu`,
 * `listSlotTechs`), from what the RAM has: in its own form, its signature technique alone; in one of its slots, what that
 * entry has, then what the other entries pass on (the ones marked in the lab). A form that is in no slot is a temporary
 * one, a blast, which has its own signature too.
 */
function techniquesOf(
  digimonId: number,
  formId: number,
  partners: readonly PartnerTechniquesPartnerDTO[],
): number[] {
  const signature = battleForm(formId)?.signature;
  if (formId === ownBattleForm(digimonId)) {
    return signature === undefined ? [] : [signature];
  }

  const forms = partners.find((partner) => partner.digimonId === digimonId)?.forms ?? [];
  const own = forms.find((form) => form.id === formId);
  const list = (own?.skills ?? []).filter((skill) => skill !== 0).map((skill) => skill & SkillId);
  if (own === undefined && signature !== undefined) {
    list.push(signature);
  }

  for (const form of forms) {
    if (form !== own) {
      for (const skill of form.skills) {
        if ((skill & SkillMarked) !== 0 && !list.includes(skill & SkillId)) {
          list.push(skill & SkillId);
        }
      }
    }
  }
  return list;
}

/**
 * The attacks use the form currently fighting, or preview the bench partner's configured evolution.
 * The game offers a separate choice of forms when actually switching in.
 */
export function buildBattleAttacks(
  state: State,
  texts: Texts,
  mapId: string,
  partners: readonly PartnerTechniquesPartnerDTO[],
  slotIndex: number = state.digimonBattle.activeFighter + 1,
  selectedForm?: number,
): BattleAttacks {
  const { enemy, field, fieldAmount, activeFighter } = state.digimonBattle;
  const out = partyDigimon(state, slotIndex);
  const enemyRaw =
    enemy === null ? null : EnemyRepository.getEnemyByMemoryIdAndGroupId(enemy.id, enemy.groupId);
  if (out === null || enemy === null) {
    return { fighterName: null, slotIndex, formId: null, forms: [], stats: [], attacks: [] };
  }

  const { digimon, digimonId } = out;
  const baseForm = ownBattleForm(digimonId);
  const liveForm = slotIndex === activeFighter + 1 ? digimon.inBattle.formId : 0;
  const equipped = digimon.digievolutions.flatMap(({ digievolutionId }) =>
    digievolutionId !== null && digievolutionId >= 3 ? [digievolutionId] : [],
  );
  const nameOf = (id: number) =>
    battleForm(id)?.name ??
    (DigievolutionRepository.findRawDigievolutionById(id) === undefined
      ? undefined
      : DigievolutionRepository.getNameById(id)) ??
    ProfilePresenter.getName(digimonId);
  const forms = Array.from(new Set([baseForm ?? 0, ...equipped, liveForm]))
    .filter((id) => id > 0)
    .map((id) => ({ id, name: nameOf(id), live: id === liveForm }));
  const configured = digimon.activeDigievolutionId;
  const defaultForm =
    liveForm ||
    (configured !== null && equipped.includes(configured) ? configured : (baseForm ?? 0));
  const formId =
    selectedForm !== undefined && forms.some(({ id }) => id === selectedForm)
      ? selectedForm
      : defaultForm;
  const fighterName = nameOf(formId);
  const preview = { ...digimon, inBattle: { ...digimon.inBattle, formId } };
  const { attributes, elements } = StatsPresenter.getStatsViewModel(preview, mapId);
  const stats = Object.entries({ ...attributes, ...elements }).map(([key, value]) => ({
    key,
    value: statTotal(value),
  }));
  const fighter = { fighterName, slotIndex, formId, forms, stats };
  if (enemyRaw === null) {
    return { ...fighter, attacks: [] };
  }
  const techniques = techniquesOf(digimonId, formId, partners);
  const attack = statTotal(attributes.strength);
  const spirit = statTotal(attributes.spirit);
  const enemyDefense = enemyRaw.defense + enemy.defense;
  const boosted = FieldRepository.getFieldById(field)?.strengthens ?? null;
  const mp = digimon.inBattle.mp.current;

  const toAttack = (
    key: string,
    name: string,
    description: string | null,
    technique: {
      type: "physical" | "magical";
      element: string;
      elementStrength: number;
      mp: number;
      power: number;
    },
  ): BattleAttack => {
    const element = elementOfTechnique(technique.element);
    const resist = element === null ? null : resistOf(enemyRaw, element);
    const boost = element !== null && element === boosted ? fieldAmount : 0;
    const multiplier = elementScale(resist, boost);
    return {
      key,
      name,
      element,
      mp: technique.mp,
      usable: technique.mp <= mp,
      damage: estimateDamage({
        type: technique.type,
        power: technique.power,
        elementPower: technique.elementStrength,
        attack,
        spirit,
        enemyDefense,
        enemySpirit: enemyRaw.spirit,
        resist,
        boost,
      }),
      multiplier,
      grade: gradeOf(multiplier),
      isBest: false,
      type: technique.type,
      power: technique.power,
      description,
    };
  };

  const attacks = techniques.flatMap((number): BattleAttack[] => {
    const technique = battleTechnique(number);
    if (technique === undefined || technique.type === null) {
      return [];
    }

    return [
      toAttack(String(number), technique.name, technique.description, {
        ...technique,
        type: technique.type,
      }),
    ];
  });

  // What the player can use first, then the better grade, then the harder hit within a grade.
  attacks.sort(
    (a, b) =>
      Number(b.usable) - Number(a.usable) ||
      gradeRank(b.grade) - gradeRank(a.grade) ||
      b.damage - a.damage,
  );
  if (attacks[0]?.usable) {
    attacks[0].isBest = true;
  }
  return {
    ...fighter,
    attacks,
  };
}

function resistOf(enemy: EnemyRaw, element: DigimonElement): number {
  return enemy[element];
}
