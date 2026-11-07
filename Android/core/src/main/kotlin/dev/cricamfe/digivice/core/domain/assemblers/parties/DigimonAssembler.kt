package dev.cricamfe.digivice.core.domain.assemblers.parties

import dev.cricamfe.digivice.core.domain.assemblers.parties.digimons.DigievolutionSlotAssembler
import dev.cricamfe.digivice.core.domain.assemblers.parties.digimons.StoredDigievolutionAssembler
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource

/** Port of `Backend/Domain/Assemblers/Parties/DigimonAssembler.cs`. */
object DigimonAssembler {
    fun assemble(resource: DigimonResource): Digimon {
        val activeDigievolutionId = if (resource.activeDigievolutionId <= 0) null else resource.activeDigievolutionId

        return Digimon(
            experience = resource.experience,
            level = resource.level,
            tp = resource.tp,
            blast = resource.blast,
            hp = Vital(
                current = resource.hp.current,
                max = resource.hp.max,
            ),
            mp = Vital(
                current = resource.mp.current,
                max = resource.mp.max,
            ),
            inBattle = InBattle(
                formId = resource.inBattle.formId,
                condition = resource.inBattle.condition,
                strength = resource.inBattle.strength,
                defense = resource.inBattle.defense,
                speed = resource.inBattle.speed,
                hp = Vital(
                    current = resource.inBattle.hp.current,
                    max = resource.inBattle.hp.max,
                ),
                mp = Vital(
                    current = resource.inBattle.mp.current,
                    max = resource.inBattle.mp.max,
                ),
            ),
            attributes = Attributes(
                strength = resource.attributes.strength,
                defense = resource.attributes.defense,
                spirit = resource.attributes.spirit,
                wisdom = resource.attributes.wisdom,
                speed = resource.attributes.speed,
                charisma = resource.attributes.charisma,
            ),
            elements = Elements(
                fire = resource.elements.fire,
                water = resource.elements.water,
                ice = resource.elements.ice,
                wind = resource.elements.wind,
                thunder = resource.elements.thunder,
                machine = resource.elements.machine,
                dark = resource.elements.dark,
            ),
            equipments = Equipments(
                head = resource.equipments.head.takeIf { it > 0 },
                body = resource.equipments.body.takeIf { it > 0 },
                right = resource.equipments.right.takeIf { it > 0 },
                left = resource.equipments.left.takeIf { it > 0 },
                accessory1 = resource.equipments.accessory1.takeIf { it > 0 },
                accessory2 = resource.equipments.accessory2.takeIf { it > 0 },
            ),
            digievolutions = resource.digievolutions.map { slot ->
                DigievolutionSlotAssembler.assemble(slot, resource.storedDigievolutions)
            },
            storedDigievolutions = resource.storedDigievolutions.map(StoredDigievolutionAssembler::assemble),
            activeDigievolutionId = activeDigievolutionId,
        )
    }
}
