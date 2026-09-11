@file:UseSerializers(
    dev.mahourigan.fooddiary.data.LocalDateTimeSerializer::class,
)

package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
data class MealItem(
    val ingredientId: String = "",
    val portion: Portion? = null,

    /**
     * How this particular one was made: sourdough, wholemeal, multigrain.
     *
     * On the item rather than on the ingredient, because a wholemeal sourdough
     * multigrain loaf is *one* bread described three ways — adding three
     * ingredients would say you ate three different breads, which is both false
     * and ruinous for the analysis. Kept as attributes so they rank as suspects
     * like anything else: "sourdough" is a testable factor in its own right.
     */
    val modifiers: Set<Attribute> = emptySet(),
)

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    DRINK("Drink"),
    ;

    companion object {
        /**
         * A guess from the clock, always overridable.
         *
         * Only ever a default for a field the user can change, so the exact
         * boundaries don't matter much — being wrong about a 16:00 snack costs
         * one tap.
         */
        fun fromTime(time: LocalTime): MealType = when (time.hour) {
            in 4..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 15..16 -> SNACK
            in 17..21 -> DINNER
            else -> SNACK
        }
    }

    /** The stand-in hour for "sometime that morning". Never presented as exact. */
    val typicalHour: Int
        get() = when (this) {
            BREAKFAST -> 8
            LUNCH -> 13
            DINNER -> 19
            SNACK -> 16
            DRINK -> 11
        }

    /** How a vague time reads back: "that morning", "that evening". */
    val vagueLabel: String
        get() = when (this) {
            BREAKFAST -> "that morning"
            LUNCH -> "around midday"
            DINNER -> "that evening"
            SNACK -> "that afternoon"
            DRINK -> "sometime that day"
        }
}

/**
 * A saved thing a meal was built from, frozen at the moment it was logged.
 *
 * The snapshot exists so the `+ avocado / − red onion` display stays truthful
 * forever. Diffing against the *live* recipe would rewrite history every time a
 * recipe is edited: fix the Greek Salad in March and last October's entry would
 * start claiming you'd added things you hadn't.
 */
@Serializable
data class Basis(
    val recipeId: String = "",
    val name: String = "",
    val itemsAtLogTime: List<MealItem> = emptyList(),
)

/**
 * Something eaten, at a time.
 *
 * [items] is the complete, resolved ingredient list — never a diff against a
 * recipe. This is the most load-bearing decision in the model: the log is
 * evidence, and evidence that changes when you tidy up a recipe is worthless.
 *
 * [at] is wall-clock local time rather than an instant. "Dinner at 19:30" means
 * 19:30 wherever you were, and turning it into an instant would shuffle the day
 * around the moment you flew somewhere — which would land meals and symptoms in
 * the wrong lag windows relative to each other.
 */
@Serializable
data class MealEntry(
    val id: String = "",
    val at: LocalDateTime = LocalDateTime.MIN,
    val mealType: MealType? = null,

    /**
     * The saved things this was built from — often none, sometimes several.
     *
     * A list rather than one, because most meals are assembled: toast *and* a
     * topping, curry *and* rice, dinner *and* a glass of wine. Forcing that into
     * a single basis would mean a separate saved recipe for every combination
     * you ever eat, which is how a library becomes unusable.
     */
    val bases: List<Basis> = emptyList(),

    /**
     * The single basis a v1 file held. Read on load, folded into [bases] and
     * cleared — see the migration in `LocalDiaryRepository`.
     */
    @SerialName("basis")
    val legacyBasis: Basis? = null,

    val items: List<MealItem> = emptyList(),

    /**
     * Calories you typed for this meal, which beat anything worked out.
     *
     * For packaged food, or a cafe that publishes the number — the times you
     * genuinely know, and an estimate would be a downgrade.
     */
    val kcalOverride: Int? = null,

    /**
     * True when [at] is a stand-in rather than a time you gave.
     *
     * "I had it sometime that morning" is a real answer and shouldn't be refused.
     * But a meal with *no* time can't be related to a symptom at all — every lag
     * window is measured from it — so instead of storing nothing, the meal type's
     * usual hour is used and flagged. The entry stays analysable, and the
     * analysis can widen its windows or discount it rather than treating a guess
     * as though you'd looked at a clock.
     */
    val timeApproximate: Boolean = false,

    val notes: String = "",
) {
    val isAdHoc: Boolean get() = bases.isEmpty()

    /**
     * What to call it in a list.
     *
     * The saved parts if there were any; otherwise just which meal it was. It
     * deliberately doesn't fall back to reading out the ingredients, because the
     * line underneath already does that — printing the same list twice is how
     * the day view ended up saying everything twice.
     */
    fun title(byId: Map<String, Ingredient>): String = bases
        .mapNotNull { it.name.takeIf { name -> name.isNotBlank() } }
        .joinToString(" + ")
        .ifBlank { mealType?.label.orEmpty() }
        .ifBlank { "Meal" }
}

/**
 * How a logged meal differs from the saved things it was built on.
 *
 * Computed for display only — the entry itself always holds the full list.
 */
data class MealDiff(
    val added: List<MealItem> = emptyList(),
    val removed: List<MealItem> = emptyList(),
    val reportioned: List<MealItem> = emptyList(),
) {
    val isUnchanged: Boolean
        get() = added.isEmpty() && removed.isEmpty() && reportioned.isEmpty()
}

/**
 * Null for an ad-hoc meal, which has nothing to differ from.
 *
 * With several parts the comparison is against their union, so a chilli on toast
 * and avocado reads as one `+ chilli` rather than being blamed on whichever part
 * happened not to contain it.
 */
fun MealEntry.diffFromBases(): MealDiff? {
    if (bases.isEmpty()) return null

    // Distinct by id: if two parts both bring olive oil, it's expected once.
    val original = bases.flatMap { it.itemsAtLogTime }.distinctBy { it.ingredientId }

    val originalById = original.associateBy { it.ingredientId }
    val currentById = items.associateBy { it.ingredientId }

    return MealDiff(
        added = items.filter { it.ingredientId !in originalById },
        removed = original.filter { it.ingredientId !in currentById },
        reportioned = items.filter { item ->
            val before = originalById[item.ingredientId] ?: return@filter false
            before.portion != item.portion
        },
    )
}

/**
 * Every attribute the meal carries, inherited from its ingredients.
 *
 * Nothing tags a meal by hand. Hand-tagging is where food diaries go wrong: the
 * day you forget the soy sauce had wheat in it is the day the data starts lying,
 * and you don't find out for six weeks.
 */
fun MealEntry.attributes(byId: Map<String, Ingredient>): Set<Attribute> =
    items.flatMapTo(mutableSetOf()) { item ->
        byId[item.ingredientId]?.attributes.orEmpty() + item.modifiers
    }

/**
 * Which ingredients are responsible for an attribute being present.
 *
 * This is what makes a warning actionable: "contains dairy" is an argument,
 * "contains dairy — feta" is something you can do something about.
 */
fun MealEntry.carriersOf(attribute: Attribute, byId: Map<String, Ingredient>): List<Ingredient> =
    items.mapNotNull { byId[it.ingredientId] }.filter { it.has(attribute) }.distinctBy { it.id }
