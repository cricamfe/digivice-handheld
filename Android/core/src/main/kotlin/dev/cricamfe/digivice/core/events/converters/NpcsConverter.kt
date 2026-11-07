package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.dto.npcs.NpcBattleDto
import dev.cricamfe.digivice.core.events.dto.npcs.NpcDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/NpcsConverter.cs`. Reads the domain model, never changes it. */
object NpcsConverter {
    fun toDto(npcs: Npcs): NpcsDto = NpcsDto(
        genji = Optional.Present(toNpcDto(npcs.genji)),
        natsumi = Optional.Present(toNpcDto(npcs.natsumi)),
        catherine = Optional.Present(toNpcDto(npcs.catherine)),
        lucia = Optional.Present(toNpcDto(npcs.lucia)),
        robert = Optional.Present(toNpcDto(npcs.robert)),
        akiba = Optional.Present(toNpcDto(npcs.akiba)),
        chris = Optional.Present(toNpcDto(npcs.chris)),
        tomomi = Optional.Present(toNpcDto(npcs.tomomi)),
        mitch = Optional.Present(toNpcDto(npcs.mitch)),
        bob = Optional.Present(toNpcDto(npcs.bob)),
        andy = Optional.Present(toNpcDto(npcs.andy)),
        george = Optional.Present(toNpcDto(npcs.george)),
        meiLin = Optional.Present(toNpcDto(npcs.meiLin)),
        jessica = Optional.Present(toNpcDto(npcs.jessica)),
        gordon = Optional.Present(toNpcDto(npcs.gordon)),
        alice = Optional.Present(toNpcDto(npcs.alice)),
        nakano = Optional.Present(toNpcDto(npcs.nakano)),
        seiryuLeader = Optional.Present(toNpcDto(npcs.seiryuLeader)),
        keith = Optional.Present(toNpcDto(npcs.keith)),
        suzakuLeader = Optional.Present(toNpcDto(npcs.suzakuLeader)),
        fakeByakkoLeader = Optional.Present(toNpcDto(npcs.fakeByakkoLeader)),
        byakkoLeader = Optional.Present(toNpcDto(npcs.byakkoLeader)),
        aoaAttacker = Optional.Present(toNpcDto(npcs.aoaAttacker)),
        optionalTrooper1AdminCenterB1F = Optional.Present(toNpcDto(npcs.optionalTrooper1AdminCenterB1F)),
        optionalTrooper2AdminCenterB1F = Optional.Present(toNpcDto(npcs.optionalTrooper2AdminCenterB1F)),
        optionalTrooperAdminFloor1 = Optional.Present(toNpcDto(npcs.optionalTrooperAdminFloor1)),
        optionalTrooper1AdminFloor2 = Optional.Present(toNpcDto(npcs.optionalTrooper1AdminFloor2)),
        optionalSoldier1AdminFloor2 = Optional.Present(toNpcDto(npcs.optionalSoldier1AdminFloor2)),
        optionalTrooper2AdminFloor2 = Optional.Present(toNpcDto(npcs.optionalTrooper2AdminFloor2)),
    )

    fun toNpcDto(npc: Npc): NpcDto = NpcDto(
        battles = Optional.Present(npc.battles.map(::toBattleDto)),
    )

    fun toBattleDto(battle: NpcBattle): NpcBattleDto = NpcBattleDto(
        id = battle.id,
        won = Optional.Present(battle.won),
    )
}
