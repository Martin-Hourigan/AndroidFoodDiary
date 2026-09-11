package dev.mahourigan.fooddiary.domain

import dev.mahourigan.fooddiary.data.SeedIngredients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A dish is written down the way it's cooked and eaten a portion at a time.
 *
 * Without [Recipe.serves] the app logs the whole pot: four dinners' worth of
 * GOS every time you have one, and a calorie total four times too big. These
 * check the division happens and that it stops where it stops.
 */
class ServingsTest {

    private val byId = SeedIngredients.all.associateBy { it.id }

    private val beans = Recipe(
        id = "beans",
        name = "Creamy tomato and basil beans",
        serves = 4,
        items = listOf(
            MealItem("butter-beans-tinned-and-rinsed", Portion.of(2.0, "tin")),
            MealItem("tofu-firm", Portion.of(250.0, "g")),
            MealItem("garlic", Portion.of(5.0, "clove")),
        ),
    )

    @Test
    fun `a serving is the amounts divided`() {
        val serving = beans.perServing().associate { it.ingredientId to it.portion?.amount }
        assertEquals(0.5, serving.getValue("butter-beans-tinned-and-rinsed")!!, 0.001)
        assertEquals(62.5, serving.getValue("tofu-firm")!!, 0.001)
        assertEquals(1.25, serving.getValue("garlic")!!, 0.001)
    }

    @Test
    fun `a serving carries a quarter of the energy`() {
        val whole = MealEntry(items = beans.items).energy(byId) { null }.kcal!!
        val one = MealEntry(items = beans.perServing()).energy(byId) { null }.kcal!!
        assertEquals(whole / 4.0, one, 0.5)
    }

    @Test
    fun `an unstated count means one, so nothing already written changes`() {
        val single = beans.copy(serves = null)
        assertEquals(1, single.servings)
        assertEquals(beans.items, single.perServing())
    }

    @Test
    fun `a nonsense count is treated as one rather than dividing by zero`() {
        assertEquals(1, beans.copy(serves = 0).servings)
        assertEquals(beans.items, beans.copy(serves = 0).perServing())
    }

    @Test
    fun `a coarse portion is left alone`() {
        // A quarter of "a little" is not something anyone can picture, and the
        // bucket was never a quantity to begin with.
        val withCoarse = beans.copy(
            items = listOf(MealItem("red-onion", Portion.of(PortionSize.LITTLE))),
        )
        val serving = withCoarse.perServing().single()
        assertEquals(PortionSize.LITTLE, serving.portion?.size)
        assertNull(serving.portion?.amount)
    }

    @Test
    fun `a stated whole-dish figure is divided too`() {
        assertEquals(300, beans.copy(kcal = 1200).kcalPerServing)
        assertNull(beans.kcalPerServing)
    }

    @Test
    fun `an item with no portion stays that way`() {
        val vague = beans.copy(items = listOf(MealItem("basil")))
        assertNull(vague.perServing().single().portion)
    }
}
