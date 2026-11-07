package dev.cricamfe.digivice.core.events.diffing.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.journals.quests.RequisiteDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/Journals/Quests/RequisiteDiffer.cs`. Only reads the requisites. */
object RequisiteDiffer {
    fun diff(previousRequisite: Requisite?, newRequisite: Requisite): RequisiteDto? {
        if (newRequisite.hasNoChanges(previousRequisite)) return null

        // A requisite that is new, or whose flag changed, goes out with its id; any other difference is none (the id
        // is the key it was matched by).
        return if (previousRequisite == null || previousRequisite.isDone != newRequisite.isDone) {
            RequisiteDto(id = newRequisite.id, isDone = Optional.Present(newRequisite.isDone))
        } else {
            null
        }
    }
}
