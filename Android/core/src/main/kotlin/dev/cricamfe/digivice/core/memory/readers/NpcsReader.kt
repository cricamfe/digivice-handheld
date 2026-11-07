package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.NpcAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcsAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.INpcsReader
import dev.cricamfe.digivice.core.memory.resources.NpcBattleResource
import dev.cricamfe.digivice.core.memory.resources.NpcResource
import dev.cricamfe.digivice.core.memory.resources.NpcsResource

/** Port of `Backend/Memory/Readers/NpcsReader.cs`. */
class NpcsReader(private val memoryReader: IMemoryReader) : INpcsReader {
    override fun read(addresses: NpcsAddresses): NpcsResource {
        return NpcsResource(
            genji = readNpc(addresses.genji),
            natsumi = readNpc(addresses.natsumi),
            catherine = readNpc(addresses.catherine),
            lucia = readNpc(addresses.lucia),
            robert = readNpc(addresses.robert),
            akiba = readNpc(addresses.akiba),
            chris = readNpc(addresses.chris),
            tomomi = readNpc(addresses.tomomi),
            mitch = readNpc(addresses.mitch),
            bob = readNpc(addresses.bob),
            andy = readNpc(addresses.andy),
            george = readNpc(addresses.george),
            meiLin = readNpc(addresses.meiLin),
            jessica = readNpc(addresses.jessica),
            gordon = readNpc(addresses.gordon),
            alice = readNpc(addresses.alice),
            nakano = readNpc(addresses.nakano),
            seiryuLeader = readNpc(addresses.seiryuLeader),
            keith = readNpc(addresses.keith),
            suzakuLeader = readNpc(addresses.suzakuLeader),
            fakeByakkoLeader = readNpc(addresses.fakeByakkoLeader),
            byakkoLeader = readNpc(addresses.byakkoLeader),
            aoaAttacker = readNpc(addresses.aoaAttacker),
            optionalTrooper1AdminCenterB1F = readNpc(addresses.optionalTrooper1AdminCenterB1F),
            optionalTrooper2AdminCenterB1F = readNpc(addresses.optionalTrooper2AdminCenterB1F),
            optionalTrooperAdminFloor1 = readNpc(addresses.optionalTrooperAdminFloor1),
            optionalTrooper1AdminFloor2 = readNpc(addresses.optionalTrooper1AdminFloor2),
            optionalSoldier1AdminFloor2 = readNpc(addresses.optionalSoldier1AdminFloor2),
            optionalTrooper2AdminFloor2 = readNpc(addresses.optionalTrooper2AdminFloor2),
        )
    }

    private fun readNpc(npcAddresses: NpcAddresses): NpcResource {
        return NpcResource(
            battles = readBattles(npcAddresses.battles),
        )
    }

    private fun readBattles(battles: Map<String, NpcBattleAddresses>): List<NpcBattleResource> {
        return battles.map { battle ->
            NpcBattleResource(
                id = battle.key,
                value = memoryReader.readByte(battle.value.address, battle.value.bitMask),
            )
        }
    }
}
