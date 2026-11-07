import { gameWord } from "./language";
import type { Texts } from "./map-view";
import { questText } from "./quest-words";

/**
 * The steps of the quests whose target is a Digimon, by the line of the game's Digimon names that names it
 * (`dignam:213`), read from the disc; the quests of the original app wrote the name in their text.
 */
export const QuestTargetWords: Readonly<Record<string, string>> = {
  "amaterasuRematch.steps.1": "dignam:92",
  "amaterasuRematch.steps.2": "dignam:92",
  "amaterasuRematch.steps.3": "dignam:92",
  "amaterasuRematch.steps.4": "dignam:92",
  "amaterasuRematch.steps.5": "dignam:156",
  "amaterasuRematch.steps.6": "dignam:157",
  "amaterasuTrophy.steps.1": "dignam:92",
  "amaterasuTrophy.steps.2": "dignam:92",
  "amaterasuTrophy.steps.3": "dignam:92",
  "amaterasuTrophy.steps.4": "dignam:92",
  "amaterasuTrophy.steps.5": "dignam:156",
  "amaterasuTrophy.steps.6": "dignam:157",
  "asukaRematch.steps.1": "dignam:88",
  "asukaRematch.steps.2": "dignam:88",
  "asukaRematch.steps.3": "dignam:88",
  "asukaRematch.steps.4": "dignam:88",
  "asukaRematch.steps.5": "dignam:88",
  "asukaTrophy.steps.1": "dignam:88",
  "asukaTrophy.steps.2": "dignam:88",
  "asukaTrophy.steps.3": "dignam:88",
  "asukaTrophy.steps.4": "dignam:88",
  "asukaTrophy.steps.5": "dignam:88",
  "driAgentAgumon.steps.2": "dignam:23",
  "driAgentGuilmon.steps.2": "dignam:26",
  "driAgentKotemon.steps.2": "dignam:20",
  "driAgentKumamon.steps.2": "dignam:22",
  "driAgentMonmon.steps.2": "dignam:21",
  "driAgentPatamon.steps.2": "dignam:28",
  "driAgentRenamon.steps.2": "dignam:27",
  "driAgentVeemon.steps.2": "dignam:25",
  "eternally.steps.1": "dignam:212",
  "fishingPole.steps.2": "dignam:88",
  "folderBag.steps.1": "dignam:88",
  "invincible.steps.1": "dignam:213",
  "mainQuest.steps.3": "dignam:213",
  "mainQuest.steps.8": "dignam:6",
  "mainQuest.steps.9": "dignam:6",
  "mainQuest.steps.10": "dignam:6",
  "mainQuest.steps.12": "dignam:6",
  "mainQuest.steps.15": "dignam:214",
  "mainQuest.steps.18": "dignam:70",
  "mainQuest.steps.20": "dignam:92",
  "mainQuest.steps.22": "dignam:92",
  "mainQuest.steps.23": "dignam:92",
  "mainQuest.steps.25": "dignam:214",
  "mainQuest.steps.31": "dignam:92",
  "mainQuest.steps.40": "dignam:70",
  "mainQuest.steps.41": "dignam:70",
  "mainQuest.steps.44": "dignam:105",
  "mainQuest.steps.50": "dignam:105",
  "mainQuest.steps.53": "dignam:105",
  "mainQuest.steps.55": "dignam:105",
  "mainQuest.steps.57": "dignam:105",
  "mainQuest.steps.59": "dignam:105",
  "mainQuest.steps.60": "dignam:215",
  "mainQuest.steps.61": "dignam:105",
  "mainQuest.steps.62": "dignam:217",
  "mainQuest.steps.66": "dignam:217",
  "mainQuest.steps.79": "dignam:248",
  "mainQuest.steps.91": "dignam:8",
  "mainQuest.steps.93": "dignam:219",
  "mainQuest.steps.122": "dignam:248",
  "mainQuest.steps.133": "dignam:225",
  "muramasa.steps.1": "dignam:214",
  "punishment.steps.1": "dignam:215",
  "superNova.steps.1": "dignam:217",
  "treeBoots.steps.5": "dignam:5",
  "treeBoots.steps.6": "dignam:5",
  "treeBoots.steps.7": "dignam:5",
};

/**
 * Whom or what a step of a quest points at, as the journal names it: a Digimon by its name on the disc, anything else
 * by the quest's text. Null when neither has it (or the extracted text lacks the Digimon's line).
 */
export function questTarget(texts: Texts, questId: string, step: number | string): string | null {
  const word = QuestTargetWords[`${questId}.steps.${step}`];
  if (word !== undefined) {
    return gameWord(word, "") || null;
  }

  return questText(texts, `${questId}.steps.${step}.locationTarget`);
}
