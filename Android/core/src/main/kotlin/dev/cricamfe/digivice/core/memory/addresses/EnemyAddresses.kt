package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.VitalAddresses
import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/EnemyAddresses.cs`. */
@Serializable
data class EnemyAddresses(
    @SerialName("EnemySlotBase")
    @Serializable(with = HexStringToLongConverter::class)
    val enemySlotBase: Long = 0L,
    @SerialName("SlotStride")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val slotStride: Int = 0,
    @SerialName("SlotCount")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val slotCount: Int = 0,
    @SerialName("ActiveEnemySlotIndex")
    @Serializable(with = HexStringToLongConverter::class)
    val activeEnemySlotIndex: Long = 0L,
    @SerialName("GroupId")
    @Serializable(with = HexStringToLongConverter::class)
    val groupId: Long = 0L,
    @SerialName("Id")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val id: Int = 0,
    @SerialName("Condition")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val condition: Int = 0,
    @SerialName("Strength")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val strength: Int = 0,
    @SerialName("Defense")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val defense: Int = 0,
    @SerialName("Speed")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val speed: Int = 0,
    @SerialName("HP")
    val hp: VitalAddresses = VitalAddresses(),
)
