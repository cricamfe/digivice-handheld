/**
 * The quests the game would start now, by its own conditions. Only the Android core sends it (`QuestOffersChanged`), with the
 * full cycle, when the list changes.
 */
export interface QuestOffersDTO {
  quests: string[];
}
