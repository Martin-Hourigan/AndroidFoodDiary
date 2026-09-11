package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.domain.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedLibraryTest {

    private val byId: Map<String, Ingredient> = SeedIngredients.all.associateBy { it.id }

    @Test
    fun `ids are unique`() {
        // A duplicate id would silently shadow one entry with another, and the
        // one that lost would be untaggable and unfindable.
        val duplicates = SeedIngredients.all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun `every starter recipe refers to ingredients that exist`() {
        // Recipe items are slugs typed by hand, so this is the test that catches
        // a typo before it becomes a recipe listing an ingredient nobody has.
        val missing = StarterRecipes.all.flatMap { recipe ->
            recipe.items.map { recipe.name to it.ingredientId }
        }.filterNot { (_, id) -> id in byId }

        assertEquals(emptyList<Pair<String, String>>(), missing)
    }

    @Test
    fun `every category option refers to an ingredient that exists`() {
        // Facet options carry hand-written slugs, so this is what catches a typo
        // before it becomes a chip that adds nothing.
        val missing = SeedCategories.all.flatMap { category ->
            category.facets.flatMap { facet ->
                val ids = facet.options.mapNotNull { it.ingredientId } +
                    listOfNotNull(facet.defaultIngredientId)
                ids.map { "${category.name}/${facet.name}" to it }
            }
        }.filterNot { (_, id) -> id in byId }

        assertEquals(emptyList<Pair<String, String>>(), missing)
    }

    @Test
    fun `a style facet has something to fall back on`() {
        // Without a default, ticking only descriptive options ("sourdough") would
        // silently produce no bread at all.
        val styleFacets = SeedCategories.all.flatMap { it.facets }
            .filter { it.kind == dev.mahourigan.fooddiary.domain.FacetKind.STYLE }

        assertTrue(styleFacets.isNotEmpty())
        assertTrue(styleFacets.all { it.defaultIngredientId != null })
    }

    @Test
    fun `nothing in the library is meat`() {
        // The household is vegetarian. This is a real requirement rather than a
        // preference, and a later addition should trip over it.
        // Whole words only: "chamomile" contains "ham", and a substring match
        // would fail on the tea rather than on any actual meat.
        val meatWords = setOf(
            "chicken", "beef", "pork", "lamb", "bacon", "ham", "prosciutto",
            "salmon", "tuna", "prawn", "prawns", "anchovy", "anchovies", "gelatin",
        )
        val offenders = SeedIngredients.all.filter { ingredient ->
            ingredient.name.lowercase().split(Regex("[^a-z]+")).any { it in meatWords }
        }
        assertEquals(emptyList<Ingredient>(), offenders)
    }

    @Test
    fun `wheat carries both gluten and fructans`() {
        // The confound the whole app is built around. If this ever stops being
        // true the analysis silently loses its ability to separate the two.
        val wheatBread = byId.getValue("wheat-bread-white")
        assertTrue(Attributes.Gluten in wheatBread.attributes)
        assertTrue(Attributes.Fructan in wheatBread.attributes)
    }

    @Test
    fun `gluten-free bread gives the analysis something to contrast wheat with`() {
        val glutenFree = byId.getValue("gluten-free-bread")
        assertTrue(Attributes.Gluten !in glutenFree.attributes)
    }

    @Test
    fun `hard aged cheeses are not tagged with lactose but fresh ones are`() {
        // The most informative distinction in the library for anyone who eats
        // cheese daily, which a vegetarian household does.
        listOf("parmesan", "cheddar-mature", "pecorino").forEach { id ->
            assertTrue("$id should have no lactose", Attributes.Lactose !in byId.getValue(id).attributes)
            assertTrue("$id is still dairy", Attributes.Dairy in byId.getValue(id).attributes)
        }
        listOf("ricotta", "cottage-cheese", "feta", "mozzarella").forEach { id ->
            assertTrue("$id should have lactose", Attributes.Lactose in byId.getValue(id).attributes)
        }
    }

    @Test
    fun `the alternative milks are not treated as interchangeable`() {
        assertTrue(Attributes.Fructan in byId.getValue("oat-milk").attributes)
        assertTrue(Attributes.Gos in byId.getValue("soy-milk-whole-bean").attributes)
        assertTrue(Attributes.Gos !in byId.getValue("soy-milk-protein-isolate").attributes)
        assertTrue(byId.getValue("almond-milk").attributes.none { it == Attributes.Fructan || it == Attributes.Gos })
    }

    @Test
    fun `the usual hidden sources of gluten are tagged`() {
        // Nobody thinks of a stock cube as a wheat-and-onion product. It very
        // often is both, and that is exactly why it ships pre-tagged.
        assertTrue(Attributes.Gluten in byId.getValue("soy-sauce").attributes)
        assertTrue(Attributes.Gluten !in byId.getValue("tamari").attributes)
        assertTrue(Attributes.Gluten in byId.getValue("stock-cube").attributes)
        assertTrue(Attributes.Allium in byId.getValue("stock-cube").attributes)
    }

    @Test
    fun `an ingredient with a typical amount also says what its unit is`() {
        // A typical amount with no unit cannot be bucketed against anything, so
        // it would be dead weight in the file and confusing in the editor.
        val incomplete = SeedIngredients.all.filter { it.typicalAmount != null && it.defaultUnit == null }
        assertEquals(emptyList<Ingredient>(), incomplete)
    }

    @Test
    fun `the library is big enough to be usable on day one`() {
        assertTrue("only ${SeedIngredients.all.size} ingredients", SeedIngredients.all.size >= 250)
    }

    @Test
    fun `merging keeps your edits and respects what you retired`() {
        val edited = byId.getValue("oat-milk").copy(name = "Oat milk (the barista one)", isUserCreated = true)
        val existing = listOf(edited)
        val retired = setOf("cola")

        val merged = SeedIngredients.mergeInto(existing, retired)

        assertEquals("Oat milk (the barista one)", merged.first { it.id == "oat-milk" }.name)
        assertTrue(merged.none { it.id == "cola" })
        assertTrue(merged.any { it.id == "parmesan" })
        assertEquals(SeedIngredients.all.size, merged.size + retired.size)
    }
}
