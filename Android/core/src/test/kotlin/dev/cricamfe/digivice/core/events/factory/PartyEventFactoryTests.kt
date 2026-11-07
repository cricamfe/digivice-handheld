package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.dto.PartyDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/PartyEventFactoryTests.cs`. */
class PartyEventFactoryTests {
    private fun state(party: Party) = State(party = party)

    private fun party(level: Int) = Party(
        slots = listOf(
            DigimonSlot(
                index = 1,
                digimonId = 1,
                digimon = Digimon(
                    level = level,
                    hp = Vital(),
                    mp = Vital(),
                    attributes = Attributes(),
                    elements = Elements(),
                    equipments = Equipments(),
                    digievolutions = emptyList(),
                ),
            ),
        ),
    )

    @Test
    fun `create returns no events when the party has no changes`() {
        assertTrue(PartyEventFactory.create(state(party(10)), state(party(10))).isEmpty())
    }

    @Test
    fun `create returns a party changed event when the party changes`() {
        val event = PartyEventFactory.create(state(party(10)), state(party(11))).single()

        assertEquals(EventType.PartyChanged, event.type)
        val dto = assertInstanceOf(PartyDto::class.java, event.payload)
        assertTrue(dto.slots.hasValue)
        assertEquals(1, dto.slots.value.size)
    }
}
