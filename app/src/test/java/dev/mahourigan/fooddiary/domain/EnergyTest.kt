package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class EnergyTest {

    // 2 slices at 40 g, 256 kcal per 100 g → one serving is 205 kcal.
    private val bread = Ingredient(
        id = "bread",
        name = "Sourdough",
        defaultUnit = "slice",
        typicalAmount = 2.0,
        kcalPer100 = 256.0,
        gramsPerUnit = 40.0,
    )

    private val oliveOil = Ingredient(
        id = "oil",
        name = "Olive oil",
        defaultUnit = "tbsp",
        typicalAmount = 1.0,
        kcalPer100 = 884.0,
        gramsPerUnit = 14.0,
    )

    /** No calorie data at all — something you added yourself and haven't filled in. */
    private val mystery = Ingredient(id = "mystery", name = "Something", defaultUnit = "g", typicalAmount = 50.0)

    private val library = listOf(bread, oliveOil, mystery).associateBy { it.id }

    private fun meal(vararg items: MealItem, override: Int? = null) = MealEntry(
        id = "m1",
        at = LocalDateTime.of(2026, 9, 5, 8, 0),
        items = items.toList(),
        kcalOverride = override,
    )

    // ---- Turning a portion into grams ---------------------------------------

    @Test
    fun `saying nothing means the usual serving`() {
        assertEquals(80.0, null.grams(bread)!!, 0.001)
    }

    @Test
    fun `a coarse size scales the usual serving`() {
        assertEquals(40.0, Portion.of(PortionSize.LITTLE).grams(bread)!!, 0.001)
        assertEquals(80.0, Portion.of(PortionSize.NORMAL).grams(bread)!!, 0.001)
        assertEquals(160.0, Portion.of(PortionSize.LOTS).grams(bread)!!, 0.001)
    }

    @Test
    fun `an amount in the ingredient's own unit converts`() {
        assertEquals(120.0, Portion.of(3.0, "slice").grams(bread)!!, 0.001)
    }

    @Test
    fun `an amount already in grams is taken as it is`() {
        // The portion bucketing refuses to compare grams against slices, but for
        // weight there's nothing to convert — 55 g is 55 g.
        assertEquals(55.0, Portion.of(55.0, "g").grams(bread)!!, 0.001)
    }

    @Test
    fun `an unconvertible unit gives nothing rather than a guess`() {
        // Inventing a conversion would put a made-up number into a total that
        // gets read as fact.
        assertNull(Portion.of(2.0, "handful").grams(bread))
    }

    @Test
    fun `an ingredient with no weight data gives nothing`() {
        assertNull(null.grams(mystery))
    }

    // ---- One item ------------------------------------------------------------

    @Test
    fun `a default serving of toast is about two hundred calories`() {
        val energy = MealItem("bread").energy(library)
        assertEquals(204.8, energy.kcal!!, 0.1)
        assertTrue(energy.assumed)
    }

    @Test
    fun `an amount you gave is not an assumption`() {
        val energy = MealItem("bread", Portion.of(1.0, "slice")).energy(library)
        assertEquals(102.4, energy.kcal!!, 0.1)
        assertFalse(energy.assumed)
    }

    @Test
    fun `an ingredient with no data is counted as unknown, not as zero`() {
        // Treating it as zero would quietly under-report the meal, and you'd
        // never know which meals were short.
        val energy = MealItem("mystery").energy(library)
        assertNull(energy.kcal)
        assertEquals(1, energy.unknownItems)
    }

    // ---- A whole meal --------------------------------------------------------

    @Test
    fun `a meal adds up its items`() {
        val energy = meal(
            MealItem("bread"),
            MealItem("oil", Portion.of(1.0, "tbsp")),
        ).energy(library) { null }

        assertEquals(204.8 + 123.8, energy.kcal!!, 0.5)
        // One part was assumed, so the whole total is an estimate.
        assertTrue(energy.assumed)
        assertFalse(energy.isExact)
    }

    @Test
    fun `a number you typed wins outright`() {
        val energy = meal(MealItem("bread"), override = 300).energy(library) { null }

        assertEquals(300.0, energy.kcal!!, 0.001)
        assertTrue(energy.stated)
        assertTrue(energy.isExact)
    }

    @Test
    fun `a recipe with its own figure is not re-estimated from its ingredients`() {
        // The point of putting a number on a recipe is that you worked it out
        // once. Counting the ingredients as well would double it.
        val entry = MealEntry(
            id = "m2",
            at = LocalDateTime.of(2026, 9, 5, 8, 0),
            bases = listOf(Basis("dhal", "Dhal", listOf(MealItem("bread")))),
            items = listOf(MealItem("bread"), MealItem("oil", Portion.of(1.0, "tbsp"))),
        )

        val energy = entry.energy(library) { if (it == "dhal") 400 else null }

        // 400 for the recipe, plus the oil that wasn't part of it. No bread.
        assertEquals(400.0 + 123.8, energy.kcal!!, 0.5)
    }

    @Test
    fun `a meal of nothing but stated recipes counts as stated`() {
        val entry = MealEntry(
            id = "m3",
            at = LocalDateTime.of(2026, 9, 5, 8, 0),
            bases = listOf(Basis("dhal", "Dhal", listOf(MealItem("bread")))),
            items = listOf(MealItem("bread")),
        )

        val energy = entry.energy(library) { 400 }
        assertEquals(400.0, energy.kcal!!, 0.001)
        assertTrue(energy.isExact)
    }

    @Test
    fun `a meal of things with no data has no total`() {
        val energy = meal(MealItem("mystery")).energy(library) { null }
        assertNull(energy.kcal)
        assertEquals(1, energy.unknownItems)
    }

    // ---- How it reads --------------------------------------------------------

    @Test
    fun `an estimate is rounded and marked, an exact figure is not`() {
        assertEquals("~200 kcal", Energy(kcal = 204.8, assumed = true).describe())
        assertEquals("300 kcal", Energy(kcal = 300.0, stated = true).describe())
        assertEquals("", Energy.Unknown.describe())
    }

    @Test
    fun `a total missing an ingredient is never shown as exact`() {
        val energy = Energy(kcal = 200.0, assumed = false, unknownItems = 1)
        assertFalse(energy.isExact)
        assertTrue(energy.describe().startsWith("~"))
    }
}
