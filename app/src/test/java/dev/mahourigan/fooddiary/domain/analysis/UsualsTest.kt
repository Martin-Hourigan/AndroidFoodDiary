package dev.mahourigan.fooddiary.domain.analysis

import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "The usual" inside a category.
 *
 * A recipe tagged with the category's name. Chosen over the alternatives —
 * matching ingredients, or remembering which category a recipe was built in —
 * because it needs no new field and no inference: tagging a toast "Toast" is
 * what puts it at the top of Toast, and it's the same tag mechanism as
 * everything else.
 */
class UsualsTest {

    private val toast = Category(
        id = "cat-toast",
        name = "Toast",
        facets = listOf(
            Facet(
                name = "Ingredients",
                kind = FacetKind.ITEMS,
                options = listOf(FacetOption("Butter", ingredientId = "butter")),
            ),
        ),
    )

    private val vegemiteToast = Recipe(
        id = "vegemite-toast",
        name = "Vegemite toast",
        tags = setOf("Toast"),
        items = listOf(MealItem("wholemeal"), MealItem("vegemite")),
    )
    private val cheeseToastie = Recipe(
        id = "cheese-toastie",
        name = "Cheese toastie",
        tags = setOf("toast"),
        items = listOf(MealItem("wholemeal"), MealItem("cheddar")),
    )
    private val dhal = Recipe(
        id = "dhal",
        name = "Red lentil dhal",
        items = listOf(MealItem("lentils")),
    )
    private val flatWhite = Recipe(
        id = "flat-white",
        name = "Flat white",
        tags = setOf("Cafe"),
        items = listOf(MealItem("coffee")),
    )

    private val recipes = listOf(vegemiteToast, cheeseToastie, dhal, flatWhite)

    private fun usuals() = Suggestions.usualsFor(toast, recipes, emptyList(), null)

    @Test
    fun `a recipe tagged with the category name is a usual`() {
        assertTrue("Vegemite toast" in usuals().map { it.name })
    }

    @Test
    fun `however it was capitalised`() {
        // "toast" and "Toast" are the same tag everywhere else, so they have to
        // be the same here too.
        assertTrue("Cheese toastie" in usuals().map { it.name })
    }

    @Test
    fun `nothing else is`() {
        val names = usuals().map { it.name }
        assertFalse("Red lentil dhal" in names)
        assertFalse("Flat white" in names)
    }

    @Test
    fun `a recipe sharing the category's ingredients is not a usual on that alone`() {
        // Both toasts contain wholemeal, and so does this — but it isn't tagged,
        // so it isn't a usual. Matching ingredients is exactly the kind of
        // inference that put onion in the cafe.
        val untaggedToast = Recipe(
            id = "mystery",
            name = "Something on wholemeal",
            items = listOf(MealItem("wholemeal"), MealItem("butter")),
        )
        val names = Suggestions.usualsFor(toast, recipes + untaggedToast, emptyList(), null)
            .map { it.name }
        assertFalse("Something on wholemeal" in names)
    }

    @Test
    fun `the category absorbs its own tag rather than sitting beside it`() {
        // Toast is already a chip on the first screen. A second Toast chip for
        // the tag would be two doors to the same idea.
        val labels = Suggestions.categories(
            categories = listOf(toast),
            recipes = recipes,
            ingredients = emptyList(),
            meals = emptyList(),
            mealType = null,
        ).map { it.label }

        assertEquals(1, labels.count { it.equals("Toast", ignoreCase = true) })
        // Cafe matches no category, so it still gets its own.
        assertTrue("Cafe" in labels)
    }

    @Test
    fun `an ingredient tagged with a category name is absorbed too`() {
        val labels = Suggestions.categories(
            categories = listOf(toast),
            recipes = emptyList(),
            ingredients = listOf(Ingredient(id = "crumpet", name = "Crumpet", tags = setOf("Toast"))),
            meals = emptyList(),
            mealType = null,
        ).map { it.label }

        assertEquals(listOf("Toast"), labels)
    }
}
