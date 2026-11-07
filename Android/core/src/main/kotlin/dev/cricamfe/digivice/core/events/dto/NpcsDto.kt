package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.npcs.NpcDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/NpcsDTO.cs`. */
@Serializable
data class NpcsDto(
    val genji: Optional<NpcDto> = Optional.Empty,
    val natsumi: Optional<NpcDto> = Optional.Empty,
    val catherine: Optional<NpcDto> = Optional.Empty,
    val lucia: Optional<NpcDto> = Optional.Empty,
    val robert: Optional<NpcDto> = Optional.Empty,
    val akiba: Optional<NpcDto> = Optional.Empty,
    val chris: Optional<NpcDto> = Optional.Empty,
    val tomomi: Optional<NpcDto> = Optional.Empty,
    val mitch: Optional<NpcDto> = Optional.Empty,
    val bob: Optional<NpcDto> = Optional.Empty,
    val andy: Optional<NpcDto> = Optional.Empty,
    val george: Optional<NpcDto> = Optional.Empty,
    val meiLin: Optional<NpcDto> = Optional.Empty,
    val jessica: Optional<NpcDto> = Optional.Empty,
    val gordon: Optional<NpcDto> = Optional.Empty,
    val alice: Optional<NpcDto> = Optional.Empty,
    val nakano: Optional<NpcDto> = Optional.Empty,
    val seiryuLeader: Optional<NpcDto> = Optional.Empty,
    val keith: Optional<NpcDto> = Optional.Empty,
    val suzakuLeader: Optional<NpcDto> = Optional.Empty,
    val fakeByakkoLeader: Optional<NpcDto> = Optional.Empty,
    val byakkoLeader: Optional<NpcDto> = Optional.Empty,
    val aoaAttacker: Optional<NpcDto> = Optional.Empty,
    val optionalTrooper1AdminCenterB1F: Optional<NpcDto> = Optional.Empty,
    val optionalTrooper2AdminCenterB1F: Optional<NpcDto> = Optional.Empty,
    val optionalTrooperAdminFloor1: Optional<NpcDto> = Optional.Empty,
    val optionalTrooper1AdminFloor2: Optional<NpcDto> = Optional.Empty,
    val optionalSoldier1AdminFloor2: Optional<NpcDto> = Optional.Empty,
    val optionalTrooper2AdminFloor2: Optional<NpcDto> = Optional.Empty,
) : IDto
