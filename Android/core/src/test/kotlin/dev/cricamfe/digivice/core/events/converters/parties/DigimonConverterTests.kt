package dev.cricamfe.digivice.core.events.converters.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/DigimonConverterTests.cs`. */
class DigimonConverterTests {
    @Test
    fun `toDto maps the digimon and all nested structures`() {
        val digimon = Digimon(
            level = 42,
            tp = 20,
            blast = 850,
            experience = 123456,
            activeDigievolutionId = 5,
            hp = Vital(current = 101, max = 202),
            mp = Vital(current = 33, max = 44),
            inBattle = InBattle(
                condition = 0x04,
                strength = 252,
                defense = 185,
                speed = 84,
                hp = Vital(current = 90, max = 202),
                mp = Vital(current = 20, max = 44),
            ),
            attributes = Attributes(strength = 1, defense = 2, spirit = 3, wisdom = 4, speed = 5, charisma = 6),
            elements = Elements(fire = 7, water = 8, ice = 9, wind = 10, thunder = 11, machine = 12, dark = 13),
            equipments = Equipments(head = 14, body = 15, right = 16, left = 17, accessory1 = 18, accessory2 = 19),
            digievolutions = listOf(
                DigievolutionSlot(index = 1, digievolutionId = 5, digievolution = Digievolution(level = 9, dvexp = 200)),
            ),
            storedDigievolutions = listOf(
                StoredDigievolution(digievolutionId = 5, level = 9),
                StoredDigievolution(digievolutionId = 99, level = 3),
            ),
        )

        val dto = DigimonConverter.toDto(digimon)

        assertEquals(42, dto.level.value)
        assertEquals(20, dto.tp.value)
        assertEquals(850, dto.blast.value)
        assertEquals(123456, dto.experience.value)
        assertEquals(5, dto.activeDigievolutionId.value)
        assertEquals(101, dto.hp.value.current.value)
        assertEquals(202, dto.hp.value.max.value)
        assertEquals(33, dto.mp.value.current.value)
        assertEquals(44, dto.mp.value.max.value)
        assertEquals(0x04, dto.inBattle.value.condition.value)
        assertEquals(252, dto.inBattle.value.strength.value)
        assertEquals(185, dto.inBattle.value.defense.value)
        assertEquals(84, dto.inBattle.value.speed.value)
        assertEquals(90, dto.inBattle.value.hp.value.current.value)
        assertEquals(202, dto.inBattle.value.hp.value.max.value)
        assertEquals(20, dto.inBattle.value.mp.value.current.value)
        assertEquals(44, dto.inBattle.value.mp.value.max.value)
        assertEquals(1, dto.attributes.value.strength.value)
        assertEquals(4, dto.attributes.value.wisdom.value)
        assertEquals(13, dto.elements.value.dark.value)
        assertEquals(16, dto.equipments.value.right.value)
        val evolutionSlot = dto.digievolutions.value.single()
        assertEquals(1, evolutionSlot.index)
        assertEquals(5, evolutionSlot.digievolutionId.value)
        assertEquals(9, evolutionSlot.digievolution.value!!.level.value)
        assertEquals(200, evolutionSlot.digievolution.value!!.dvexp.value)
        assertEquals(2, dto.storedDigievolutions.value.size)
        assertEquals(99, dto.storedDigievolutions.value[1].digievolutionId.value)
        assertEquals(3, dto.storedDigievolutions.value[1].level.value)
    }
}
