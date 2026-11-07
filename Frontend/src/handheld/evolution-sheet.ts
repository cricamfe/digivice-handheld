import type { ComposerTranslation } from "vue-i18n";
import { createLogger } from "@/events/logger";
import { ImageCatalog } from "@/catalogs/image.catalog";
import { DigimonAttributes } from "@/constants/digimon-attribute.constant";
import { DigimonElements } from "@/constants/digimon-element.constant";
import { DvexpBarSegments } from "@/constants/digievolution.constant";
import type { Attributes, Digimon, Elements } from "@/models";
import { DigievolutionDvexpPresenter } from "@/presenters/digievolution/digievolution-dvexp.presenter";
import { DigievolutionTechniquesPresenter } from "@/presenters/digievolution/digievolution-techniques.presenter";
import { DigievolutionPresenter } from "@/presenters/digievolution/digievolution.presenter";
import { DigievolutionsModalPresenter } from "@/presenters/digievolution/digievolutions-modal.presenter";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";
import { NodePresenter } from "@/presenters/tree/node.presenter";
import { TreePresenter } from "@/presenters/tree/tree.presenter";
import { DigievolutionRepository } from "@/repositories/digievolution.repository";
import type { TechniqueType } from "@/types/technique-type.type";
import type { NodeViewModel } from "@/viewmodels/digievolution/node.viewmodel";
import type { RequirementViewModel } from "@/viewmodels/digimon/requirement.viewmodel";
import { gameText, type Texts } from "./map-view";

const logger = createLogger("EvolutionSheet", "#e67e22");

export interface TreeRequirement {
  text: string;
  isMet: boolean;
}

export interface TreeNode {
  id: number;
  name: string;
  /** Null when the game files have no art of the digievolution. */
  imageUrl: string | null;
  isUnlocked: boolean;
  /** At most two, like the original tree. */
  requirements: TreeRequirement[];
  /** Names of the nodes that follow this one in its family line. */
  next: string[];
}

/** A family of the tree: a line, or a line that forks into branches. */
export interface TreeFamily {
  key: string;
  /** The nodes before the fork (the whole line when the family does not fork). */
  line: TreeNode[];
  /** Empty when the family does not fork. */
  branches: TreeNode[][];
}

export interface EvolutionTree {
  digimonName: string;
  families: TreeFamily[];
}

function buildNode(
  digimon: Digimon,
  digimonName: string,
  node: NodeViewModel,
  t: ComposerTranslation,
): TreeNode {
  return {
    id: node.id,
    name: node.name,
    imageUrl: ImageCatalog.getDigievolutionImageUrl(node.id),
    isUnlocked: NodePresenter.checkRequirements(digimon, node),
    requirements: node.requirements.slice(0, 2).map((requirement) => ({
      text: NodePresenter.getRequirementText(digimonName, requirement, t),
      isMet: NodePresenter.isRequirementMet(digimon, requirement),
    })),
    next: (node.next === null ? [] : [node.next].flat()).map((id) =>
      DigievolutionPresenter.getNameById(id),
    ),
  };
}

/** The digievolution tree of a partner, with what the original tree draws for each node. */
export function buildEvolutionTree(
  digimon: Digimon,
  digimonId: number,
  t: ComposerTranslation,
): EvolutionTree {
  const digimonName = ProfilePresenter.getName(digimonId);
  const build = (node: NodeViewModel) => buildNode(digimon, digimonName, node, t);
  const families = TreePresenter.getDigievolutionsTree(digimonId, null).families.map(
    (family): TreeFamily => {
      // The original tree draws the fork only when it has a shared start and more than one branch.
      const forks = family.nodesBeforeFork.length > 0 && family.branchs.length > 1;
      return forks
        ? {
            key: family.key,
            line: family.nodesBeforeFork.map(build),
            branches: family.branchs.map((branch) => branch.map(build)),
          }
        : { key: family.key, line: (family.branchs[0] ?? []).map(build), branches: [] };
    },
  );

  return { digimonName, families };
}

/**
 * The tree of a partner, or null when it cannot be built. The failure is logged, so a bug in a
 * presenter does not pass for a partner without data; the screen shows its "no data" state.
 */
export function tryBuildEvolutionTree(
  digimon: Digimon,
  digimonId: number,
  t: ComposerTranslation,
): EvolutionTree | null {
  try {
    return buildEvolutionTree(digimon, digimonId, t);
  } catch (error) {
    logger.error(`The digievolution tree of Digimon ${digimonId} could not be built.`, error);
    return null;
  }
}

/** Id of the first node of the tree, the fallback when no digievolution is active. */
export function firstNodeId(tree: EvolutionTree): number | null {
  const family = tree.families[0];
  return family?.line[0]?.id ?? null;
}

/** A node of the tree by digievolution id, or null when the tree has no such node. */
export function findNode(tree: EvolutionTree, id: number): TreeNode | null {
  for (const family of tree.families) {
    const node = [...family.line, ...family.branches.flat()].find((entry) => entry.id === id);
    if (node !== undefined) {
      return node;
    }
  }

  return null;
}

/** One requirement of a digievolution, with where the partner stands against it. */
export interface PanelRequirement {
  /** What the original tree writes for it ("Greymon Nv.50", "Fuerza >= 280"). */
  text: string;
  isMet: boolean;
  /** What the partner has now (0 for a digievolution it has not obtained). */
  current: number;
  required: number;
}

/** What a digievolution adds to one attribute or element while the partner fights in it. */
export interface PanelBonus {
  /** Kit icon, which is also the name of the stat. */
  key: string;
  label: string;
  value: number;
}

/** Everything the floating panel of a node tells about its digievolution. */
export interface NodePanel {
  id: number;
  name: string;
  imageUrl: string | null;
  isObtained: boolean;
  /** Null while it has not been obtained. */
  level: number | null;
  /** Filled segments of the DV bar out of `DvexpBarSegments`; null when the partner does not wear it in a slot. */
  filled: number | null;
  /** It is the form the partner starts a battle in. */
  startsBattle: boolean;
  requirements: PanelRequirement[];
  attributes: PanelBonus[];
  elements: PanelBonus[];
  /** The digievolutions that follow this one. */
  next: string[];
}

/** What the partner has now for a requirement: the same lookups `NodePresenter.isRequirementMet` compares. */
function currentFor(digimon: Digimon, requirement: RequirementViewModel): number {
  const stat = requirement.stat?.toLowerCase();
  switch (requirement.type) {
    case "DigimonLevel":
      return digimon.level;
    case "Attribute":
      return digimon.attributes[stat as keyof Attributes] ?? 0;
    case "Element":
      return digimon.elements[stat as keyof Elements] ?? 0;
    case "DigievolutionLevel":
      return (
        digimon.storedDigievolutions.find(
          (stored) => stored.digievolutionId === requirement.digievolution,
        )?.level ?? 0
      );
    default:
      return 0;
  }
}

function bonusesOf<Key extends string>(
  keys: readonly Key[],
  values: Partial<Record<Key, number>>,
  t: ComposerTranslation,
): PanelBonus[] {
  return keys
    .filter((key) => (values[key] ?? 0) !== 0)
    .map((key) => ({ key, label: t(`stat.${key}`), value: values[key]! }));
}

/**
 * The panel of a node: its state in this partner, every requirement with the value it has against the one it asks for
 * (the rules are the tree's own, `NodePresenter`), and what the digievolution gives.
 */
export function buildNodePanel(
  tree: EvolutionTree,
  node: TreeNode,
  digimon: Digimon,
  digimonId: number,
  t: ComposerTranslation,
): NodePanel {
  const level = digievolutionLevel(digimon, node.id);
  const worn = digimon.digievolutions.find((entry) => entry.digievolutionId === node.id);
  const raw = DigievolutionRepository.findRawDigievolutionById(node.id);
  const digimonName = ProfilePresenter.getName(digimonId);
  const derivatives = DigievolutionTechniquesPresenter.getViewModel(node.id, {
    digimonId,
    showTreeSections: true,
  }).derivativeDigievolutions.map((link) => link.name);

  return {
    id: node.id,
    name: node.name,
    imageUrl: node.imageUrl,
    isObtained: level !== null,
    level,
    filled:
      worn?.digievolution == null
        ? null
        : Math.min(
            DvexpBarSegments,
            DigievolutionDvexpPresenter.getFilledSegments(
              node.id,
              worn.digievolution.level,
              worn.digievolution.dvexp,
            ),
          ),
    startsBattle: digimon.activeDigievolutionId === node.id,
    requirements: DigievolutionsModalPresenter.getDigievolutionRequirements(digimonId, node.id).map(
      (requirement) => ({
        text: NodePresenter.getRequirementText(digimonName, requirement, t),
        isMet: NodePresenter.isRequirementMet(digimon, requirement),
        current: currentFor(digimon, requirement),
        required: requirement.value,
      }),
    ),
    attributes: bonusesOf(DigimonAttributes, raw?.attributes ?? {}, t),
    elements: bonusesOf(DigimonElements, raw?.elements ?? {}, t),
    next: followingNames(tree, node.id, derivatives),
  };
}

export interface TechniqueRow {
  id: string;
  name: string;
  description: string;
  type: TechniqueType;
  typeLabel: string;
  /** Kit icon of the type of technique. */
  typeIcon: string;
  elementLabel: string;
  /** Kit icon of the element (the cross for the techniques without one). */
  elementIcon: string;
  mp: number;
  learnAt: number;
  /** Null for the signature technique (and for one with no load level). */
  loadAt: number | null;
  isSignature: boolean;
  isUnlocked: boolean;
}

export interface TechniqueSheet {
  name: string;
  imageUrl: string | null;
  /** Level of the digievolution in this Digimon, or null while it has not been obtained. */
  level: number | null;
  /** Digievolutions this one leads to. */
  next: string[];
  techniques: TechniqueRow[];
}

/** The kit has no icon for healing or support techniques, so these borrow the closest ones. */
const iconOfType: Record<TechniqueType, string> = {
  physical: "strength",
  magical: "spirit",
  heal: "charisma",
  support: "defense",
  field: "field",
};

/**
 * The digievolutions that follow one: the next ones of its family line in the tree (the design's
 * "Sigue: Guardiangemon") and then the ones that ask for it as a requirement (what the original panel
 * lists as "next digievolutions"), without repeating any.
 */
function followingNames(
  tree: EvolutionTree,
  digievolutionId: number,
  derivatives: string[],
): string[] {
  const fromTree = findNode(tree, digievolutionId)?.next ?? [];
  return [...new Set([...fromTree, ...derivatives])];
}

/** Level of a digievolution in a Digimon, from its slots or its stored digievolutions. */
export function digievolutionLevel(digimon: Digimon, digievolutionId: number): number | null {
  const stored = digimon.storedDigievolutions.find(
    (entry) => entry.digievolutionId === digievolutionId,
  );
  if (stored !== undefined) {
    return stored.level;
  }

  const slot = digimon.digievolutions.find((entry) => entry.digievolutionId === digievolutionId);
  return slot?.digievolution?.level ?? null;
}

/** The techniques of a digievolution as the original panel lists them. */
export function buildTechniqueSheet(
  tree: EvolutionTree,
  digimon: Digimon,
  digimonId: number,
  digievolutionId: number,
  texts: Texts,
): TechniqueSheet {
  const level = digievolutionLevel(digimon, digievolutionId);
  const model = DigievolutionTechniquesPresenter.getViewModel(digievolutionId, {
    digimonId,
    // A digievolution the Digimon does not have has taught it nothing yet, so every row shows as locked.
    digievolutionLevel: level ?? 0,
    showTreeSections: true,
  });

  return {
    name: model.evolutionName,
    imageUrl: ImageCatalog.getDigievolutionImageUrl(digievolutionId),
    level,
    next: followingNames(
      tree,
      digievolutionId,
      model.derivativeDigievolutions.map((link) => link.name),
    ),
    techniques: model.techniques.map((technique) => {
      const element = technique.element.toLowerCase();
      const hasElement = element !== "none";
      return {
        id: technique.id,
        name: gameText(texts, `technique.${technique.id}.name`) ?? technique.id,
        description: gameText(texts, `technique.${technique.id}.description`) ?? "",
        type: technique.type,
        typeLabel: texts.t(`techniqueTypes.${technique.type}`),
        typeIcon: iconOfType[technique.type],
        elementLabel: hasElement
          ? texts.t(`stat.${element}`)
          : texts.t("handheld.techniques.noElement"),
        elementIcon: hasElement ? element : "cross",
        mp: technique.mp,
        learnAt: technique.learnAt,
        loadAt: technique.loadAt,
        isSignature: technique.isSignature,
        isUnlocked: technique.isUnlocked,
      };
    }),
  };
}
