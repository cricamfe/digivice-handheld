package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Domain/Assemblers/Journals/DuelIslandAssembler.cs`. */
object DuelIslandAssembler {
    fun assemble(resource: QuestResource): Quest {
        val quest = QuestAssembler.assemble(resource)
        normalizeProgression(quest)
        return quest
    }

    private fun normalizeProgression(quest: Quest) {
        if (quest.steps.isEmpty()) {
            return
        }

        if (!quest.requisites.all { it.isDone }) {
            for (step in quest.steps) {
                step.isDone = false
            }

            return
        }

        val trophyStep = quest.steps.last()
        if (!trophyStep.isDone) {
            return
        }

        for (i in 0 until quest.steps.size - 1) {
            if (!quest.steps[i].isDone) {
                quest.steps[i].isDone = true
            }
        }
    }
}
