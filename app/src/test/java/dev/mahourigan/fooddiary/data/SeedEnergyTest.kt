package dev.mahourigan.fooddiary.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The calorie table has to cover the whole library.
 *
 * A missing entry doesn't fail loudly — it silently drops that ingredient out of
 * every total, so a meal reads lower than it was. This is the test that stops
 * that happening when an ingredient is added later.
 */
class SeedEnergyTest {

    @Test
    fun `every ingredient has energy data`() {
        val missing = SeedIngredients.all.filter { it.kcalPer100 == null || it.gramsPerUnit == null }
        assertEquals(emptyList<String>(), missing.map { it.id })
    }

    @Test
    fun `the table has no entries for ingredients that do not exist`() {
        val known = SeedIngredients.all.map { it.id }.toSet()
        assertEquals(emptySet<String>(), SeedEnergy.values.keys - known)
    }

    @Test
    fun `anything measured in grams or millilitres weighs one per unit`() {
        val wrong = SeedIngredients.all.filter { ingredient ->
            val unit = ingredient.defaultUnit?.lowercase()
            (unit == "g" || unit == "ml") && ingredient.gramsPerUnit != 1.0
        }
        assertEquals(emptyList<String>(), wrong.map { it.id })
    }

    @Test
    fun `no ingredient is denser than pure fat`() {
        // 900 kcal per 100 g is about the physical ceiling. A number above it is
        // a typo, and a typo here quietly inflates every meal it appears in.
        val absurd = SeedIngredients.all.filter { (it.kcalPer100 ?: 0.0) > 900.0 }
        assertEquals(emptyList<String>(), absurd.map { it.id })
    }

    @Test
    fun `a serving is a plausible weight`() {
        val absurd = SeedIngredients.all.filter { (it.gramsPerUnit ?: 1.0) > 400.0 }
        assertEquals(emptyList<String>(), absurd.map { it.id })
    }

    @Test
    fun `a few known figures land in the right region`() {
        // Spot checks, not precision: these are the ones a wrong unit would
        // make obviously silly.
        val byId = SeedIngredients.all.associateBy { it.id }

        // Two slices of sourdough, the default serving.
        val toast = byId.getValue("sourdough-wheat")
        assertTrue(toast.kcalPer100!! in 200.0..300.0)
        assertTrue(toast.gramsPerUnit!! in 30.0..60.0)

        // Olive oil is nearly pure fat.
        assertTrue(byId.getValue("olive-oil").kcalPer100!! > 800.0)

        // Salad leaves are nearly nothing.
        assertTrue(byId.getValue("lettuce").kcalPer100!! < 30.0)
    }
}
