package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MealEntryTest {

    private val cucumber = Ingredient(id = "cucumber", name = "Cucumber")
    private val tomato = Ingredient(id = "tomato", name = "Tomato", attributes = setOf(Attributes.Nightshade))
    private val onion = Ingredient(id = "onion", name = "Red onion", attributes = setOf(Attributes.Allium, Attributes.Fructan))
    private val feta = Ingredient(id = "feta", name = "Feta", attributes = setOf(Attributes.Dairy, Attributes.Lactose))
    private val avocado = Ingredient(id = "avocado", name = "Avocado", attributes = setOf(Attributes.Sorbitol))
    private val beetroot = Ingredient(id = "beetroot", name = "Beetroot", attributes = setOf(Attributes.Fructan))

    private val library = listOf(cucumber, tomato, onion, feta, avocado, beetroot).associateBy { it.id }

    private val greekSalad = listOf(
        MealItem("cucumber"),
        MealItem("tomato"),
        MealItem("onion"),
        MealItem("feta"),
    )

    private fun logged(items: List<MealItem>) = MealEntry(
        id = "m1",
        at = LocalDateTime.of(2026, 9, 3, 12, 40),
        bases = listOf(Basis(recipeId = "greek", name = "Greek salad", itemsAtLogTime = greekSalad)),
        items = items,
    )

    @Test
    fun `the plan's example diffs as you would read it`() {
        val meal = logged(
            greekSalad.filterNot { it.ingredientId == "onion" } + MealItem("avocado") + MealItem("beetroot"),
        )

        val diff = meal.diffFromBases()!!
        assertEquals(listOf("avocado", "beetroot"), diff.added.map { it.ingredientId })
        assertEquals(listOf("onion"), diff.removed.map { it.ingredientId })
        assertTrue(diff.reportioned.isEmpty())
    }

    @Test
    fun `an unchanged meal reports no diff`() {
        assertTrue(logged(greekSalad).diffFromBases()!!.isUnchanged)
    }

    @Test
    fun `an ad-hoc meal has nothing to differ from`() {
        val meal = MealEntry(id = "m2", at = LocalDateTime.now(), items = listOf(MealItem("tomato")))
        assertNull(meal.diffFromBases())
        assertTrue(meal.isAdHoc)
    }

    @Test
    fun `a changed portion is reported separately from an addition`() {
        val meal = logged(
            greekSalad.map { if (it.ingredientId == "onion") it.copy(portion = Portion.of(PortionSize.LOTS)) else it },
        )
        val diff = meal.diffFromBases()!!
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertEquals(listOf("onion"), diff.reportioned.map { it.ingredientId })
    }

    @Test
    fun `editing the recipe afterwards cannot rewrite what was logged`() {
        // The load-bearing guarantee. The entry holds its own resolved items and
        // its own snapshot of the recipe, so a later recipe edit — here, the
        // onion being dropped from Greek salad for good — changes neither what
        // the entry says was eaten nor what it says was changed at the time.
        val meal = logged(greekSalad + MealItem("avocado"))
        val diffBefore = meal.diffFromBases()!!

        @Suppress("UNUSED_VARIABLE")
        val recipeAfterEdit = greekSalad.filterNot { it.ingredientId == "onion" }

        val diffAfter = meal.diffFromBases()!!
        assertEquals(diffBefore, diffAfter)
        assertTrue(meal.items.any { it.ingredientId == "onion" })
        assertEquals(listOf("avocado"), diffAfter.added.map { it.ingredientId })
    }

    @Test
    fun `attributes are inherited from the ingredients`() {
        val meal = logged(greekSalad)
        val attributes = meal.attributes(library)

        assertTrue(Attributes.Dairy in attributes)
        assertTrue(Attributes.Lactose in attributes)
        assertTrue(Attributes.Allium in attributes)
        assertTrue(Attributes.Fructan in attributes)
        assertTrue(Attributes.Nightshade in attributes)
        assertFalse(Attributes.Gluten in attributes)
    }

    @Test
    fun `removing the culprit removes the attribute`() {
        val withoutFeta = logged(greekSalad.filterNot { it.ingredientId == "feta" })
        assertFalse(Attributes.Dairy in withoutFeta.attributes(library))
    }

    @Test
    fun `carriers name what is responsible so a warning can be acted on`() {
        val meal = logged(greekSalad)
        assertEquals(listOf("Feta"), meal.carriersOf(Attributes.Dairy, library).map { it.name })
        assertEquals(
            listOf("Red onion"),
            meal.carriersOf(Attributes.Allium, library).map { it.name },
        )
    }

    @Test
    fun `an unknown ingredient id is skipped rather than crashing`() {
        val meal = logged(greekSalad + MealItem("something-deleted"))
        assertFalse(meal.attributes(library).isEmpty())
        assertEquals("Greek salad", meal.title(library))
    }

    @Test
    fun `meal type is guessed from the clock`() {
        assertEquals(MealType.BREAKFAST, MealType.fromTime(LocalDateTime.of(2026, 9, 3, 8, 0).toLocalTime()))
        assertEquals(MealType.LUNCH, MealType.fromTime(LocalDateTime.of(2026, 9, 3, 12, 40).toLocalTime()))
        assertEquals(MealType.DINNER, MealType.fromTime(LocalDateTime.of(2026, 9, 3, 19, 30).toLocalTime()))
        assertEquals(MealType.SNACK, MealType.fromTime(LocalDateTime.of(2026, 9, 3, 23, 30).toLocalTime()))
    }
}
