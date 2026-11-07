package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.factory.StateEventFactory
import dev.cricamfe.digivice.core.memory.TestEnvironment
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/**
 * The events that `StateEventFactory.create` produces against the golden documents that the original C# backend wrote
 * from the same RAM snapshots (`src/test/resources/golden/events`): `<a>__<b>.json` is `create(state a, state b)` for
 * each consecutive pair of snapshots sorted by name (not necessarily real game transitions), `__initial__<first>.json`
 * is `create(null, first)`. The goldens are the truth: they are never edited, and the order of the events matters.
 */
class EventsGoldenTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `events json equals the original backend`(golden: String, previous: String?, current: String) {
        val events = StateEventFactory.create(previous?.let(::compose), compose(current))

        assertJsonEquals(golden("events/$golden.json"), coreJson.encodeToString(ListSerializer(EventSerializer), events), golden)
    }

    @Test
    fun `every golden belongs to a case and there are eighteen`() {
        val goldenDirectory = File(checkNotNull(javaClass.getResource("/golden/events")) { "golden/events not found" }.toURI())
        val onDisk = goldenDirectory.listFiles { file -> file.extension == "json" }!!.map { it.nameWithoutExtension }.sorted()

        assertEquals(18, onDisk.size)
        assertEquals(cases().map { it.get()[0] as String }.sorted(), onDisk)
    }

    companion object {
        private val states = HashMap<String, State>()

        private fun compose(name: String): State = states.getOrPut(name) {
            CoreModule(
                accessor = TestEnvironment.snapshot(name),
                readDefinition = TestEnvironment.definitions::readText,
                listDefinitions = TestEnvironment.definitions::listFiles,
            ).stateComposer.compose()
        }

        /** (golden name, previous snapshot or null, current snapshot): 17 consecutive pairs and the initial state. */
        @JvmStatic
        fun cases(): List<Arguments> {
            val names = TestEnvironment.snapshotNames()
            val initial = Arguments.of("__initial__${names.first()}", null, names.first())
            val pairs = names.zipWithNext { previous, current -> Arguments.of("${previous}__$current", previous, current) }
            return listOf(initial) + pairs
        }
    }
}
