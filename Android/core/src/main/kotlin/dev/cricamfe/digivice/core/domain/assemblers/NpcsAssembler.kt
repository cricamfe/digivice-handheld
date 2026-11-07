package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.memory.resources.NpcBattleResource
import dev.cricamfe.digivice.core.memory.resources.NpcResource
import dev.cricamfe.digivice.core.memory.resources.NpcsResource

/** Port of `Backend/Domain/Assemblers/NpcsAssembler.cs`. */
object NpcsAssembler {
    fun assemble(resource: NpcsResource): Npcs = Npcs(
        genji = assembleNpc(resource.genji),
        natsumi = assembleNpc(resource.natsumi),
        catherine = assembleNpc(resource.catherine),
        lucia = assembleNpc(resource.lucia),
        robert = assembleNpc(resource.robert),
        akiba = assembleNpc(resource.akiba),
        chris = assembleNpc(resource.chris),
        tomomi = assembleNpc(resource.tomomi),
        mitch = assembleNpc(resource.mitch),
        bob = assembleNpc(resource.bob),
        andy = assembleNpc(resource.andy),
        george = assembleNpc(resource.george),
        meiLin = assembleNpc(resource.meiLin),
        jessica = assembleNpc(resource.jessica),
        gordon = assembleNpc(resource.gordon),
        alice = assembleNpc(resource.alice),
        nakano = assembleNpc(resource.nakano),
        seiryuLeader = assembleNpc(resource.seiryuLeader),
        keith = assembleNpc(resource.keith),
        suzakuLeader = assembleNpc(resource.suzakuLeader),
        fakeByakkoLeader = assembleNpc(resource.fakeByakkoLeader),
        byakkoLeader = assembleNpc(resource.byakkoLeader),
        aoaAttacker = assembleNpc(resource.aoaAttacker),
        optionalTrooper1AdminCenterB1F = assembleNpc(resource.optionalTrooper1AdminCenterB1F),
        optionalTrooper2AdminCenterB1F = assembleNpc(resource.optionalTrooper2AdminCenterB1F),
        optionalTrooperAdminFloor1 = assembleNpc(resource.optionalTrooperAdminFloor1),
        optionalTrooper1AdminFloor2 = assembleNpc(resource.optionalTrooper1AdminFloor2),
        optionalSoldier1AdminFloor2 = assembleNpc(resource.optionalSoldier1AdminFloor2),
        optionalTrooper2AdminFloor2 = assembleNpc(resource.optionalTrooper2AdminFloor2),
    )

    private fun assembleNpc(npcResource: NpcResource): Npc = Npc(
        battles = assembleBattles(npcResource.battles),
    )

    private fun assembleBattles(battleResources: List<NpcBattleResource>): List<NpcBattle> =
        battleResources.map { battleResource ->
            NpcBattle(
                id = battleResource.id,
                won = battleResource.value.toInt() != 0,
            )
        }
}
