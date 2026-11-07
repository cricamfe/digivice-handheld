package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/DigimonBattleAddresses.cs`. */
@Serializable
data class DigimonBattleAddresses(
    @SerialName("Field")
    @Serializable(with = HexStringToLongConverter::class)
    val field: Long = 0L,
    /** How much the field boosts its element, in 128ths (`ElementBoost.amount`). */
    @SerialName("FieldAmount")
    @Serializable(with = HexStringToLongConverter::class)
    val fieldAmount: Long = 0L,
    /** The player's fighter in the field, 0 to 2 (`BattleState.active[0]`). */
    @SerialName("ActiveFighter")
    @Serializable(with = HexStringToLongConverter::class)
    val activeFighter: Long = 0L,
    /** The fight stage the battle starts on (`BATTLE_SETUP.stage`), an index of SFSTDATA's table. */
    @SerialName("Stage")
    @Serializable(with = HexStringToLongConverter::class)
    val stage: Long = 0L,
)
