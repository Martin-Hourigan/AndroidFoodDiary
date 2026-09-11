package dev.mahourigan.fooddiary.domain

import dev.mahourigan.fooddiary.data.LocalDiaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

/**
 * Keeping a meal you built out of ingredients.
 *
 * The saved copy has to be a snapshot, not a reference: editing the recipe later
 * must not reach back and change what a logged meal says you ate. That's the
 * same rule as [MealEntry] and it's the one worth a test.
 */
class SaveAsMealTest {

    @get:Rule
    val folder = TemporaryFolder()

    private suspend fun repo(): LocalDiaryRepository =
        LocalDiaryRepository(File(folder.newFolder(), "diary.json")).apply { load() }

    private fun recipeOf(items: List<MealItem>, tags: Set<String>) = Recipe(
        id = UUID.randomUUID().toString(),
        name = "Vegemite toast",
        items = items,
        tags = tags,
        lastUsedAt = LocalDateTime.now(),
        timesUsed = 1,
    )

    @Test
    fun `a saved meal lands in the folder it was given`() = runTest {
        val repository = repo()
        val items = listOf(MealItem("wheat-bread-wholemeal"), MealItem("vegemite"))

        repository.upsertRecipe(recipeOf(items, tags = setOf("Toasts")))

        val saved = repository.snapshot.value.recipes.first { it.name == "Vegemite toast" }
        assertEquals(setOf("Toasts"), saved.tags)
        assertEquals(listOf("wheat-bread-wholemeal", "vegemite"), saved.items.map { it.ingredientId })
    }

    @Test
    fun `no folder is a real answer, not an empty one`() = runTest {
        // A blank string would sort as its own nameless folder in the picker.
        val repository = repo()
        repository.upsertRecipe(recipeOf(listOf(MealItem("vegemite")), tags = emptySet()))

        assertTrue(repository.snapshot.value.recipes.first { it.name == "Vegemite toast" }.tags.isEmpty())
    }

    @Test
    fun `editing the saved meal later does not rewrite what you ate`() = runTest {
        val repository = repo()
        val items = listOf(MealItem("wheat-bread-wholemeal"), MealItem("vegemite"))
        val recipe = recipeOf(items, tags = setOf("Toasts"))
        repository.upsertRecipe(recipe)

        // Logged from it, carrying its own copy of what was in it at the time.
        repository.upsertMeal(
            MealEntry(
                id = "m1",
                at = LocalDateTime.of(2026, 9, 6, 8, 0),
                mealType = MealType.BREAKFAST,
                bases = listOf(Basis(recipe.id, recipe.name, items)),
                items = items,
            ),
        )

        // Later you decide the toast always had butter on it too.
        repository.upsertRecipe(recipe.copy(items = items + MealItem("butter")))

        val logged = repository.snapshot.value.meals.single()
        assertEquals(listOf("wheat-bread-wholemeal", "vegemite"), logged.items.map { it.ingredientId })
        assertEquals(
            listOf("wheat-bread-wholemeal", "vegemite"),
            logged.bases.single().itemsAtLogTime.map { it.ingredientId },
        )
    }
}
