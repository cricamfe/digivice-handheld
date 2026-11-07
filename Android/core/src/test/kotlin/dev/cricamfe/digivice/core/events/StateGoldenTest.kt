package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.events.converters.StateConverter
import dev.cricamfe.digivice.core.memory.TestEnvironment
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The wire format of the whole state against the golden documents that the original C# backend produced from the
 * same RAM snapshots (`src/test/resources/golden/state`). The goldens are the truth: they are never edited.
 */
class StateGoldenTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("snapshots")
    fun `state json equals the original backend`(name: String) {
        val module = CoreModule(
            accessor = TestEnvironment.snapshot(name),
            readDefinition = TestEnvironment.definitions::readText,
            listDefinitions = TestEnvironment.definitions::listFiles,
        )
        val state = module.stateComposer.compose()

        assertJsonEquals(golden("state/$name.json"), coreJson.encodeToString(StateConverter.toDto(state)), name)
    }

    @Test
    fun `every snapshot has a golden`() {
        val snapshots = TestEnvironment.snapshotNames()

        assertEquals(18, snapshots.size)
        for (name in snapshots) {
            golden("state/$name.json")
        }
    }

    companion object {
        @JvmStatic
        fun snapshots(): List<String> = TestEnvironment.snapshotNames()
    }
}
