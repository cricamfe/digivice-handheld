package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/**
 * The payload of `PartnerTechniquesChanged`: for each partner of the party, the Digimon in its slots and their
 * `PartnerEntry.skills` as the RAM has them. It is not part of the state's diff; the loop sends it with the full cycle,
 * only when it changed. Not in the C# backend.
 */
@Serializable
data class PartnerTechniquesDto(val partners: List<PartnerTechniquesPartnerDto>) : IDto

@Serializable
data class PartnerTechniquesPartnerDto(val digimonId: Int, val forms: List<PartnerTechniquesFormDto>)

@Serializable
data class PartnerTechniquesFormDto(val id: Int, val skills: List<Int>)
