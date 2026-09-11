package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CategoryTest {

    private val toast = Category(
        id = "cat-toast",
        name = "Toast",
        facets = listOf(
            Facet(
                name = "Bread",
                kind = FacetKind.STYLE,
                defaultIngredientId = "wheat-bread",
                options = listOf(
                    FacetOption("Sourdough", modifiers = setOf(Attributes.Sourdough, Attributes.Fermented)),
                    FacetOption("Wholemeal", modifiers = setOf(Attributes.Wholegrain)),
                    FacetOption("Multigrain", modifiers = setOf(Attributes.Wholegrain)),
                    FacetOption("Gluten-free", ingredientId = "gf-bread"),
                ),
            ),
            Facet(
                name = "On it",
                kind = FacetKind.ITEMS,
                options = listOf(
                    FacetOption("Butter", ingredientId = "butter"),
                    FacetOption("Vegemite", ingredientId = "vegemite"),
                ),
            ),
        ),
    )

    private fun chose(vararg pairs: Pair<String, Set<String>>) = mapOf(*pairs)

    // ---- The load-bearing one ----------------------------------------------

    @Test
    fun `a wholemeal sourdough multigrain loaf is one bread, not three`() {
        // The whole reason style is its own kind of facet. Three ticks describe
        // a single loaf; adding three breads would claim you ate three.
        val items = toast.resolve(
            chose("Bread" to setOf("sourdough", "wholemeal", "multigrain")),
        )

        assertEquals(1, items.size)
        assertEquals("wheat-bread", items.single().ingredientId)
        assertEquals(
            setOf(Attributes.Sourdough, Attributes.Fermented, Attributes.Wholegrain),
            items.single().modifiers,
        )
    }

    @Test
    fun `an option that names an ingredient decides what the bread actually is`() {
        val items = toast.resolve(
            chose("Bread" to setOf("sourdough", "gf-bread")),
        )

        assertEquals(1, items.size)
        assertEquals("gf-bread", items.single().ingredientId)
        // Still a sourdough — gluten-free sourdough is a real thing.
        assertTrue(Attributes.Sourdough in items.single().modifiers)
    }

    @Test
    fun `style with nothing ticked contributes nothing`() {
        assertTrue(toast.resolve(emptyMap()).isEmpty())
        assertTrue(toast.resolve(chose("Bread" to emptySet())).isEmpty())
    }

    // ---- Item facets --------------------------------------------------------

    @Test
    fun `an items facet adds one ingredient per tick`() {
        val items = toast.resolve(
            chose("Bread" to setOf("wholemeal"), "On it" to setOf("butter", "vegemite")),
        )

        assertEquals(
            listOf("wheat-bread", "butter", "vegemite"),
            items.map { it.ingredientId },
        )
        // Toppings are themselves, not descriptions of the bread.
        assertTrue(items.drop(1).all { it.modifiers.isEmpty() })
    }

    @Test
    fun `toppings can be chosen without a bread`() {
        val items = toast.resolve(chose("On it" to setOf("butter")))
        assertEquals(listOf("butter"), items.map { it.ingredientId })
    }

    // ---- Options you add yourself -------------------------------------------

    @Test
    fun `an added topping behaves like the bundled ones`() {
        val withMarmalade = toast.copy(
            facets = toast.facets.map { facet ->
                if (facet.name == "On it") {
                    facet.copy(options = facet.options + FacetOption("Marmalade", ingredientId = "marmalade"))
                } else {
                    facet
                }
            },
        )

        val items = withMarmalade.resolve(
            chose("Bread" to setOf("wholemeal"), "On it" to setOf("marmalade")),
        )
        assertEquals(listOf("wheat-bread", "marmalade"), items.map { it.ingredientId })
    }

    @Test
    fun `a description you add describes the bread rather than replacing it`() {
        // "Long-fermented" is a way a loaf was made, so it has to behave like
        // sourdough — a modifier on whatever bread you picked, not a new one.
        val ownWords = Attribute.custom("long-fermented")
        val withDescriptor = toast.copy(
            facets = toast.facets.map { facet ->
                if (facet.name == "Bread") {
                    facet.copy(options = facet.options + FacetOption("Long-fermented", modifiers = setOf(ownWords)))
                } else {
                    facet
                }
            },
        )

        val items = withDescriptor.resolve(
            chose("Bread" to setOf("wholemeal", "long-fermented")),
        )

        assertEquals(1, items.size)
        assertEquals("wheat-bread", items.single().ingredientId)
        assertTrue(ownWords in items.single().modifiers)
        assertTrue(Attributes.Wholegrain in items.single().modifiers)
    }

    // ---- What the analysis sees --------------------------------------------

    @Test
    fun `modifiers reach the meal's attributes`() {
        // Sourdough has to be testable as a factor in its own right — it's the
        // thing that can separate gluten from fructans, since fermenting breaks
        // fructans down but leaves the gluten alone.
        val library = mapOf(
            "wheat-bread" to Ingredient(
                id = "wheat-bread",
                name = "Wheat bread",
                attributes = setOf(Attributes.Gluten, Attributes.Wheat, Attributes.Fructan),
            ),
        )
        val meal = MealEntry(
            id = "m1",
            at = LocalDateTime.of(2026, 9, 5, 8, 0),
            items = toast.resolve(chose("Bread" to setOf("sourdough"))),
        )

        val attributes = meal.attributes(library)
        assertTrue(Attributes.Gluten in attributes)
        assertTrue(Attributes.Sourdough in attributes)
    }

    @Test
    fun `an ordinary loaf carries no sourdough tag`() {
        val library = mapOf(
            "wheat-bread" to Ingredient(id = "wheat-bread", name = "Wheat bread", attributes = setOf(Attributes.Gluten)),
        )
        val meal = MealEntry(
            id = "m2",
            at = LocalDateTime.of(2026, 9, 5, 8, 0),
            items = toast.resolve(chose("Bread" to setOf("white-ish-nothing"))),
        )
        assertFalse(Attributes.Sourdough in meal.attributes(library))
    }
    @Test
    fun `something picked from a role subscription reaches the meal`() {
        // The bug this exists to stop: options offered by a role subscription
        // are built from the library at display time, so they aren't in
        // Facet.options. Resolving against that list alone ticked them on
        // screen and then silently dropped them from the meal.
        val library = listOf(
            Ingredient(id = "asparagus", name = "Asparagus", roles = setOf(Roles.Vegetable)),
            Ingredient(id = "butter", name = "Butter", roles = setOf(Roles.Spread)),
        )
        val category = Category(
            id = "cat-toast",
            name = "Toast",
            facets = listOf(
                Facet(
                    name = "Ingredients",
                    kind = FacetKind.ITEMS,
                    options = listOf(FacetOption("Butter", ingredientId = "butter")),
                    roles = setOf(Roles.Vegetable),
                ),
            ),
        )

        val items = category.resolve(
            mapOf("Ingredients" to setOf("butter", "asparagus")),
            library.associateBy { it.id },
        )

        assertEquals(listOf("butter", "asparagus"), items.map { it.ingredientId })
    }

    @Test
    fun `a tick for something no longer in the library adds nothing`() {
        val category = Category(
            id = "cat-toast",
            name = "Toast",
            facets = listOf(
                Facet(name = "Ingredients", kind = FacetKind.ITEMS, roles = setOf(Roles.Spread)),
            ),
        )

        val items = category.resolve(mapOf("Ingredients" to setOf("deleted-thing")), emptyMap())

        assertTrue(items.isEmpty())
    }
}
