package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.events.converters.journals.QuestConverter
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/JournalConverter.cs`. Reads the domain model, never changes it. */
object JournalConverter {
    fun toDto(journal: Journal): JournalDto = JournalDto(
        mainQuest = Optional.Present(QuestConverter.toDto(journal.mainQuest)),
        sideQuests = Optional.Present(journal.sideQuests.map(QuestConverter::toDto)),
        legendaryWeapons = Optional.Present(journal.legendaryWeapons.map(QuestConverter::toDto)),
        driAgents = Optional.Present(journal.driAgents.map(QuestConverter::toDto)),
        duelIsland = Optional.Present(journal.duelIsland.map(QuestConverter::toDto)),
    )
}
