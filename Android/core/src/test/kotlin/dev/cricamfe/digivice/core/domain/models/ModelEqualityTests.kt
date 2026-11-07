package dev.cricamfe.digivice.core.domain.models

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

// In C# these models carry a hand-written Equals; here they are data classes and the compiler generates it. The
// tests are kept as a guard that every property takes part in equality and in hashCode, deep through the nested
// lists and objects, so a later refactor cannot hide real changes from the state diff.
class ModelEqualityTests {
    private fun <T : Any> assertEveryPropertyAffectsEquality(
        type: KClass<T>,
        create: () -> T,
        mutators: Map<String, (T) -> Unit>,
    ) {
        val propertyNames = type.declaredMemberProperties.map { it.name }.sorted()

        assertEquals(propertyNames, mutators.keys.sorted())
        assertEquals(create(), create())
        assertEquals(create().hashCode(), create().hashCode())

        for ((propertyName, mutate) in mutators) {
            val changed = create()
            mutate(changed)

            assertFalse(create() == changed, "${type.simpleName}.$propertyName is not compared in equals.")
            assertFalse(create().hashCode() == changed.hashCode(), "${type.simpleName}.$propertyName is not hashed.")
        }
    }

    private fun createDigimon() = Digimon(
        level = 10,
        tp = 5,
        blast = 100,
        experience = 1000,
        hp = Vital(current = 100, max = 120),
        mp = Vital(current = 50, max = 60),
        inBattle = InBattle(condition = 0, hp = Vital(), mp = Vital()),
        attributes = Attributes(strength = 5),
        elements = Elements(fire = 1),
        equipments = Equipments(head = 101),
        digievolutions = listOf(
            DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5)),
        ),
        storedDigievolutions = listOf(StoredDigievolution(digievolutionId = 4, level = 5)),
        activeDigievolutionId = 4,
    )

    private fun createQuest() = Quest(
        id = "treeBoots",
        requisites = listOf(Requisite(id = "folderBag", isDone = false)),
        steps = listOf(Step(number = 1, isDone = false, requisites = emptyList())),
    )

    @Test
    fun `digimon should compare every property`() {
        assertEveryPropertyAffectsEquality<Digimon>(
            Digimon::class,
            ::createDigimon,
            mapOf(
                "level" to { digimon -> digimon.level++ },
                "tp" to { digimon -> digimon.tp++ },
                "blast" to { digimon -> digimon.blast++ },
                "experience" to { digimon -> digimon.experience++ },
                "hp" to { digimon -> digimon.hp.current++ },
                "mp" to { digimon -> digimon.mp.current++ },
                "inBattle" to { digimon -> digimon.inBattle.condition = 0x04 },
                "attributes" to { digimon -> digimon.attributes.strength++ },
                "elements" to { digimon -> digimon.elements.fire++ },
                "equipments" to { digimon -> digimon.equipments.head = null },
                "digievolutions" to { digimon -> digimon.digievolutions[0].digievolution!!.dvexp++ },
                "storedDigievolutions" to { digimon -> digimon.storedDigievolutions[0].level++ },
                "activeDigievolutionId" to { digimon -> digimon.activeDigievolutionId = null },
            ),
        )
    }

    @Test
    fun `party should compare every property`() {
        assertEveryPropertyAffectsEquality<Party>(
            Party::class,
            { Party(slots = listOf(DigimonSlot(index = 1, digimonId = 1, digimon = createDigimon()))) },
            mapOf("slots" to { party -> party.slots[0].digimon!!.level++ }),
        )
    }

    @Test
    fun `digievolution should compare every property`() {
        assertEveryPropertyAffectsEquality<Digievolution>(
            Digievolution::class,
            { Digievolution(level = 5, dvexp = 100) },
            mapOf(
                "level" to { digievolution -> digievolution.level++ },
                "dvexp" to { digievolution -> digievolution.dvexp++ },
            ),
        )
    }

    @Test
    fun `stored digievolution should compare every property`() {
        assertEveryPropertyAffectsEquality<StoredDigievolution>(
            StoredDigievolution::class,
            { StoredDigievolution(digievolutionId = 4, level = 5) },
            mapOf(
                "digievolutionId" to { stored -> stored.digievolutionId++ },
                "level" to { stored -> stored.level++ },
            ),
        )
    }

    @Test
    fun `step should compare every property`() {
        assertEveryPropertyAffectsEquality<Step>(
            Step::class,
            { Step(number = 1, isDone = false, requisites = listOf(Requisite(id = "bambooSpear", isDone = false))) },
            mapOf(
                "number" to { step -> step.number++ },
                "isDone" to { step -> step.isDone = true },
                "requisites" to { step -> step.requisites[0].isDone = true },
            ),
        )
    }

    @Test
    fun `quest should compare every property`() {
        assertEveryPropertyAffectsEquality<Quest>(
            Quest::class,
            ::createQuest,
            mapOf(
                "id" to { quest -> quest.id = "fishingPole" },
                "requisites" to { quest -> quest.requisites[0].isDone = true },
                "steps" to { quest -> quest.steps[0].isDone = true },
            ),
        )
    }

    @Test
    fun `journal should compare every property`() {
        assertEveryPropertyAffectsEquality<Journal>(
            Journal::class,
            {
                Journal(
                    mainQuest = createQuest(),
                    sideQuests = listOf(createQuest()),
                    legendaryWeapons = listOf(createQuest()),
                    driAgents = listOf(createQuest()),
                    duelIsland = listOf(createQuest()),
                )
            },
            mapOf(
                "mainQuest" to { journal -> journal.mainQuest.steps[0].isDone = true },
                "sideQuests" to { journal -> journal.sideQuests[0].steps[0].isDone = true },
                "legendaryWeapons" to { journal -> journal.legendaryWeapons[0].steps[0].isDone = true },
                "driAgents" to { journal -> journal.driAgents[0].steps[0].isDone = true },
                "duelIsland" to { journal -> journal.duelIsland[0].steps[0].isDone = true },
            ),
        )
    }

    @Test
    fun `npc should compare every property`() {
        assertEveryPropertyAffectsEquality<Npc>(
            Npc::class,
            { Npc(battles = listOf(NpcBattle(id = "first", won = false))) },
            mapOf("battles" to { npc -> npc.battles[0].won = true }),
        )
    }
}
