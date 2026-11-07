package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/PlayerAddresses.cs`. */
@Serializable
data class PlayerAddresses(
    @SerialName("Bits")
    @Serializable(with = HexStringToLongConverter::class)
    val bits: Long = 0L,
    @SerialName("MapId")
    @Serializable(with = HexStringToLongConverter::class)
    val mapId: Long = 0L,
    @SerialName("PreviousMapId")
    @Serializable(with = HexStringToLongConverter::class)
    val previousMapId: Long = 0L,
    @SerialName("SeabedRoute")
    @Serializable(with = HexStringToLongConverter::class)
    val seabedRoute: Long = 0L,
    @SerialName("MapVariant")
    @Serializable(with = HexStringToLongConverter::class)
    val mapVariant: Long = 0L,
    /** The game's task registry: 100 pointers to the tasks that other code finds by id (`TASK_REGISTRY.tasks`). */
    @SerialName("TaskRegistry")
    @Serializable(with = HexStringToLongConverter::class)
    val taskRegistry: Long = 0L,
    /** Offsets in an actor (a field character) of its position in pixels: `Actor.tile`. */
    @SerialName("ActorTileX")
    @Serializable(with = HexStringToLongConverter::class)
    val actorTileX: Long = 0L,
    @SerialName("ActorTileY")
    @Serializable(with = HexStringToLongConverter::class)
    val actorTileY: Long = 0L,
    /** The direction an actor faces (0 to 7) and the file of its animations, which is 0 for one that is not drawn. */
    @SerialName("ActorDir")
    @Serializable(with = HexStringToLongConverter::class)
    val actorDir: Long = 0L,
    @SerialName("ActorAnimFile")
    @Serializable(with = HexStringToLongConverter::class)
    val actorAnimFile: Long = 0L,
)
