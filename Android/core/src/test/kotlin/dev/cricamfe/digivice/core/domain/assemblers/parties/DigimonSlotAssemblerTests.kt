package dev.cricamfe.digivice.core.domain.assemblers.parties

import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.AttributesResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.ElementsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.EquipmentsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DigimonSlotAssemblerTests {
    @Test
    fun `assemble should map all fields correctly`() {
        val resource = DigimonSlotResource(
            index = 2,
            digimonId = 8,
            digimonResource = DigimonResource(
                experience = 200,
                level = 5,
                hp = VitalResource(),
                mp = VitalResource(),
                attributes = AttributesResource(),
                elements = ElementsResource(),
                equipments = EquipmentsResource(),
                digievolutions = emptyList(),
            ),
        )

        val result = DigimonSlotAssembler.assemble(resource)

        assertEquals(2, result.index)
        assertEquals(8, result.digimonId)
        assertNotNull(result.digimon)
        assertEquals(5, result.digimon!!.level)
    }

    @Test
    fun `assemble should keep kotemon as occupied slot when digimon id is zero`() {
        val resource = DigimonSlotResource(
            index = 1,
            digimonId = 0,
            digimonResource = DigimonResource(level = 3),
        )

        val result = DigimonSlotAssembler.assemble(resource)

        assertEquals(0, result.digimonId)
        assertNotNull(result.digimon)
        assertEquals(3, result.digimon!!.level)
    }

    @ParameterizedTest
    @ValueSource(ints = [0xFF, 42])
    fun `assemble should return empty slot when digimon resource is null`(rawDigimonId: Int) {
        val resource = DigimonSlotResource(index = 2, digimonId = rawDigimonId, digimonResource = null)

        val result = DigimonSlotAssembler.assemble(resource)

        assertEquals(2, result.index)
        assertNull(result.digimonId)
        assertNull(result.digimon)
    }
}
