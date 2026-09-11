package dev.mahourigan.fooddiary.domain

import dev.mahourigan.fooddiary.data.SeedCategories
import dev.mahourigan.fooddiary.data.SeedIngredients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roles, and the one thing they're for.
 *
 * A role only ever decides what a category offers. These check that it does
 * that, and — more importantly — that it stays out of everything else: a
 * vocabulary added for convenience must not end up influencing what the
 * analysis concludes.
 */
class RoleTest {

    private val library = SeedIngredients.all
    private val byName = library.associateBy { it.name }

    @Test
    fun `a facet offers what it subscribes to, minus what it already lists`() {
        val facet = Facet(
            name = "Ingredients",
            kind = FacetKind.ITEMS,
            options = listOf(FacetOption("Butter", ingredientId = "butter")),
            roles = setOf(Roles.Spread),
        )

        val offered = facet.alsoOffers(library).map { it.id }

        assertTrue("peanut-butter" in offered)
        assertTrue("tahini" in offered)
        // Already a chip in its own right; it shouldn't appear twice.
        assertFalse("butter" in offered)
        // Not a spread by any reading.
        assertFalse("red-wine" in offered)
    }

    @Test
    fun `a facet with no subscription offers nothing extra`() {
        val facet = Facet(name = "Ingredients", kind = FacetKind.ITEMS)
        assertEquals(emptyList<Ingredient>(), facet.alsoOffers(library))
    }

    @Test
    fun `peanut butter reaches toast and porridge without being listed in both`() {
        // The thing that started this: one tag, several categories, and no
        // hand-editing when a new one turns up.
        val peanutButter = byName.getValue("Peanut butter")
        assertTrue(peanutButter.isA(Roles.Spread))

        val reaches = SeedCategories.all
            .filter { category ->
                category.facets.any { peanutButter.id in it.alsoOffers(library).map(Ingredient::id) } ||
                    category.facets.any { facet -> facet.options.any { it.ingredientId == peanutButter.id } }
            }
            .map { it.name }

        assertTrue("Toast" in reaches)
        assertTrue("Porridge" in reaches)
    }

    @Test
    fun `roles never reach the analysis`() {
        // Roles are a convenience for the picker. If one ever showed up as an
        // attribute it would become a candidate cause, and "spread" is not a
        // hypothesis about anyone's gut.
        val attributeIds = library.flatMap { it.attributes }.map { it.id }.toSet()
        val roleIds = library.flatMap { it.roles }.map { it.id }.toSet()

        assertEquals(emptySet<String>(), attributeIds intersect roleIds)
    }

    @Test
    fun `the seeded vocabulary is the one the app knows about`() {
        // A role invented in the seed data but missing from the registry would
        // show up in the editor as a raw id, and couldn't be un-set.
        val known = Roles.known.map { it.role }.toSet()
        val used = library.flatMap { it.roles }.toSet() +
            SeedCategories.all.flatMap { it.facets }.flatMap { it.roles }

        assertEquals(emptySet<Role>(), used - known)
    }

    @Test
    fun `every subscription actually offers something`() {
        // A role nothing carries is a dead chip in the category editor.
        SeedCategories.all.forEach { category ->
            category.facets.forEach { facet ->
                facet.roles.forEach { role ->
                    assertTrue(
                        "${category.name} › ${facet.name} subscribes to ${role.id}, which nothing has",
                        library.any { role in it.roles },
                    )
                }
            }
        }
    }
}
