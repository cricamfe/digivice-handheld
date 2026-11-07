package dev.cricamfe.digivice.core.memory.addresses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/NpcsAddresses.cs`. */
@Serializable
data class NpcsAddresses(
    @SerialName("Genji")
    val genji: NpcAddresses = NpcAddresses(),
    @SerialName("Natsumi")
    val natsumi: NpcAddresses = NpcAddresses(),
    @SerialName("Catherine")
    val catherine: NpcAddresses = NpcAddresses(),
    @SerialName("Lucia")
    val lucia: NpcAddresses = NpcAddresses(),
    @SerialName("Robert")
    val robert: NpcAddresses = NpcAddresses(),
    @SerialName("Akiba")
    val akiba: NpcAddresses = NpcAddresses(),
    @SerialName("Chris")
    val chris: NpcAddresses = NpcAddresses(),
    @SerialName("Tomomi")
    val tomomi: NpcAddresses = NpcAddresses(),
    @SerialName("Mitch")
    val mitch: NpcAddresses = NpcAddresses(),
    @SerialName("Bob")
    val bob: NpcAddresses = NpcAddresses(),
    @SerialName("Andy")
    val andy: NpcAddresses = NpcAddresses(),
    @SerialName("George")
    val george: NpcAddresses = NpcAddresses(),
    @SerialName("MeiLin")
    val meiLin: NpcAddresses = NpcAddresses(),
    @SerialName("Jessica")
    val jessica: NpcAddresses = NpcAddresses(),
    @SerialName("Gordon")
    val gordon: NpcAddresses = NpcAddresses(),
    @SerialName("Alice")
    val alice: NpcAddresses = NpcAddresses(),
    @SerialName("Nakano")
    val nakano: NpcAddresses = NpcAddresses(),
    @SerialName("SeiryuLeader")
    val seiryuLeader: NpcAddresses = NpcAddresses(),
    @SerialName("Keith")
    val keith: NpcAddresses = NpcAddresses(),
    @SerialName("SuzakuLeader")
    val suzakuLeader: NpcAddresses = NpcAddresses(),
    @SerialName("FakeByakkoLeader")
    val fakeByakkoLeader: NpcAddresses = NpcAddresses(),
    @SerialName("ByakkoLeader")
    val byakkoLeader: NpcAddresses = NpcAddresses(),
    @SerialName("AoaAttacker")
    val aoaAttacker: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalTrooper1AdminCenterB1F")
    val optionalTrooper1AdminCenterB1F: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalTrooper2AdminCenterB1F")
    val optionalTrooper2AdminCenterB1F: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalTrooperAdminFloor1")
    val optionalTrooperAdminFloor1: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalTrooper1AdminFloor2")
    val optionalTrooper1AdminFloor2: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalSoldier1AdminFloor2")
    val optionalSoldier1AdminFloor2: NpcAddresses = NpcAddresses(),
    @SerialName("OptionalTrooper2AdminFloor2")
    val optionalTrooper2AdminFloor2: NpcAddresses = NpcAddresses(),
)
