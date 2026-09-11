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
}
