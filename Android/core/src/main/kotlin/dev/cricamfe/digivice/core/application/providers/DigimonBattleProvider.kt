package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IDigimonBattleLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IDigimonBattleProvider
import dev.cricamfe.digivice.core.domain.assemblers.DigimonBattleAssembler
import dev.cricamfe.digivice.core.domain.models.DigimonBattle

/** Port of `Backend/Application/Providers/DigimonBattleProvider.cs`. */
class DigimonBattleProvider(private val digimonBattleLoader: IDigimonBattleLoader) : IDigimonBattleProvider {
    override fun get(): DigimonBattle = DigimonBattleAssembler.assemble(digimonBattleLoader.load())
}
