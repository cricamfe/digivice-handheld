package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.DigimonBattle

/** Port of `Backend/Application/Providers/Interfaces/IDigimonBattleProvider.cs`. */
interface IDigimonBattleProvider {
    fun get(): DigimonBattle
}
