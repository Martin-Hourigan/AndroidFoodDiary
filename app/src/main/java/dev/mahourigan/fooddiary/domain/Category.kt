package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable

/**
 * A way of building a meal by answering a couple of questions.
 *
 * Toast isn't a list of saved meals — nobody wants "Toast, sourdough", "Toast,
 * wholemeal", "Toast, gluten-free" as three separate things to scroll past. It's
 * one question about the bread and another about what went on it.
 *
 * So a category is a base ingredient plus [facets]: each facet is a labelled
 * multi-select, and between them they describe the meal.
 */
@Serializable
data class Category(
    val id: String = "",
    val name: String = "",

    /**
     * What using this category means you ate, before any question is answered.
     *
     * A coffee contains coffee. Making that a tick inside the Coffee category is
     * asking you to state the obvious, and an option you always choose is not a
     * choice. Toast has none of these, because which bread it was is a real
     * question.
     */
    val always: List<MealItem> = emptyList(),

    val facets: List<Facet> = emptyList(),
    val isUserCreated: Boolean = false,
)

@Serializable
enum class FacetKind {
    /**
     * Describes one thing rather than adding several.
     *
     * "Wholemeal + sourdough + multigrain" is a single loaf with three
     * characteristics, so the options here resolve to one ingredient carrying
     * the lot as modifiers.
     */
    STYLE,

    /** Adds an ingredient each. Toppings, extras, sides. */
    ITEMS,
}

@Serializable
data class Facet(
    val name: String = "",
    val kind: FacetKind = FacetKind.ITEMS,

    /**
     * For a [FacetKind.STYLE] facet: what's being described when no option says
     * otherwise. Bread styles land on wheat bread unless you pick gluten-free.
     */
    val defaultIngredientId: String? = null,

    val options: List<FacetOption> = emptyList(),

    /**
     * Roles this facet will offer beyond its own [options].
     *
     * The options list is the shortlist — what you actually reach for, in the
     * order you reach for it. A subscription says what else is *eligible*, so a
     * jar of peanut butter tagged [Roles.Spread] turns up under Toast and
     * Porridge without being written into either by hand. Kept behind a "more"
     * so subscribing to something broad doesn't bury the shortlist.
     */
    val roles: Set<Role> = emptySet(),
)

@Serializable
data class FacetOption(
    val label: String = "",

    /**
     * The ingredient this adds (an ITEMS facet), or the one it switches the
     * subject to (a STYLE facet, e.g. picking gluten-free changes the loaf).
     */
    val ingredientId: String? = null,

    /** Attributes this contributes. Only meaningful on a STYLE facet. */
    val modifiers: Set<Attribute> = emptySet(),
) {
    /** A stable key for selection state; the label is what you see. */
    val key: String get() = ingredientId ?: label.lowercase().replace(' ', '-')
}

/**
 * What a set of choices adds up to.
 *
 * Style facets collapse to one item each — the base ingredient, carrying every
 * modifier that was ticked. Item facets contribute one apiece. The result is an
 * ordinary list of [MealItem], so nothing downstream knows a category was
 * involved.
 */
fun Category.resolve(
    chosen: Map<String, Set<String>>,
    /**
     * The library, so a pick from a facet's role subscription can be resolved.
     *
     * Those options don't exist in [Facet.options] — they're built from the
     * library at display time — so without this they tick on screen and then
     * quietly fail to appear in the meal.
     */
    byId: Map<String, Ingredient> = emptyMap(),
): List<MealItem> = buildList {
    addAll(always)

    facets.forEach { facet ->
        val keys = chosen[facet.name].orEmpty()
        if (keys.isEmpty()) return@forEach
        val picked = facet.options.filter { it.key in keys }

        when (facet.kind) {
            FacetKind.STYLE -> {
                // Last option that names an ingredient wins the argument about
                // what the thing actually is; the rest just describe it.
                val ingredientId = picked.lastOrNull { it.ingredientId != null }?.ingredientId
                    ?: facet.defaultIngredientId
                    ?: return@forEach
                add(
                    MealItem(
                        ingredientId = ingredientId,
                        modifiers = picked.flatMapTo(mutableSetOf()) { it.modifiers },
                    ),
                )
            }

            FacetKind.ITEMS -> {
                picked.forEach { option ->
                    option.ingredientId?.let { add(MealItem(ingredientId = it)) }
                }
                // Anything ticked that the shortlist has never heard of came
                // from the role subscription, where the key is the ingredient's
                // own id. Checked against the library rather than trusted, so a
                // stale key from a deleted ingredient adds nothing.
                val listed = facet.options.mapTo(mutableSetOf()) { it.key }
                keys.filter { it !in listed && it in byId }
                    .forEach { add(MealItem(ingredientId = it)) }
            }
        }
    }
}.mergeByIngredient()

/**
 * One entry per ingredient, keeping every modifier that landed on it.
 *
 * Naively de-duplicating would drop whichever copy came second, and with it any
 * "sourdough" a style facet had attached — so the merge unions the modifiers
 * rather than picking a winner.
 */
private fun List<MealItem>.mergeByIngredient(): List<MealItem> =
    groupBy { it.ingredientId }.map { (_, same) ->
        if (same.size == 1) {
            same.single()
        } else {
            same.first().copy(modifiers = same.flatMapTo(mutableSetOf()) { it.modifiers })
        }
    }

/**
 * What a facet offers on top of its shortlist, by role subscription.
 *
 * Anything already listed explicitly is left out, so the shortlist stays the
 * shortlist and this is only ever the overflow.
 */
fun Facet.alsoOffers(ingredients: List<Ingredient>): List<Ingredient> {
    if (roles.isEmpty()) return emptyList()
    val already = options.mapNotNull { it.ingredientId }.toSet()
    return ingredients
        .filter { it.id !in already && it.roles.any { role -> role in roles } }
        .sortedBy { it.name }
}
