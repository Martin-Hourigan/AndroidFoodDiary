@file:UseSerializers(
    dev.mahourigan.fooddiary.data.LocalDateTimeSerializer::class,
)

package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

/**
 * A meal you eat often enough to not want to rebuild each time.
 *
 * [timesUsed] and [lastUsedAt] exist so the picker can lead with what you
 * actually eat rather than an alphabetical list. Logging tonight's dinner should
 * be one tap most nights, and it only is if the right eight things are on top.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class Recipe(
    val id: String = "",
    val name: String = "",
    val items: List<MealItem> = emptyList(),
    val notes: String = "",
    val isFavourite: Boolean = false,
    val timesUsed: Int = 0,
    val lastUsedAt: LocalDateTime? = null,
    val isUserCreated: Boolean = true,

    /**
     * Calories for the whole thing, when you worked it out once and would
     * rather it wasn't re-estimated from the ingredients every time.
     *
     * The *whole thing*, matching [items] and [serves] — not one portion.
     */
    val kcal: Int? = null,

    /**
     * How many people the quantities in [items] feed.
     *
     * A cooked dish is written down the way it's cooked — two tins of beans,
     * 250 g of tofu — and then eaten a quarter at a time. Without this the app
     * would log four dinners' worth of GOS every time you had one, and the
     * calorie total would be nonsense.
     *
     * Null means one, so anything written as a single serving keeps working
     * without being told.
     */
    val serves: Int? = null,

    /**
     * Free-text tags: "Cafe", "Soups", "Kettle Black".
     *
     * Each one becomes a chip in the picker holding everything carrying it —
     * recipes *and* ingredients, since [Ingredient.tags] shares this namespace.
     * A café order is a whole recipe; a hash brown is a single ingredient; both
     * belong behind Cafe and neither is inferred from the other.
     *
     * Deliberately *not* a base-versus-topping classification. That isn't a real
     * property of food — is avocado a topping or a meal, is rice the base or is
     * the curry — and maintaining it would be a running argument with yourself.
     * This is only ever a place to put things so a long list stays navigable.
     *
     * Many, and flat. A single nested string ("Cafe/Kettle Black") said the same
     * thing worse: two tags on one recipe means browsing either finds it.
     */
    val tags: Set<String> = emptySet(),

    /**
     * The old single nested folder, kept only so a file written before tags can
     * still be read. Migration moves it into [tags] and clears it.
     */
    @SerialName("group")
    val legacyGroup: String? = null,

    /**
     * Ingredient ids this is usually eaten with — a day-one hint, nothing more.
     *
     * On a fresh install there is no log to learn "toast goes with butter" from,
     * so the bundled components carry the answer and the picker has something to
     * offer on the first morning. It costs you nothing to maintain because it
     * only ships with the seed data, and real usage overtakes it within a week.
     */
    val suggests: List<String> = emptyList(),
) {
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() ||
            name.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }

    fun taggedWith(tag: String): Boolean = tags.any { it.equals(tag, ignoreCase = true) }
}

fun Recipe.attributes(byId: Map<String, Ingredient>): Set<Attribute> =
    items.flatMapTo(mutableSetOf()) { byId[it.ingredientId]?.attributes.orEmpty() }

/**
 * Ordering for the picker: favourites, then most recently eaten, then the rest.
 *
 * Recency beats frequency deliberately — a recipe you had yesterday is a better
 * bet for tonight than one you had forty times last year and got bored of.
 */
fun List<Recipe>.forPicker(): List<Recipe> = sortedWith(
    compareByDescending<Recipe> { it.isFavourite }
        .thenByDescending { it.lastUsedAt ?: LocalDateTime.MIN }
        .thenByDescending { it.timesUsed }
        .thenBy { it.name.lowercase() },
)

/** Servings, treating an unstated or nonsensical count as one. */
val Recipe.servings: Int get() = serves?.coerceAtLeast(1) ?: 1

/**
 * One portion's worth of the ingredients.
 *
 * Amounts are divided; coarse portions ("a little") are left alone, because a
 * quarter of "a little" isn't a thing anyone can picture and the bucket was
 * never a quantity in the first place. An item with no portion stays that way.
 *
 * This is what goes into a meal, and it's also what the [Basis] snapshot
 * records — the snapshot's job is to say what this meal took from the recipe,
 * which is one serving, not the pot.
 */
fun Recipe.perServing(): List<MealItem> {
    val n = servings
    if (n == 1) return items
    return items.map { item ->
        val amount = item.portion?.amount ?: return@map item
        item.copy(portion = item.portion.copy(amount = amount / n))
    }
}

/** The stated whole-dish figure, divided down to one portion. */
val Recipe.kcalPerServing: Int?
    get() = kcal?.let { (it.toDouble() / servings).toInt() }
