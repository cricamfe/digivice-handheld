package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/InBattleConverterTests.cs`. */
class InBattleConverterTests {
    @Test
    fun `toDto maps condition vitals and attribute buffs`() {
        val digimonInBattle = InBattle(
            formId = 389,
            condition = 0x04,
            strength = 252,
            defense = 185,
            speed = 84,
            hp = Vital(current = 1400, max = 1850),
            mp = Vital(current = 900, max = 1140),
        )

        val dto = InBattleConverter.toDto(digimonInBattle)

        assertEquals(389, dto.formId.value)
        assertEquals(0x04, dto.condition.value)
        assertEquals(252, dto.strength.value)
        assertEquals(185, dto.defense.value)
        assertEquals(84, dto.speed.value)
        assertEquals(1400, dto.hp.value.current.value)
        assertEquals(1850, dto.hp.value.max.value)
        assertEquals(900, dto.mp.value.current.value)
        assertEquals(1140, dto.mp.value.max.value)
    }
}
