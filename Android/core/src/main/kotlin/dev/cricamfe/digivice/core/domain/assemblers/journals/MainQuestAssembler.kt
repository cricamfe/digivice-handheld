package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Domain/Assemblers/Journals/MainQuestAssembler.cs`. */
object MainQuestAssembler {
    fun assemble(resource: QuestResource): Quest {
        val mainQuest = QuestAssembler.assemble(resource)

        // Completion cascade: if a later step is done, earlier unfinished steps must also be done.
        for (i in mainQuest.steps.size - 2 downTo 0) {
            if (!mainQuest.steps[i].isDone && mainQuest.steps[i + 1].isDone) {
                mainQuest.steps[i].isDone = true
            }
        }

        return mainQuest
    }
}
