package dev.mahourigan.fooddiary.domain.analysis

import dev.mahourigan.fooddiary.domain.Basis
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.MealType
import dev.mahourigan.fooddiary.domain.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The picker: what's offered, and in what order.
 *
 * Membership is declared. A recipe or an ingredient is behind a chip because it
 * carries that tag, and for no other reason — the ranking decides the order,
 * never whether something belongs. An earlier version inferred membership from
 * co-occurrence and got it badly wrong; the tests for that inference are gone
 * with it, and the two that matter most below are the ones that stop it coming
 * back.
 */
class SuggestionsTest {

    private val butter = Ingredient(id = "butter", name = "Butter")
    private val hashBrown = Ingredient(id = "hash-brown", name = "Hash brown", tags = setOf("Cafe"))
    private val flatWhite = Ingredient(id = "flat-white", name = "Flat white", tags = setOf("Cafe"))
    private val rice = Ingredient(id = "rice", name = "Rice, basmati")
    private val sourdough = Ingredient(id = "sourdough", name = "Sourdough")
    private val wholemeal = Ingredient(id = "wholemeal", name = "Wholemeal bread")
    private val lentils = Ingredient(id = "lentils", name = "Lentils, red")

    private val ingredients =
        listOf(butter, hashBrown, flatWhite, rice, sourdough, wholemeal, lentils)

    private val toastSourdough = Recipe(
        id = "toast-sourdough",
        name = "Toast, sourdough",
        tags = setOf("Toasts"),
        items = listOf(MealItem("sourdough")),
    )
    private val toastWholemeal = Recipe(
        id = "toast-wholemeal",
        name = "Toast, wholemeal",
        tags = setOf("Toasts"),
        items = listOf(MealItem("wholemeal")),
    )
    private val scramble = Recipe(
        id = "scramble",
        name = "Chilli scramble",
        tags = setOf("Cafe", "Kettle Black"),
        items = listOf(MealItem("sourdough"), MealItem("butter")),
    )
    private val dhal = Recipe(id = "dhal", name = "Red lentil dhal", items = listOf(MealItem("lentils")))

    private val recipes = listOf(toastSourdough, toastWholemeal, scramble, dhal)

    private val toastCategory = Category(
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

    private val day = LocalDate.of(2026, 9, 9)

    private fun meal(
        n: Int,
        type: MealType,
        baseIds: List<String> = emptyList(),
        itemIds: List<String> = emptyList(),
    ) = MealEntry(
        id = "m$n",
        at = day.minusDays(n.toLong()).atTime(8, 0),
        mealType = type,
        bases = baseIds.map { Basis(recipeId = it, name = it, itemsAtLogTime = emptyList()) },
        items = itemIds.map { MealItem(it) },
    )

    private fun top(
        meals: List<MealEntry> = emptyList(),
        type: MealType? = MealType.BREAKFAST,
    ) = Suggestions.categories(
        categories = listOf(toastCategory),
        recipes = recipes,
        ingredients = ingredients,
        meals = meals,
        mealType = type,
    )

    private fun inside(
        tag: String,
        meals: List<MealEntry> = emptyList(),
        type: MealType? = MealType.BREAKFAST,
        current: Set<String> = emptySet(),
    ) = Suggestions.forPicker(
        recipes = recipes,
        ingredients = ingredients,
        meals = meals,
        mealType = type,
        currentItems = current,
        currentPath = listOf(tag),
    )

    // ---- What's on the first screen -----------------------------------------

    @Test
    fun `the first screen is categories, tag chips, and the door to everything`() {
        val labels = top().map { it.label }
        assertTrue("Toast" in labels)
        assertTrue("Cafe" in labels)
        assertTrue("Toasts" in labels)
        assertEquals("Recipes", labels.last())
    }

    @Test
    fun `a tag is one chip rather than each of its members`() {
        val labels = top().map { it.label }
        assertFalse("Chilli scramble" in labels)
        assertFalse("Toast, sourdough" in labels)
    }

    @Test
    fun `an untagged recipe is only behind the door`() {
        // Red lentil dhal carries no tag. It must not sit loose beside Toast —
        // mixing whole meals in with the categories is what made the very first
        // version of this screen unreadable.
        assertFalse("Red lentil dhal" in top().map { it.label })
        assertTrue("Red lentil dhal" in inside(Suggestions.ALL_RECIPES).map { it.label })
    }

    @Test
    fun `an ingredient's tag makes a chip on its own`() {
        // Nothing else is tagged "Cafe" among the ingredients but the hash brown
        // and the flat white, and either would be enough to justify the chip.
        val labels = Suggestions.categories(
            categories = emptyList(),
            recipes = emptyList(),
            ingredients = listOf(hashBrown),
            meals = emptyList(),
            mealType = null,
        ).map { it.label }
        assertEquals(listOf("Cafe"), labels)
    }

    // ---- What's behind a chip -----------------------------------------------

    @Test
    fun `a chip holds the recipes and the ingredients that carry the tag`() {
        val labels = inside("Cafe").map { it.label }
        assertTrue("Chilli scramble" in labels)
        assertTrue("Hash brown" in labels)
        assertTrue("Flat white" in labels)
    }

    @Test
    fun `a recipe's own ingredients do not come with it`() {
        // The bug this stops: Chilli scramble is tagged Cafe and contains
        // sourdough and butter. Nobody orders a slice of sourdough, so the
        // recipe is the thing in the cafe and its parts are not.
        val labels = inside("Cafe").map { it.label }
        assertFalse("Sourdough" in labels)
        assertFalse("Butter" in labels)
    }

    @Test
    fun `nothing is inferred from what you ate alongside it`() {
        // The bug this stops: Chilli scramble is in Cafe and contains sourdough,
        // so a dinner of sourdough and rice used to put rice in the cafe.
        // Sharing an ingredient with something behind a chip says nothing about
        // belonging behind it — otherwise one shared ingredient drags in
        // everything you have ever eaten beside it.
        val history = List(12) { n ->
            meal(n, MealType.DINNER, itemIds = listOf("sourdough", "rice"))
        }
        assertFalse("Rice, basmati" in inside("Cafe", meals = history, type = MealType.DINNER).map { it.label })
    }

    @Test
    fun `the door holds every recipe whatever its tags`() {
        val labels = inside(Suggestions.ALL_RECIPES).map { it.label }
        assertEquals(recipes.size, labels.size)
        assertTrue("Chilli scramble" in labels)
        assertTrue("Red lentil dhal" in labels)
    }

    @Test
    fun `the door is recipes by name, not ingredients`() {
        assertFalse("Hash brown" in inside(Suggestions.ALL_RECIPES).map { it.label })
    }

    @Test
    fun `a second tag on the same recipe is a second way to it`() {
        assertTrue("Chilli scramble" in inside("Cafe").map { it.label })
        assertTrue("Chilli scramble" in inside("Kettle Black").map { it.label })
    }

    @Test
    fun `a tag matches however it was capitalised`() {
        assertTrue("Chilli scramble" in inside("cafe").map { it.label })
    }

    @Test
    fun `what is already in the meal is not offered again`() {
        val labels = inside("Cafe", current = setOf("hash-brown")).map { it.label }
        assertFalse("Hash brown" in labels)
    }

    // ---- Order --------------------------------------------------------------

    @Test
    fun `each meal type leads with what you eat at it`() {
        val breakfasts = (0 until 10).map { n ->
            meal(n, MealType.BREAKFAST, baseIds = listOf("toast-sourdough"), itemIds = listOf("sourdough"))
        }
        val dinners = (10 until 20).map { n ->
            // Not butter: that is the Toast category's own option, so it would
            // score the category at dinner and the test would be measuring the
            // fixture rather than the ranking.
            meal(n, MealType.DINNER, baseIds = listOf("scramble"), itemIds = listOf("rice"))
        }
        val history = breakfasts + dinners

        assertEquals("Toasts", top(history, MealType.BREAKFAST).first().label)
        assertEquals("Cafe", top(history, MealType.DINNER).first().label)
    }

    @Test
    fun `a chip is ranked by its best member`() {
        val history = List(15) { n ->
            meal(n, MealType.BREAKFAST, baseIds = listOf("toast-sourdough"), itemIds = listOf("sourdough"))
        }
        val labels = top(history).map { it.label }
        assertTrue(labels.indexOf("Toasts") < labels.indexOf("Cafe"))
    }

    @Test
    fun `recent meals count for more than old ones`() {
        val recent = (0 until 14).map { n ->
            meal(n, MealType.BREAKFAST, baseIds = listOf("toast-wholemeal"), itemIds = listOf("wholemeal"))
        }
        val older = (14 until 28).map { n ->
            meal(n, MealType.BREAKFAST, baseIds = listOf("toast-sourdough"), itemIds = listOf("sourdough"))
        }
        assertEquals("Toast, wholemeal", inside("Toasts", meals = recent + older).first().label)
    }

    @Test
    fun `the same diary always produces the same order`() {
        // Purely computed is the promise; this is what it means in practice.
        val history = List(9) { n ->
            meal(n, MealType.BREAKFAST, baseIds = listOf("toast-sourdough"), itemIds = listOf("sourdough", "butter"))
        }
        assertEquals(
            top(history).map { it.label },
            top(history.shuffled()).map { it.label },
        )
    }

    @Test
    fun `ordering does not depend on the wall clock`() {
        val history = List(5) { n ->
            MealEntry(
                id = "x$n",
                at = LocalDateTime.of(2020, 1, 1, 8, 0).plusDays(n.toLong()),
                mealType = MealType.BREAKFAST,
                bases = listOf(Basis(recipeId = "toast-sourdough", name = "Toast, sourdough")),
            )
        }
        assertEquals("Toasts", top(history).first().label)
    }

    @Test
    fun `with nothing saved there is no door to show`() {
        val labels = Suggestions.categories(
            categories = listOf(toastCategory),
            recipes = emptyList(),
            ingredients = emptyList(),
            meals = emptyList(),
            mealType = null,
        ).map { it.label }
        assertEquals(listOf("Toast"), labels)
    }
}
