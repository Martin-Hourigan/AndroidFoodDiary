package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three text sizes.
 *
 * A multiplier over each style's own scale, so what these tests protect is the
 * arithmetic and the default — the rendering itself is Compose's job and is not
 * testable here.
 */
class TextSizeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `medium is exactly one, so it changes nothing`() {
        // Not 0.99 or 1.01. Medium has to be the identity or every style's own
        // carefully chosen scale is quietly nudged for everybody who never
        // opens this setting.
        assertEquals(1f, TextSize.MEDIUM.factor, 0f)
    }

    @Test
    fun `the three are in the order their names claim`() {
        assertTrue(TextSize.SMALL.factor < TextSize.MEDIUM.factor)
        assertTrue(TextSize.MEDIUM.factor < TextSize.LARGE.factor)
    }

    @Test
    fun `each step is worth noticing but not jarring`() {
        // Under about 10% nobody can tell the setting did anything; over about
        // 25% and a two-line row becomes three and the day screen reflows.
        listOf(TextSize.SMALL, TextSize.LARGE).forEach { size ->
            val change = kotlin.math.abs(size.factor - 1f)
            assertTrue("$size moves by $change", change in 0.1f..0.25f)
        }
    }

    @Test
    fun `a diary written before text size existed reads back as medium`() {
        // The whole reason this is a defaulted field rather than a migration.
        val old = """{"energyUnit":"KCAL","style":"HERBARIUM","themeMode":"SYSTEM"}"""
        assertEquals(TextSize.MEDIUM, json.decodeFromString<Settings>(old).textSize)
    }

    @Test
    fun `a chosen size survives a round trip`() {
        val settings = Settings(textSize = TextSize.LARGE)
        val back = json.decodeFromString<Settings>(json.encodeToString(settings))
        assertEquals(TextSize.LARGE, back.textSize)
    }

    @Test
    fun `every size has a label to put on a chip`() {
        TextSize.entries.forEach { assertTrue(it.label.isNotBlank()) }
    }
}
