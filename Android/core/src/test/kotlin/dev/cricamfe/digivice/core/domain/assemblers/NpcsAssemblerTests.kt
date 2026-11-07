package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.memory.resources.NpcBattleResource
import dev.cricamfe.digivice.core.memory.resources.NpcResource
import dev.cricamfe.digivice.core.memory.resources.NpcsResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class NpcsAssemblerTests {
    @Test
    fun `assemble should map battles from resource as won bool`() {
        val resource = NpcsResource(
            genji = NpcResource(
                battles = listOf(
                    NpcBattleResource(id = "first", value = 0x20u),
                    NpcBattleResource(id = "second", value = 0x01u),
                ),
            ),
            natsumi = NpcResource(battles = listOf(NpcBattleResource(id = "first", value = 0x02u))),
            catherine = NpcResource(battles = listOf(NpcBattleResource(id = "first", value = 0u))),
        )

        val result = NpcsAssembler.assemble(resource)

        assertEquals(2, result.genji.battles.size)
        assertTrue(result.genji.battles.single { it.id == "first" }.won)
        assertTrue(result.genji.battles.single { it.id == "second" }.won)
        assertTrue(result.natsumi.battles.single().won)
        assertFalse(result.catherine.battles.single().won)
        assertTrue(result.lucia.battles.isEmpty())
    }

    @Test
    fun `assemble should map every npc to its own property`() {
        // Reflection plays the role of the C# `GetProperties()`/`SetValue`: every NPC of the resource gets one
        // battle whose id is the property name, and must come out under the same-named property of the model.
        val resourceProperties = NpcsResource::class.declaredMemberProperties.map { it.name }
        val constructor = NpcsResource::class.primaryConstructor!!
        val resource = constructor.callBy(
            constructor.parameters.associateWith { parameter ->
                NpcResource(battles = listOf(NpcBattleResource(id = parameter.name!!, value = 1u)))
            },
        )

        val result = NpcsAssembler.assemble(resource)

        val modelProperties = Npcs::class.declaredMemberProperties
        assertEquals(29, resourceProperties.size)
        assertEquals(resourceProperties.sorted(), modelProperties.map { it.name }.sorted())
        for (property in modelProperties) {
            val npc = property.get(result) as Npc
            val battle = npc.battles.single()
            assertEquals(property.name, battle.id)
            assertTrue(battle.won)
        }
        // The model properties are plain mutable properties, like the C# `{ get; set; }`.
        assertTrue(modelProperties.all { it is KMutableProperty1<*, *> })
    }
}
