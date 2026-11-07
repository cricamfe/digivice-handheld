package dev.cricamfe.digivice.core.domain.assemblers.parties

import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.resources.parties.InBattleResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.AttributesResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.ElementsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.EquipmentsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DigimonAssemblerTests {
    @ParameterizedTest
    @CsvSource("0,", "-1,", "5,5")
    fun `assemble should sanitize active digievolution id`(inputId: Int, expectedId: Int?) {
        val resource = createBaseDigimonResource().copy(activeDigievolutionId = inputId)

        val result = DigimonAssembler.assemble(resource)

        assertEquals(expectedId, result.activeDigievolutionId)
    }

    @ParameterizedTest
    @CsvSource("0,", "-1,", "101,101")
    fun `assemble should sanitize equipment ids`(rawEquipmentId: Int, expectedEquipmentId: Int?) {
        val resource = createBaseDigimonResource().copy(
            equipments = EquipmentsResource(
                head = rawEquipmentId,
                body = rawEquipmentId,
                right = rawEquipmentId,
                left = rawEquipmentId,
                accessory1 = rawEquipmentId,
                accessory2 = rawEquipmentId,
            ),
        )

        val result = DigimonAssembler.assemble(resource)

        assertEquals(expectedEquipmentId, result.equipments.head)
        assertEquals(expectedEquipmentId, result.equipments.body)
        assertEquals(expectedEquipmentId, result.equipments.right)
        assertEquals(expectedEquipmentId, result.equipments.left)
        assertEquals(expectedEquipmentId, result.equipments.accessory1)
        assertEquals(expectedEquipmentId, result.equipments.accessory2)
    }

    @Test
    fun `assemble should map all sub structures correctly`() {
        val resource = DigimonResource(
            experience = 5000,
            level = 30,
            tp = 15,
            blast = 600,
            activeDigievolutionId = 12,
            hp = VitalResource(current = 100, max = 120),
            mp = VitalResource(current = 50, max = 60),
            inBattle = InBattleResource(
                condition = 0x04,
                strength = 25,
                defense = 185,
                speed = 84,
                hp = VitalResource(current = 80, max = 120),
                mp = VitalResource(current = 40, max = 60),
            ),
            attributes = AttributesResource(strength = 10, defense = 11, spirit = 12, wisdom = 13, speed = 14, charisma = 15),
            elements = ElementsResource(fire = 1, water = 2, ice = 3, wind = 4, thunder = 5, machine = 6, dark = 7),
            equipments = EquipmentsResource(head = 101, body = 102, right = 103, left = 104, accessory1 = 105, accessory2 = 106),
            digievolutions = listOf(DigievolutionSlotResource(index = 0, digievolutionId = 1)),
            storedDigievolutions = listOf(
                StoredDigievolutionResource(digievolutionId = 1, level = 5, dvexp = 200),
                StoredDigievolutionResource(digievolutionId = 99, level = 12),
            ),
        )

        val result = DigimonAssembler.assemble(resource)

        assertEquals(5000, result.experience)
        assertEquals(30, result.level)
        assertEquals(15, result.tp)
        assertEquals(600, result.blast)
        assertEquals(12, result.activeDigievolutionId)

        // HP / MP
        assertEquals(100, result.hp.current)
        assertEquals(120, result.hp.max)
        assertEquals(50, result.mp.current)
        assertEquals(60, result.mp.max)

        assertEquals(0x04, result.inBattle.condition)
        assertEquals(80, result.inBattle.hp.current)
        assertEquals(120, result.inBattle.hp.max)
        assertEquals(40, result.inBattle.mp.current)
        assertEquals(60, result.inBattle.mp.max)
        assertEquals(25, result.inBattle.strength)
        assertEquals(185, result.inBattle.defense)
        assertEquals(84, result.inBattle.speed)

        // Attributes
        assertEquals(10, result.attributes.strength)
        assertEquals(11, result.attributes.defense)
        assertEquals(12, result.attributes.spirit)
        assertEquals(13, result.attributes.wisdom)
        assertEquals(14, result.attributes.speed)
        assertEquals(15, result.attributes.charisma)

        // Elements
        assertEquals(1, result.elements.fire)
        assertEquals(7, result.elements.dark)

        // Equipments
        assertEquals(101, result.equipments.head)
        assertEquals(106, result.equipments.accessory2)

        // Digievolutions
        assertEquals(1, result.digievolutions.size)
        assertEquals(1, result.digievolutions[0].digievolutionId)
        assertEquals(5, result.digievolutions[0].digievolution!!.level)
        assertEquals(200, result.digievolutions[0].digievolution!!.dvexp)

        assertEquals(2, result.storedDigievolutions.size)
        assertEquals(99, result.storedDigievolutions[1].digievolutionId)
        assertEquals(12, result.storedDigievolutions[1].level)
    }

    private fun createBaseDigimonResource() = DigimonResource(
        experience = 100,
        level = 1,
        tp = 0,
        hp = VitalResource(),
        mp = VitalResource(),
        attributes = AttributesResource(),
        elements = ElementsResource(),
        equipments = EquipmentsResource(),
        digievolutions = emptyList(),
    )
}
