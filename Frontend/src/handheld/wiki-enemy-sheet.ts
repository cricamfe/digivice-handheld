import type { State } from "@/models";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { WikiModalPresenter } from "@/presenters/map/wiki-modal.presenter";
import { WikiEnemyAttributesPresenter } from "@/presenters/map/wiki-modal/wiki-enemy-attributes.presenter";
import { WikiEnemyConditionsPresenter } from "@/presenters/map/wiki-modal/wiki-enemy-conditions.presenter";
import { WikiEnemyDropsPresenter } from "@/presenters/map/wiki-modal/wiki-enemy-drops.presenter";
import { WikiEnemyElementsPresenter } from "@/presenters/map/wiki-modal/wiki-enemy-elements.presenter";
import { WikiEnemyResumePresenter } from "@/presenters/map/wiki-modal/wiki-enemy-resume.presenter";
import { NpcBattleOpponentHelper } from "@/presenters/helper/npc-battle-opponent.helper";
import { EnemyRepository } from "@/repositories/enemy.repository";
import { EnemyAffinityService } from "@/services/enemy-affinity.service";
import { spriteUrl } from "./assets";
import {
  speciesIconOf,
  toCondition,
  toStat,
  type BattleCondition,
  type BattleStat,
} from "./battle-sheet";
import { attempt } from "./attempt";
import type { FigureDetail } from "./figure-detail";
import { EnemyTechniqueWords, fillWords } from "./game-words";
import { gameText, locationName, type Texts } from "./map-view";
import { dropIcon, dropName, dropTarget, npcTarget, type WikiTarget } from "./wiki-common";

export type FigureTone = "" | "up" | "down";

export interface EnemyFigure {
  key: string;
  label: string;
  /** What the figure says: the value, a percentage, Si or No. */
  text: string;
  tone: FigureTone;
  /** What a tap explains in a floating panel. */
  detail: FigureDetail;
}

export interface EnemyReward {
  exp: number;
  bits: number;
  dvexp: number;
  /** DVEXP each partner gains, null when it cannot be told (no active digievolution). */
  party: { name: string; sprite: string | null; gain: number | null }[];
}

export interface EnemyDrop {
  key: string;
  name: string;
  icon: string;
  /** "Solo en {place}" when the enemy drops it only there. */
  only: string | null;
  target: WikiTarget;
}

export interface EnemyPlace {
  id: string;
  name: string;
}

export interface WikiEnemySheet {
  id: string;
  name: string;
  level: number;
  isBoss: boolean;
  speciesIcon: string;
  speciesLabel: string;
  imageUrl: string | null;
  hp: number;
  /** Null when the data has no reward of its own (the Digimon of a tamer pay through the battle). */
  reward: EnemyReward | null;
  /** The game's word for a regular attack (the battle command), empty without its line. */
  attackLabel: string;
  /** The regular attack and the technique, in the game's language; null when the data has none. */
  attack: string | null;
  technique: string | null;
  figures: EnemyFigure[];
  places: EnemyPlace[];
  drops: EnemyDrop[];
  /** The tamer or NPC this Digimon belongs to, with the battle it is in. */
  owner: { name: string; target: WikiTarget } | null;
}

function stats(texts: Texts, enemy: ReturnType<typeof WikiModalPresenter.getEnemyById>) {
  const affinities = EnemyAffinityService.getAffinities(enemy.elements);
  const attributes = WikiEnemyAttributesPresenter.getStats(enemy.attributes).map((stat) =>
    toStat(stat, texts),
  );
  const elements = WikiEnemyElementsPresenter.getStats(enemy.elements).map((stat) => ({
    stat: toStat(stat, texts),
    tone: (affinities.effective.some((element) => element === stat.statKey)
      ? "up"
      : affinities.notEffective.some((element) => element === stat.statKey)
        ? "down"
        : "") as FigureTone,
  }));
  const conditions = WikiEnemyConditionsPresenter.getConditions(enemy.conditions).map((condition) =>
    toCondition(condition, texts),
  );

  return { attributes, elements, conditions };
}

function statFigure(stat: BattleStat, tone: FigureTone): EnemyFigure {
  return { key: stat.key, label: stat.label, text: String(stat.value), tone, detail: stat.detail };
}

/** The colour the original wiki gives a condition: green or red for Si and No, white or red for a chance. */
function conditionTone(condition: BattleCondition): FigureTone {
  if (!condition.hasChance) {
    return condition.can ? "up" : "down";
  }

  return condition.can ? "" : "down";
}

/** The sheet of an enemy, or null when the id is not in the data. */
export function buildEnemySheet(id: string, state: State, texts: Texts): WikiEnemySheet | null {
  if (EnemyRepository.getEnemyTable()[id] === undefined) {
    return null;
  }

  const enemy = WikiModalPresenter.getEnemyById(id);
  const { attributes, elements, conditions } = stats(texts, enemy);

  const hasReward =
    enemy.exp !== undefined && enemy.bits !== undefined && enemy.dvexp !== undefined;
  const party = hasReward
    ? attempt(() => WikiEnemyResumePresenter.getPartyDvexp(state.party, enemy.dvexp ?? 0), [])
    : [];

  const places = WikiEnemyResumePresenter.getAvailableEnemyLocations(
    enemy.locations,
    state.journal.mainQuest,
  ).map((location) => ({
    id: location.id,
    name: locationName(texts, location.id) ?? location.id,
  }));

  const drops = WikiEnemyDropsPresenter.getViewModel(enemy.drops).map((drop): EnemyDrop => {
    const name = dropName(texts, drop.labelKey, drop.dropId);
    return {
      key: `${drop.type}-${drop.dropId}-${drop.locationOnly ?? ""}`,
      name,
      icon: dropIcon(drop.type, drop.dropId),
      only:
        drop.locationOnly === undefined
          ? null
          : texts.t("enemy.locationOnly", {
              location: locationName(texts, drop.locationOnly) ?? drop.locationOnly,
            }),
      target: dropTarget(drop.type, drop.dropId),
    };
  });

  const npc = WikiModalPresenter.resolveNpcBattleFromEnemyId(id);
  const ownerKey = npc === null ? null : NpcBattleOpponentHelper.getNameKey(npc.npcId);
  const ownerName = ownerKey === null ? null : gameText(texts, ownerKey);

  return {
    id,
    name: enemy.name,
    level: enemy.level,
    isBoss: enemy.boss,
    speciesIcon: speciesIconOf(enemy.species, enemy.boss),
    speciesLabel: texts.t(`species.${enemy.species}`),
    imageUrl: ImageCatalog.getEnemyImageUrl(id),
    hp: enemy.hp,
    reward: hasReward
      ? {
          exp: enemy.exp ?? 0,
          bits: enemy.bits ?? 0,
          dvexp: enemy.dvexp ?? 0,
          party: party.map((member) => ({
            name: member.digimonName,
            sprite: spriteUrl(member.digimonId),
            gain: member.dvexp,
          })),
        }
      : null,
    attackLabel: gameText(texts, "enemy.regularAttack") ?? "",
    attack:
      enemy.regularAttackId === null
        ? null
        : gameText(texts, `regularAttacks.${enemy.regularAttackId}`),
    technique:
      enemy.techniqueId === null
        ? null
        : gameText(
            texts,
            `enemyTechniques.${enemy.techniqueId}`,
            fillWords(texts, EnemyTechniqueWords[enemy.techniqueId] ?? {}),
          ),
    figures: [
      ...attributes.map((stat) => statFigure(stat, "")),
      ...elements.map(({ stat, tone }) => statFigure(stat, tone)),
      ...conditions.map(
        (condition): EnemyFigure => ({
          key: condition.key,
          label: condition.label,
          text: condition.text,
          tone: conditionTone(condition),
          detail: condition.detail,
        }),
      ),
    ],
    places,
    drops,
    owner:
      npc === null || ownerName === null
        ? null
        : { name: ownerName, target: npcTarget(npc.npcId, npc.battleOptionId) },
  };
}
