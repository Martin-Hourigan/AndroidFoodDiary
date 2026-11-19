package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bundled recipes, and the merge that carries additions into a diary that
 * already exists.
 */
class RecipeSeedTest {

    /**
     * The count every bump has to keep in step.
     *
     * Adding a recipe without raising [StarterRecipes.VERSION] is silent in the
     * worst way: the recipe ships inside the APK, appears on a fresh install,
     * and never reaches a single existing diary. Pinning the count here turns
     * that into a failing test instead of a quiet nothing.
     *
     * When this fails because a recipe was genuinely added: set this to the
     * new count, and raise [StarterRecipes.VERSION] unless it has already been
     * raised since the last release. The version counts releases, not edits --
     * what it has to beat is the number written into diaries in the wild, so
     * several recipes added before the next tag all ride in on one bump.
     */
    @Test
    fun `the recipe count and the seed version are in step`() {
        assertEquals(
            "StarterRecipes.all changed size -- bump StarterRecipes.VERSION and update this count",
            8,
            StarterRecipes.all.size,
        )
        assertEquals(2, StarterRecipes.VERSION)
    }

    @Test
    fun `ids are unique`() {
        val duplicates = StarterRecipes.all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun `merging into an empty diary brings everything`() {
        val merged = StarterRecipes.mergeInto(emptyList(), emptySet())
        assertEquals(StarterRecipes.all.size, merged.size)
    }

    @Test
    fun `merging twice adds nothing the second time`() {
        val once = StarterRecipes.mergeInto(emptyList(), emptySet())
        val twice = StarterRecipes.mergeInto(once, emptySet())
        assertEquals(once.size, twice.size)
    }

    @Test
    fun `an edited recipe keeps the edit`() {
        val original = StarterRecipes.all.first()
        val edited = original.copy(name = "My version", isUserCreated = true)

        val merged = StarterRecipes.mergeInto(listOf(edited), emptySet())

        assertEquals("My version", merged.first { it.id == original.id }.name)
        // And the rest still arrive.
        assertEquals(StarterRecipes.all.size, merged.size)
    }

    @Test
    fun `a retired recipe stays gone`() {
        val thrownOut = StarterRecipes.all.first().id

        val merged = StarterRecipes.mergeInto(emptyList(), setOf(thrownOut))

        assertTrue(merged.none { it.id == thrownOut })
        assertEquals(StarterRecipes.all.size - 1, merged.size)
    }

    @Test
    fun `a recipe of your own is never touched`() {
        val mine = Recipe(
            id = "mine-laksa",
            name = "Laksa",
            tags = setOf("Dinner"),
            items = emptyList(),
            isUserCreated = true,
        )

        val merged = StarterRecipes.mergeInto(listOf(mine), emptySet())

        assertEquals(mine, merged.first { it.id == "mine-laksa" })
        assertEquals(StarterRecipes.all.size + 1, merged.size)
    }
}
