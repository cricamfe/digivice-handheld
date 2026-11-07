package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.events.converters.JournalConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changedItems
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.extensions.nested
import dev.cricamfe.digivice.core.events.diffing.journals.QuestDiffer
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.dto.journals.QuestDto

/** Port of `Backend/Events/Diffing/JournalDiffer.cs`. Only reads the journals. */
object JournalDiffer {
    fun diff(previousJournal: Journal?, newJournal: Journal): JournalDto {
        if (newJournal.hasNoChanges(previousJournal)) return JournalDto()
        if (previousJournal == null) return JournalConverter.toDto(newJournal)

        return JournalDto(
            mainQuest = nested(QuestDiffer.diff(previousJournal.mainQuest, newJournal.mainQuest)),
            sideQuests = changedItems(questsDelta(newJournal.sideQuests, previousJournal.sideQuests)),
            legendaryWeapons = changedItems(questsDelta(newJournal.legendaryWeapons, previousJournal.legendaryWeapons)),
            driAgents = changedItems(questsDelta(newJournal.driAgents, previousJournal.driAgents)),
            duelIsland = changedItems(questsDelta(newJournal.duelIsland, previousJournal.duelIsland)),
        )
    }

    private fun questsDelta(newQuests: List<Quest>, previousQuests: List<Quest>): List<QuestDto> =
        newQuests.mapNotNull { quest -> QuestDiffer.diff(previousQuests.firstOrNull { it.id == quest.id }, quest) }
}
