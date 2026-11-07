package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.events.converters.NpcsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.extensions.nested
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.dto.npcs.NpcDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/NpcsDiffer.cs`. Only reads the npcs; the 29 npcs go out in the order of the dto. */
object NpcsDiffer {
    fun diff(previousNpcs: Npcs?, newNpcs: Npcs): NpcsDto {
        if (newNpcs.hasNoChanges(previousNpcs)) return NpcsDto()
        if (previousNpcs == null) return NpcsConverter.toDto(newNpcs)

        return NpcsDto(
            genji = npcDelta(previousNpcs.genji, newNpcs.genji),
            natsumi = npcDelta(previousNpcs.natsumi, newNpcs.natsumi),
            catherine = npcDelta(previousNpcs.catherine, newNpcs.catherine),
            lucia = npcDelta(previousNpcs.lucia, newNpcs.lucia),
            robert = npcDelta(previousNpcs.robert, newNpcs.robert),
            akiba = npcDelta(previousNpcs.akiba, newNpcs.akiba),
            chris = npcDelta(previousNpcs.chris, newNpcs.chris),
            tomomi = npcDelta(previousNpcs.tomomi, newNpcs.tomomi),
            mitch = npcDelta(previousNpcs.mitch, newNpcs.mitch),
            bob = npcDelta(previousNpcs.bob, newNpcs.bob),
            andy = npcDelta(previousNpcs.andy, newNpcs.andy),
            george = npcDelta(previousNpcs.george, newNpcs.george),
            meiLin = npcDelta(previousNpcs.meiLin, newNpcs.meiLin),
            jessica = npcDelta(previousNpcs.jessica, newNpcs.jessica),
            gordon = npcDelta(previousNpcs.gordon, newNpcs.gordon),
            alice = npcDelta(previousNpcs.alice, newNpcs.alice),
            nakano = npcDelta(previousNpcs.nakano, newNpcs.nakano),
            seiryuLeader = npcDelta(previousNpcs.seiryuLeader, newNpcs.seiryuLeader),
            keith = npcDelta(previousNpcs.keith, newNpcs.keith),
            suzakuLeader = npcDelta(previousNpcs.suzakuLeader, newNpcs.suzakuLeader),
            fakeByakkoLeader = npcDelta(previousNpcs.fakeByakkoLeader, newNpcs.fakeByakkoLeader),
            byakkoLeader = npcDelta(previousNpcs.byakkoLeader, newNpcs.byakkoLeader),
            aoaAttacker = npcDelta(previousNpcs.aoaAttacker, newNpcs.aoaAttacker),
            optionalTrooper1AdminCenterB1F = npcDelta(previousNpcs.optionalTrooper1AdminCenterB1F, newNpcs.optionalTrooper1AdminCenterB1F),
            optionalTrooper2AdminCenterB1F = npcDelta(previousNpcs.optionalTrooper2AdminCenterB1F, newNpcs.optionalTrooper2AdminCenterB1F),
            optionalTrooperAdminFloor1 = npcDelta(previousNpcs.optionalTrooperAdminFloor1, newNpcs.optionalTrooperAdminFloor1),
            optionalTrooper1AdminFloor2 = npcDelta(previousNpcs.optionalTrooper1AdminFloor2, newNpcs.optionalTrooper1AdminFloor2),
            optionalSoldier1AdminFloor2 = npcDelta(previousNpcs.optionalSoldier1AdminFloor2, newNpcs.optionalSoldier1AdminFloor2),
            optionalTrooper2AdminFloor2 = npcDelta(previousNpcs.optionalTrooper2AdminFloor2, newNpcs.optionalTrooper2AdminFloor2),
        )
    }

    private fun npcDelta(previousNpc: Npc, newNpc: Npc): Optional<NpcDto> = nested(NpcDiffer.diff(previousNpc, newNpc))
}
