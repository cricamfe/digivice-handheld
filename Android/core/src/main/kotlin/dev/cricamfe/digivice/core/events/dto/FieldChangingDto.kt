package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/** The game is closing a field or has not opened the next one; [to] is the map it leaves for, when known. Android only. */
@Serializable
data class FieldChangingDto(val changing: Boolean, val to: String? = null) : IDto
