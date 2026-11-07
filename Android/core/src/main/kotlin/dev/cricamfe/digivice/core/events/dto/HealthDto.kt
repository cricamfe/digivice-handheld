package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.models.HealthStatus
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/HealthDTO.cs`. It has no optional fields: the two error fields go out as `null`. */
@Serializable
data class HealthDto(
    val status: HealthStatus,
    @EncodeDefault val errorCode: String? = null,
    @EncodeDefault val errorDetail: String? = null,
) : IDto
