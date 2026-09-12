package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable

/**
 * One thing you can eat, and what it carries.
 *
 * The library is the app's whole vocabulary: a meal is ingredients, and an
 * ingredient's [attributes] are what the analysis actually reasons over. Getting
 * a tag wrong here quietly corrupts every conclusion drawn later, which is why
 * the seed data is treated as real work and why every ingredient is editable.
 */
@Serializable
data class Ingredient(
    val id: String = "",
    val name: String = "",
    val attributes: Set<Attribute> = emptySet(),

    /**
     * What this is for — see [Role]. Nothing to do with the analysis; it only
     * decides which category lists offer it without being told to.
     */
    val roles: Set<Role> = emptySet(),

    /**
     * Free-text tags, sharing a namespace with [Recipe.tags].
     *
     * Where this shows up in the picker, declared rather than guessed. A hash
     * brown tagged "Cafe" appears behind the Cafe chip beside the café orders;
     * the sourdough *inside* one of those orders does not, because nobody
     * orders a slice of sourdough.
     *
     * Distinct from [roles] on purpose. A role is a fixed word that category
     * lists subscribe to ("spread" → offered under Toast); a tag is a bucket
     * you invented. Merging them would make "spread" a browsable chip.
     */
    val tags: Set<String> = emptySet(),

    /**
     * Other names for the same thing, so searching "aubergine" finds "eggplant".
     *
     * Also carries the brand-ish variants people actually type — "courgette"
     * for zucchini, "garbanzo" for chickpea.
     */
    val aliases: List<String> = emptyList(),

    /**
     * What *one* of this is: "slice", "clove", "g", "ml".
     *
     * Drives the portion stepper, so counting slices of bread never means
     * choosing a unit from a list first.
     */
    val defaultUnit: String? = null,

    /**
     * A normal serving in [defaultUnit] — the midpoint the coarse buckets sit
     * around. Null means exact amounts can be recorded but not bucketed.
     */
    val typicalAmount: Double? = null,

    /**
     * Energy per 100 g, or per 100 ml for anything liquid.
     *
     * Per-100 rather than per-serving because it's the figure printed on the
     * packet, and because it's the only one that works when you give an exact
     * weight. A serving is then just [gramsPerUnit] × how many.
     */
    val kcalPer100: Double? = null,

    /**
     * What one [defaultUnit] weighs — a slice of bread ≈ 40 g, a clove ≈ 3 g.
     *
     * This is what turns "2 slices" into grams and therefore into calories. For
     * an ingredient already measured in g or ml it's 1.
     *
     * Only meaningful for a *count*: a thing has no volume you can infer, so
     * each one needs its own weight. Anything spooned or poured uses
     * [densityGPerMl] instead, which answers every volume at once.
     */
    val gramsPerUnit: Double? = null,

    /**
     * Grams per millilitre, for anything measured by volume.
     *
     * The reason this is a density rather than a weight-per-spoon: a tablespoon
     * is 15 ml whatever is in it, so one number answers teaspoons, tablespoons
     * and cups together — including a unit nobody has added yet. Storing grams
     * per tablespoon would mean storing grams per teaspoon separately, and they
     * could then drift apart.
     *
     * It is also the only honest way to say that a tablespoon of chives (3 g)
     * and a tablespoon of maple syrup (20 g) are both tablespoons. Chives are
     * 0.2 g/ml and syrup is 1.33; the unit never carried the weight.
     *
     * Null for anything counted or weighed, where volume means nothing.
     */
    val densityGPerMl: Double? = null,

    /** True for anything you added or edited, so a reseed never overwrites it. */
    val isUserCreated: Boolean = false,
) {
    /** Everything this matches on in search, lowercased once at the call site. */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return name.lowercase().contains(q) || aliases.any { it.lowercase().contains(q) }
    }

    fun has(attribute: Attribute): Boolean = attribute in attributes

    fun isA(role: Role): Boolean = role in roles

    fun taggedWith(tag: String): Boolean = tags.any { it.equals(tag, ignoreCase = true) }
}
