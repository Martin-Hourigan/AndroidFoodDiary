package dev.mahourigan.fooddiary.domain.analysis

import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MealType
import dev.mahourigan.fooddiary.domain.Recipe
import kotlin.math.pow

/**
 * What to offer next while building a meal.
 *
 * The whole point is that logging your usual breakfast should be four taps, and
 * it only is if the right four things are already on screen. So the picker is
 * ordered by what you actually eat — learned from your own log, not declared by
 * you in advance and not guessed at by anything.
 *
 * Purely computed, like everything else that reads the log: counts, ratios and a
 * fixed decay. The same diary always produces the same order.
 */

/** Something you can tap to add to a meal. */
sealed interface Suggestion {

    val label: String

    /** A saved recipe or component — adds all of its items. */
    data class SavedMeal(val recipe: Recipe) : Suggestion {
        override val label: String get() = recipe.name
    }

    /** A category you answer questions inside, rather than a list to scroll. */
    data class BuildIt(val category: dev.mahourigan.fooddiary.domain.Category) : Suggestion {
        override val label: String get() = category.name
    }

    /** A bare ingredient — adds one item. Butter needs no recipe. */
    data class Loose(val ingredient: Ingredient) : Suggestion {
        override val label: String get() = ingredient.name
    }

    /**
     * A folder to step into. [path] is the full path so far, so tapping "Kettle
     * Black" inside "Cafe" navigates to ["Cafe", "Kettle Black"].
     */
    data class Group(val path: List<String>, val count: Int) : Suggestion {
        override val label: String get() = path.lastOrNull().orEmpty()
    }
}

data class ScoredSuggestion(val suggestion: Suggestion, val score: Double)

/**
 * Ordering for the picker.
 *
 * Three signals, multiplied rather than added so that a thing has to be
 * plausible on every axis to reach the top:
 *
 * - **Habit.** How often it's been used, damped so last month still counts but
 *   this week counts more.
 * - **Time of day.** Used at this meal type, because porridge at 19:00 is not a
 *   suggestion anybody wants.
 * - **Goes-with.** Once something is in the meal, how often the candidate has
 *   appeared alongside it. This is the part that makes toast offer butter.
 *
 * Everything gets a small floor so a never-used ingredient can still be found by
 * searching rather than being scored into oblivion.
 */
object Suggestions {

    /**
     * The path of the door that holds every recipe.
     *
     * A reserved folder name rather than a separate mode, so browsing, the
     * breadcrumb and the back button all keep working through one mechanism. A
     * folder you happen to name "Recipes" merges into it, which is harmless —
     * everything is in there anyway.
     */
    const val ALL_RECIPES = "Recipes"


    /** Weight of a use n meals ago, halving roughly every 40 meals logged. */
    private const val DECAY = 0.983

    private const val FLOOR = 0.01


    /** What a use at a different meal type is worth. Enough to find, not to lead. */
    private const val CROSS_MEAL = 0.15

    /**
     * The top of the picker: categories, then a chip per tag, then the door to
     * every recipe.
     *
     * A tag chip holds whatever carries that tag — recipes and ingredients
     * alike. No bare untagged ingredients and no flat list of every recipe;
     * those live behind [ALL_RECIPES], where you look for them on purpose.
     */
    fun categories(
        categories: List<dev.mahourigan.fooddiary.domain.Category>,
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        meals: List<MealEntry>,
        mealType: MealType?,
    ): List<Suggestion> {
        val history = History(meals)

        val built = categories.map { category ->
            // A category ranks by the best its options have done, so the one you
            // reach for every morning leads.
            val best = category.facets
                .flatMap { it.options }
                .mapNotNull { it.ingredientId }
                .maxOfOrNull { history.ingredientScore(it, mealType, emptySet()) }
                ?: 0.0
            ScoredSuggestion(Suggestion.BuildIt(category), best)
        }

        // One chip per tag, counting recipes and ingredients together, ranked by
        // the best thing in it. Case-insensitive but shown as first written, so
        // "cafe" and "Cafe" are one chip rather than two.
        val byTag = linkedMapOf<String, MutableList<Double>>()
        val display = linkedMapOf<String, String>()
        fun note(tag: String, score: Double) {
            val key = tag.lowercase()
            display.putIfAbsent(key, tag)
            byTag.getOrPut(key) { mutableListOf() }.add(score)
        }
        // A tag matching a category's name is absorbed by that category rather
        // than getting a chip of its own. Toast is already on this screen; a
        // second Toast chip beside it would be two doors to the same idea, and
        // the recipes tagged Toast are exactly what "the usual" means inside it.
        val absorbed = categories.mapTo(mutableSetOf()) { it.name.lowercase() }
        fun free(tag: String) = tag.lowercase() !in absorbed

        recipes.forEach { recipe ->
            recipe.tags.filter(::free).forEach {
                note(it, history.score(recipe.id, mealType, emptySet()))
            }
        }
        ingredients.forEach { ingredient ->
            ingredient.tags.filter(::free).forEach {
                note(it, history.ingredientScore(ingredient.id, mealType, emptySet()))
            }
        }

        val tagChips = byTag.map { (key, scores) ->
            ScoredSuggestion(
                Suggestion.Group(listOf(display.getValue(key)), scores.size),
                scores.max(),
            )
        }

        // Categories and tag chips rank against each other, then the door to
        // everything sits last. A catch-all doesn't compete for position: it's
        // where you go when the shortcuts didn't have it.
        val doors = (built + tagChips).sortedByDescending { it.score }.map { it.suggestion }
        return if (recipes.isEmpty()) {
            doors
        } else {
            doors + Suggestion.Group(listOf(ALL_RECIPES), recipes.size)
        }
    }

    /**
     * The saved things that count as a usual for a category.
     *
     * A recipe tagged with the category's name. That's the whole rule: no
     * overlap-of-ingredients guessing, and no second field to keep in step —
     * tagging a toast "Toast" is what puts it at the top of Toast, and it's the
     * same tag mechanism as everything else.
     */
    fun usualsFor(
        category: dev.mahourigan.fooddiary.domain.Category,
        recipes: List<Recipe>,
        meals: List<MealEntry>,
        mealType: MealType?,
    ): List<Recipe> {
        val history = History(meals)
        return recipes
            .filter { it.taggedWith(category.name) }
            .sortedByDescending { history.score(it.id, mealType, emptySet()) }
    }

    /**
     * What's inside a chip.
     *
     * Membership is **declared, never inferred**. A recipe or an ingredient is
     * in here because it carries the tag, full stop.
     *
     * This used to be worked out: co-occurrence with the tag's recipes, plus
     * their ingredients, plus bundled hints, scored against a floor. That made
     * the inference transitive and the result nonsense — "Mushrooms on toast"
     * contains sourdough, so a dinner of sourdough and rice put rice in the
     * café. One shared ingredient dragged in everything ever eaten beside it.
     * Guessing membership was the whole mistake, so nothing guesses now.
     */
    fun forPicker(
        recipes: List<Recipe>,
        ingredients: List<Ingredient>,
        meals: List<MealEntry>,
        mealType: MealType?,
        currentItems: Set<String>,
        currentPath: List<String> = emptyList(),
        limit: Int = 40,
    ): List<Suggestion> {
        val history = History(meals)
        val tag = currentPath.firstOrNull() ?: return emptyList()

        // The door to everything: every recipe, flat, whatever it's tagged.
        // The tag chips are shortcuts to a subset and this is the full list, so
        // a café order is reachable both ways on purpose.
        val everything = tag.equals(ALL_RECIPES, ignoreCase = true)

        val here = recipes
            .filter { everything || it.taggedWith(tag) }
            .map {
                ScoredSuggestion(Suggestion.SavedMeal(it), history.score(it.id, mealType, currentItems))
            }

        // Ingredients only where they were tagged, and never inside the
        // all-recipes door, which is about recipes by name.
        val loose = if (everything) {
            emptyList()
        } else {
            ingredients
                .filter { it.taggedWith(tag) && it.id !in currentItems }
                .map {
                    ScoredSuggestion(
                        Suggestion.Loose(it),
                        history.ingredientScore(it.id, mealType, currentItems),
                    )
                }
        }

        return (here + loose)
            .sortedByDescending { it.score }
            .map { it.suggestion }
            .take(limit)
    }


    /**
     * Counts pulled from the log once, rather than re-walked per candidate.
     *
     * A few thousand meals is small enough that this is cheap, and doing it here
     * keeps the scoring itself a pure function of counts.
     */
    private class History(meals: List<MealEntry>) {

        private val recipeUse = mutableMapOf<String, Double>()
        private val recipeByType = mutableMapOf<Pair<String, MealType>, Double>()
        private val ingredientUse = mutableMapOf<String, Double>()
        private val ingredientByType = mutableMapOf<Pair<String, MealType>, Double>()

        /** How often B appeared in a meal that also contained A. */
        private val together = mutableMapOf<Pair<String, String>, Double>()

        init {
            // Newest first, so index 0 is the most recent meal and carries full weight.
            val ordered = meals.sortedByDescending { it.at }

            ordered.forEachIndexed { index, meal ->
                val weight = DECAY.pow(index)
                val type = meal.mealType
                val itemIds = meal.items.map { it.ingredientId }.distinct()

                meal.bases.forEach { basis ->
                    recipeUse.merge(basis.recipeId, weight, Double::plus)
                    if (type != null) recipeByType.merge(basis.recipeId to type, weight, Double::plus)

                    // A part goes with everything else that was in that meal.
                    itemIds.forEach { id -> together.merge(basis.recipeId to id, weight, Double::plus) }
                }

                itemIds.forEach { id ->
                    ingredientUse.merge(id, weight, Double::plus)
                    if (type != null) ingredientByType.merge(id to type, weight, Double::plus)
                    itemIds.forEach { other ->
                        if (other != id) together.merge(id to other, weight, Double::plus)
                    }
                }
            }
        }

        fun score(recipeId: String, mealType: MealType?, currentItems: Set<String>): Double =
            combine(
                habit = recipeUse[recipeId] ?: 0.0,
                atType = mealType?.let { recipeByType[recipeId to it] } ?: 0.0,
                goesWith = goesWith(recipeId, currentItems),
            )

        fun ingredientScore(id: String, mealType: MealType?, currentItems: Set<String>): Double =
            combine(
                habit = ingredientUse[id] ?: 0.0,
                atType = mealType?.let { ingredientByType[id to it] } ?: 0.0,
                goesWith = goesWith(id, currentItems),
            )

        /**
         * Scored against what's already in the meal, keyed both ways round: the
         * candidate may be a recipe whose id was recorded against ingredients,
         * or an ingredient recorded against a recipe.
         */
        private fun goesWith(id: String, currentItems: Set<String>): Double {
            if (currentItems.isEmpty()) return 0.0
            return currentItems.sumOf { present ->
                (together[id to present] ?: 0.0) + (together[present to id] ?: 0.0)
            }
        }

        /**
         * How strongly something is associated with a folder's contents.
         *
         * This is what decides that butter belongs in Toast: not a declaration,
         * but the fact that you keep eating them together.
         */

        private fun combine(habit: Double, atType: Double, goesWith: Double): Double {
            // Habit is counted *at this meal type*, with use elsewhere worth only
            // a fraction. A curry eaten every night should barely register at
            // breakfast — adding a flat bonus for the right meal type instead
            // left raw frequency dominating, so dinner leaked into the morning.
            val h = 1.0 + atType + habit * CROSS_MEAL

            // Multiplied rather than added, so a thing has to be plausible on
            // both axes to reach the top; being new on one isn't fatal.
            val g = 1.0 + goesWith * 3.0
            return h * g - 1.0 + FLOOR
        }
    }
}
