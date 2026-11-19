package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Portion
import dev.mahourigan.fooddiary.domain.PortionSize
import dev.mahourigan.fooddiary.domain.Recipe

/**
 * Saved meals — whole things you'd eat as they are.
 *
 * The toast and porridge components that used to live here are gone: they became
 * [SeedCategories], because "which bread, what on it" is two questions rather
 * than a list of every combination. What's left is genuinely whole meals, and
 * they live behind **Saved** rather than cluttering the first screen.
 *
 * Cafe keeps a group, since a café dish really is a fixed thing you order by
 * name. Groups nest one level, so `Cafe/Kettle Black` gives a venue its own step.
 *
 * All of it is meant to be edited or deleted. Vegetarian throughout.
 */
object StarterRecipes {

    /**
     * Bump this whenever a recipe is added below.
     *
     * Forgetting to is the one way this goes wrong: the recipes ship inside the
     * APK but no existing diary merges them, so they appear on a fresh install
     * and nowhere else. [RecipeSeedTest] fails if the count here and the number
     * of recipes stop agreeing, which is what makes forgetting it loud.
     */
    const val VERSION = 2

    /**
     * The version this seed was at when the merge arrived, in schema 17.
     *
     * A diary written before then is stamped with *this*, not [VERSION], so
     * recipes added in the very release that introduced the merge still reach
     * it. See the 16 -> 17 step in
     * [dev.mahourigan.fooddiary.data.LocalDiaryRepository] for what that costs
     * and how it is paid for.
     */
    const val VERSION_AT_MERGE = 1

    /**
     * The ids the seed held at [VERSION_AT_MERGE].
     *
     * Written out rather than derived, because it is a fact about a past
     * release and [all] has moved on since. The 16 -> 17 migration reads a
     * diary missing one of *these* as a deletion; reading it from [all] would
     * mean every newly added recipe looked like one too, and the migration
     * would retire the very recipes it exists to let through.
     */
    val ID_AT_MERGE: Set<String> = setOf(
        "starter-cafe-scramble",
        "starter-cafe-mushrooms",
        "starter-greek-salad",
        "starter-dhal",
        "starter-pasta-pesto",
        "starter-cheese-toastie",
    )

    private fun id(name: String) = SeedIngredients.slug(name)

    private fun item(name: String, portion: Portion? = null) =
        MealItem(ingredientId = id(name), portion = portion)

    val all: List<Recipe> = listOf(

        // ---- Cafe: dishes you order by name ---------------------------------
        Recipe(
            id = "starter-cafe-scramble",
            name = "Chilli scramble",
            tags = setOf("Cafe"),
            items = listOf(
                item("Egg, whole", Portion.of(3.0, "egg")),
                item("Sourdough, wheat", Portion.of(2.0, "slice")),
                item("Chilli, fresh"),
                item("Butter"),
                item("Spinach"),
                item("Feta"),
            ),
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-cafe-mushrooms",
            name = "Mushrooms on toast",
            tags = setOf("Cafe"),
            items = listOf(
                item("Mushrooms, button", Portion.of(150.0, "g")),
                item("Sourdough, wheat", Portion.of(2.0, "slice")),
                item("Garlic", Portion.of(1.0, "clove")),
                item("Butter"),
                item("Parsley"),
            ),
            isUserCreated = false,
        ),

        // ---- Whole meals ----------------------------------------------------
        Recipe(
            id = "starter-greek-salad",
            name = "Greek salad",
            items = listOf(
                item("Cucumber"),
                item("Tomato"),
                item("Red onion", Portion.of(PortionSize.LITTLE)),
                item("Feta"),
                item("Olives"),
                item("Olive oil"),
                item("Oregano"),
            ),
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-dhal",
            name = "Red lentil dhal",
            items = listOf(
                item("Lentils, red", Portion.of(100.0, "g")),
                item("Onion"),
                item("Garlic", Portion.of(2.0, "clove")),
                item("Ginger"),
                item("Cumin"),
                item("Turmeric"),
                item("Tinned tomatoes"),
                item("Coconut cream"),
                item("Coriander leaf"),
            ),
            // Dhal is eaten with rice or bread far more often than not, which is
            // the combination the picker should offer rather than making you save
            // "dhal and rice" as a separate recipe.
            suggests = listOf(id("Rice, basmati"), id("Naan"), id("Yoghurt, natural")),
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-pasta-pesto",
            name = "Pasta with pesto",
            items = listOf(
                item("Pasta, wheat", Portion.of(100.0, "g")),
                item("Pesto", Portion.of(2.0, "tbsp")),
                item("Parmesan"),
                item("Cherry tomatoes"),
                item("Rocket"),
            ),
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-tofu-wrap",
            name = "Tofu and avocado wrap",
            items = listOf(
                item("Tortilla, wheat", Portion.of(1.0, "whole")),
                item("Tofu, firm", Portion.of(100.0, "g")),
                item("Avocado", Portion.of(35.0, "g")),
                item("Hummus", Portion.of(25.0, "g")),
                item("Sweet chilli sauce", Portion.of(25.0, "g")),
                item("Cucumber", Portion.of(100.0, "g")),
                item("Tomato", Portion.of(0.5, "whole")),
                item("Red onion", Portion.of(PortionSize.LITTLE)),
            ),
            // Read off the packets rather than estimated from the library: the
            // wrap is 172 on its own, the hummus 253 per 100 g and the sweet
            // chilli 1220 kJ per 100 g. Left stated so it survives the library's
            // own figures being generic -- a seeded tortilla is a 45 g one, and
            // this is not. Clear the field in the editor to go back to the
            // estimate, and change it if the quantities above change.
            kcal = 540,
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-tofu-lettuce-wrap",
            name = "Tofu and avocado lettuce wraps",
            items = listOf(
                item("Lettuce", Portion.of(70.0, "g")),
                item("Tofu, firm", Portion.of(100.0, "g")),
                item("Avocado", Portion.of(35.0, "g")),
                item("Hummus", Portion.of(25.0, "g")),
                item("Sweet chilli sauce", Portion.of(25.0, "g")),
                item("Cucumber", Portion.of(100.0, "g")),
                item("Tomato", Portion.of(0.5, "whole")),
                item("Red onion", Portion.of(PortionSize.LITTLE)),
            ),
            // The same filling in cos leaves rather than a tortilla. Kept as
            // its own recipe rather than a note on [starter-tofu-wrap] because
            // the difference is the point: the tortilla is the only wheat,
            // gluten and fructan in either, so eating both over a few weeks
            // gives the analysis the same meal with and without all three.
            // One recipe edited back and forth would record that as one food.
            kcal = 380,
            isUserCreated = false,
        ),
        Recipe(
            id = "starter-cheese-toastie",
            name = "Cheese toastie",
            items = listOf(
                item("Wheat bread, wholemeal", Portion.of(2.0, "slice")),
                item("Cheddar, mature"),
                item("Butter"),
                item("Mustard"),
            ),
            isUserCreated = false,
        ),
    )

    /**
     * Adds recipes a newer build knows about, leaving everything else alone.
     *
     * Deliberately the same shape as [SeedIngredients.mergeInto]: matching on
     * id and skipping anything already present means a recipe you have edited
     * keeps your version, and one you deleted stays deleted.
     */
    fun mergeInto(existing: List<Recipe>, retired: Set<String>): List<Recipe> {
        val known = existing.map { it.id }.toSet()
        return existing + all.filter { it.id !in known && it.id !in retired }
    }
}
