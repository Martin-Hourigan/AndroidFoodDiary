package dev.mahourigan.fooddiary.domain

import dev.mahourigan.fooddiary.data.SeedIngredients
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A tin is a unit, and its weight is the drained weight.
 *
 * You buy two 400 g tins and you eat about 480 g, because half of a tin is
 * brine. Recording 800 g would overstate the food by two thirds; recording
 * 480 g would mean doing that sum in your head at the shelf. So the unit is
 * the tin and [Ingredient.gramsPerUnit] carries the difference.
 */
class TinnedPortionTest {

    private val byId = SeedIngredients.all.associateBy { it.id }
    private val tinnedBeans = byId.getValue("butter-beans-tinned-and-rinsed")

    @Test
    fun `two tins is the drained weight, not the label weight`() {
        assertEquals(480.0, Portion.of(2.0, "tin").grams(tinnedBeans)!!, 0.001)
    }

    @Test
    fun `and the energy follows from that weight`() {
        // 480 g at 115 kcal per 100 g.
        val energy = MealItem("butter-beans-tinned-and-rinsed", Portion.of(2.0, "tin"))
            .energy(byId)
        assertEquals(552.0, energy.kcal!!, 0.5)
    }

    @Test
    fun `a weight still means a weight`() {
        // Someone who weighs out 300 g of already-drained beans is not talking
        // about tins, and must not have 240 multiplied into it.
        assertEquals(300.0, Portion.of(300.0, "g").grams(tinnedBeans)!!, 0.001)
    }

    @Test
    fun `half a tin is the normal serving`() {
        // No portion given falls back to the typical one, which is half a tin.
        assertEquals(120.0, null.grams(tinnedBeans)!!, 0.001)
    }

    @Test
    fun `the by-weight entry is untouched`() {
        // Changing a shipped ingredient's unit is a migration, not an addition,
        // so the plain entry still measures in grams.
        val plain = byId.getValue("butter-beans")
        assertEquals("g", plain.defaultUnit)
        assertEquals(300.0, Portion.of(300.0, "g").grams(plain)!!, 0.001)
    }
}
