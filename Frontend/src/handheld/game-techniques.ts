import { gameAssetUrl } from "./game-assets";
import { language, Languages } from "./language";

interface Technique {
  mp: number;
  power: number;
  type: "physical" | "magical" | null;
  element: string;
  elementStrength: number;
}

interface Form {
  nameId: number;
  signature: number;
}

interface Words {
  names: string[];
  descriptions: string[];
  forms: string[];
}

interface BattleCatalog {
  techniques: Technique[];
  forms: Record<number, Form>;
  partners: number[];
  words: Record<string, Words>;
}

let catalog: BattleCatalog | null = null;

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function uint(value: unknown, max: number): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value >= 0 && value <= max;
}

function stringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((text) => typeof text === "string");
}

function isCatalog(value: unknown): value is BattleCatalog {
  if (!record(value)) return false;
  const { techniques, forms, partners, words } = value;
  if (
    !Array.isArray(techniques) ||
    techniques.length !== 443 ||
    !techniques.every(
      (row) =>
        record(row) &&
        uint(row.mp, 65535) &&
        uint(row.power, 65535) &&
        (row.type === null || row.type === "physical" || row.type === "magical") &&
        typeof row.element === "string" &&
        uint(row.elementStrength, 255),
    )
  )
    return false;
  if (
    !record(forms) ||
    Object.keys(forms).length !== 55 ||
    !Object.entries(forms).every(
      ([id, form]) =>
        /^\d{1,5}$/.test(id) &&
        record(form) &&
        uint(form.nameId, 255) &&
        uint(form.signature, techniques.length),
    )
  )
    return false;
  if (
    !Array.isArray(partners) ||
    partners.length !== 8 ||
    new Set(partners).size !== 8 ||
    !partners.every((id) => uint(id, 65535) && Object.hasOwn(forms, id))
  )
    return false;
  const lastName = Math.max(...Object.values(forms).map((form) => (form as Form).nameId));
  return (
    record(words) &&
    Languages.every(({ locale }) => {
      const lang = words[locale];
      return (
        record(lang) &&
        stringArray(lang.names) &&
        lang.names.length === techniques.length &&
        stringArray(lang.descriptions) &&
        lang.descriptions.length === techniques.length &&
        stringArray(lang.forms) &&
        lang.forms.length > lastName
      );
    })
  );
}

export async function loadBattleCatalog(): Promise<boolean> {
  catalog = null;
  const url = gameAssetUrl("tables", "battle");
  if (url === null) return false;
  try {
    const response = await fetch(url);
    if (!response.ok) return false;
    const value: unknown = await response.json();
    if (!isCatalog(value)) return false;
    catalog = value;
    return true;
  } catch {
    return false;
  }
}

export function battleTechnique(id: number) {
  const data = catalog?.techniques[id - 1];
  if (!data || !catalog) return undefined;
  const english = catalog.words["en-US"]!;
  const selected = catalog.words[language.value] ?? english;
  return {
    ...data,
    name: selected.names[id - 1] || english.names[id - 1] || String(id),
    description: selected.descriptions[id - 1] || english.descriptions[id - 1] || null,
  };
}

export function battleForm(id: number) {
  const form = catalog?.forms[id];
  if (!form || !catalog) return undefined;
  const english = catalog.words["en-US"]!;
  return {
    signature: form.signature || undefined,
    name: catalog.words[language.value]?.forms[form.nameId] || english.forms[form.nameId],
  };
}

export function ownBattleForm(partnerId: number): number | undefined {
  return catalog?.partners[partnerId];
}

/** Used by the development snapshots, which store English labels in place of RAM ids. */
export function englishTechniqueIds(): Map<string, number> {
  return new Map(catalog?.words["en-US"]?.names.map((name, index) => [name, index + 1]));
}
