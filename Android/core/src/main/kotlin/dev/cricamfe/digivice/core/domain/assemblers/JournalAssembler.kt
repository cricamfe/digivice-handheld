package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.assemblers.journals.DuelIslandAssembler
import dev.cricamfe.digivice.core.domain.assemblers.journals.MainQuestAssembler
import dev.cricamfe.digivice.core.domain.assemblers.journals.QuestAssembler
import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.memory.resources.JournalResource

/** Port of `Backend/Domain/Assemblers/JournalAssembler.cs`. */
object JournalAssembler {
    fun assemble(resource: JournalResource): Journal = Journal(
        mainQuest = MainQuestAssembler.assemble(resource.mainQuest),
        sideQuests = resource.sideQuests.map(QuestAssembler::assemble),
        legendaryWeapons = resource.legendaryWeapons.map(QuestAssembler::assemble),
        driAgents = resource.driAgents.map(QuestAssembler::assemble),
        duelIsland = resource.duelIsland.map(DuelIslandAssembler::assemble),
    )
}
