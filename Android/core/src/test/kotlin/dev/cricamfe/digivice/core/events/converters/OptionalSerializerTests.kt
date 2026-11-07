package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.events.coreJson
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.parties.DigimonSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import dev.cricamfe.digivice.core.events.models.HealthStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Port of `OptionalJsonConverterTests.cs` and `OptionalJsonConverterFactoryTests.cs`, in the camelCase of the wire
 * format, plus the checks that the three states (absent, null, value) reach the JSON as they do in the backend.
 */
class OptionalSerializerTests {
    private fun encode(dto: DigimonBattleDto) = coreJson.encodeToString(DigimonBattleDto.serializer(), dto)

    @Test
    fun `a present value is written as the bare value`() {
        val json = coreJson.encodeToString(OptionalSerializer(Int.serializer()), Optional.Present(42))

        assertEquals("42", json)
    }

    @Test
    fun `a dto with no present value is an empty object`() {
        assertEquals("{}", coreJson.encodeToString(PlayerDto.serializer(), PlayerDto()))
    }

    @Test
    fun `present properties are written with their camelCase name and the rest is left out`() {
        val dto = PlayerDto(bits = Optional.Present(123), mapId = Optional.Present("00AF"))

        val root = coreJson.parseToJsonElement(coreJson.encodeToString(PlayerDto.serializer(), dto)).jsonObject

        assertEquals(setOf("bits", "mapId"), root.keys)
        assertEquals(123, root.getValue("bits").jsonPrimitive.int)
        assertEquals("00AF", root.getValue("mapId").jsonPrimitive.content)
    }

    @Test
    fun `a present null is written as an explicit null`() {
        val root = coreJson.parseToJsonElement(encode(DigimonBattleDto(enemy = Optional.Present(null)))).jsonObject

        assertEquals(JsonNull, root["enemy"])
        assertFalse("field" in root)
    }

    @Test
    fun `a present zero is written as zero`() {
        val json = coreJson.encodeToString(
            DigimonSlotDto.serializer(),
            DigimonSlotDto(index = 1, digimonId = Optional.Present(0)),
        )

        assertEquals("""{"index":1,"digimonId":0}""", json)
    }

    @Test
    fun `plain fields are written even when they hold their default`() {
        assertEquals("""{"index":0}""", coreJson.encodeToString(DigimonSlotDto.serializer(), DigimonSlotDto()))
    }

    @Test
    fun `nullable plain fields are written as null`() {
        val json = coreJson.encodeToString(HealthDto.serializer(), HealthDto(HealthStatus.Healthy))

        assertEquals("""{"status":"Healthy","errorCode":null,"errorDetail":null}""", json)
    }

    @Test
    fun `unsigned bytes are written as plain numbers`() {
        val high = DigimonBattleDto(field = Optional.Present(255u))
        val low = DigimonBattleDto(field = Optional.Present(0u))

        assertEquals("""{"field":255}""", encode(high))
        assertEquals("""{"field":0}""", encode(low))
    }

    @Test
    fun `the json is not pretty printed`() {
        val json = coreJson.encodeToString(PlayerDto.serializer(), PlayerDto(bits = Optional.Present(1)))

        assertFalse(json.any { it == '\n' || it == ' ' })
    }

    @Test
    fun `an empty optional cannot be encoded by a json that writes defaults`() {
        val writesDefaults = Json { encodeDefaults = true }

        assertThrows(SerializationException::class.java) {
            writesDefaults.encodeToString(PlayerDto.serializer(), PlayerDto())
        }
    }

    @Test
    fun `deserialize creates an optional with the value`() {
        val optional = coreJson.decodeFromString(OptionalSerializer(Int.serializer()), "42")

        assertTrue(optional.hasValue)
        assertEquals(42, optional.value)
    }

    @Test
    fun `deserialize keeps an explicit null as a present value`() {
        val optional = coreJson.decodeFromString(serializer<Optional<String?>>(), "null")

        assertTrue(optional.hasValue)
        assertEquals(Optional.Present(null), optional)
    }

    @Test
    fun `a dto round trips its three states`() {
        val dto = DigimonBattleDto(field = Optional.Empty, enemy = Optional.Present(null))

        val back = coreJson.decodeFromString(DigimonBattleDto.serializer(), encode(dto))

        assertEquals(dto, back)
    }

    @Test
    fun `a missing key is empty and a null key is a present null`() {
        val absent = coreJson.decodeFromString(DigimonBattleDto.serializer(), "{}")
        val explicitNull = coreJson.decodeFromString(DigimonBattleDto.serializer(), """{"enemy":null}""")

        assertEquals(Optional.Empty, absent.enemy)
        assertEquals(Optional.Present(null), explicitNull.enemy)
    }

    @Test
    fun `the serializer of an optional type is the optional serializer`() {
        assertTrue(serializer<Optional<Int>>() is OptionalSerializer<*>)
        assertTrue(serializer<Optional<String>>() is OptionalSerializer<*>)
        assertFalse(serializer<Int>() is OptionalSerializer<*>)
        assertFalse(serializer<Int?>() is OptionalSerializer<*>)
        assertFalse(PlayerDto.serializer() is OptionalSerializer<*>)
    }
}
