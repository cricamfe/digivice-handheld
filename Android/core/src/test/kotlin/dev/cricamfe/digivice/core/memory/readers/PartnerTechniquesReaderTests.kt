package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.memory.TestEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PartnerTechniquesReaderTests {
    private val SKILL_ID = 0x1FFF

    private fun read(snapshot: String): List<PartnerTechniquesReader.Partner> = CoreModule(
        accessor = TestEnvironment.snapshot(snapshot),
        readDefinition = TestEnvironment.definitions::readText,
        listDefinitions = TestEnvironment.definitions::listFiles,
    ).partnerTechniques.read()

    @Test
    fun `reads the forms in the slots of each partner of a real save, with their skills`() {
        val partners = read("durante-patrulheiro-opcional-1-centro-admin-subsolo-andar-1")

        assertEquals(listOf(0, 7, 6), partners.map { it.digimonId })
        val kotemon = partners.first().forms
        assertEquals(listOf(386, 5, 389), kotemon.map { it.id })
        assertEquals(listOf(105, 107, 184, 198, 0, 61), kotemon[0].skills.map { it and SKILL_ID })
        assertEquals(listOf(105, 107, 0, 0, 0, 0), kotemon[2].skills.map { it and SKILL_ID })
    }
}
