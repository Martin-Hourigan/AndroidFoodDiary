@file:UseSerializers(
    LocalDateSerializer::class,
)

package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.DayLog
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MedEntry
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.Settings
import dev.mahourigan.fooddiary.domain.StoolEntry
import dev.mahourigan.fooddiary.domain.SymptomEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/**
 * Everything the app knows, in one serialisable root.
 *
 * Split conceptually into the *library* — ingredients and recipes, small and
 * slow-changing — and the *log*, which grows for as long as you use the app.
 * Only the log would ever need sharding by year, and [schemaVersion] is here
 * from the first release so that day is a migration rather than a rescue.
 */
@Serializable
data class DiarySnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA,

    /** Display preferences. Nothing here changes what is stored. */
    val settings: Settings = Settings(),

    // The library
    val ingredients: List<Ingredient> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val categories: List<Category> = emptyList(),

    // The log
    val meals: List<MealEntry> = emptyList(),
    val symptoms: List<SymptomEntry> = emptyList(),
    val stools: List<StoolEntry> = emptyList(),
    val meds: List<MedEntry> = emptyList(),
    val days: List<DayLog> = emptyList(),

    /** The version of the bundled library already merged in, so a reseed is idempotent. */
    val seedVersion: Int = 0,

    /** Bundled category version, merged in the same way as the ingredients. */
    val categorySeedVersion: Int = 0,

    /**
     * Seed ingredients you've deleted.
     *
     * Kept so a later version adding more to the bundled library doesn't
     * resurrect the ones you threw out. Without this, "delete" means "until the
     * next update", which is the kind of small betrayal that makes an app feel
     * broken.
     */
    val retiredIngredientIds: Set<String> = emptySet(),
) {
    /** The lookup everything in the domain layer wants; built once per snapshot. */
    val ingredientsById: Map<String, Ingredient> by lazy { ingredients.associateBy { it.id } }

    val recipesById: Map<String, Recipe> by lazy { recipes.associateBy { it.id } }

    fun dayLog(date: LocalDate): DayLog = days.firstOrNull { it.date == date } ?: DayLog(date = date)

    companion object {
        /**
         * 1 → 2: a meal's single `basis` became a list of `bases`, so a meal can
         * be assembled from several saved things (toast *and* a topping).
         *
         * 2 → 3: toast and porridge stopped being folders of near-identical
         * saved meals and became categories you answer questions about, so the
         * bundled components they replaced are retired.
         *
         * 3 → 4: some bundled ingredients were renamed, and an id is derived
         * from the name, so anything already logged has to be pointed at the
         * new one.
         *
         * 4 → 5: a category's several lists of addable things ("On it", "Made
         * with", "Sweetened") became one called Ingredients.
         *
         * 5 → 6: a style facet named after its own category ("Coffee › Coffee")
         * becomes "Type".
         *
         * 6 → 7: coffee stopped asking black/espresso/decaf, and dairy milk came
         * out of the option lists — this household doesn't drink it.
         *
         * 7 → 8: the coffee in the Coffee category stopped being something you
         * tick and became something the category simply includes.
         *
         * 8 → 9: rapeseed oil is called canola here.
         *
         * 9 → 10: and aubergine, courgette and pepper are eggplant, zucchini
         * and capsicum. The other words stay as search aliases.
         *
         * 10 → 11: bundled ingredients adopt attributes the seed has gained
         * since — seed oils, here — so a library that was merged in months ago
         * doesn't sit there missing a tag the analysis is counting on.
         *
         * 11 → 12: ingredients gained roles and facets gained role
         * subscriptions, so a category's list can fill itself in instead of
         * being written out by hand. Both are adopted from the seed on the same
         * terms as attributes — anything you've edited is left as you left it.
         *
         * 12 → 13: search aliases are adopted the same way. Adding "nooch" to
         * nutritional yeast in the seed would otherwise only ever work on a
         * fresh install, which makes the seed's aliases a lie everywhere else.
         *
         * 13 → 14: a recipe's single nested folder ("Cafe/Kettle Black") became
         * a set of flat tags, shared with ingredients. Splitting on the slash
         * keeps both halves as tags, so browsing either still finds it.
         */
        const val CURRENT_SCHEMA = 15

        /** Options dropped from the bundled categories. Still in the library. */
        val DROPPED_CATEGORY_OPTIONS: Set<String> = setOf(
            "cow-s-milk",
            "cow-s-milk-lactose-free",
        )

        /**
         * Bundled ingredients that changed id because their name changed.
         *
         * An entry's items hold ids, so a rename without this would leave a
         * logged meal referring to something that no longer exists — the day
         * would still show it, by falling back to the raw id, but its attributes
         * would silently vanish from every count.
         */
        val RENAMED_INGREDIENTS: Map<String, String> = mapOf(
            "yeast-extract-spread" to "vegemite",
            "rapeseed-oil" to "canola-oil",
            "aubergine" to "eggplant",
            "courgette" to "zucchini",
            "red-pepper" to "red-capsicum",
            "green-pepper" to "green-capsicum",
        )

        /** Bundled recipes that categories replaced. Removed on upgrade. */
        val RETIRED_STARTERS: Set<String> = setOf(
            "starter-toast-sourdough", "starter-toast-wholemeal", "starter-toast-gf",
            "starter-porridge-oats", "starter-porridge-gf-oat",
            "starter-flat-white", "starter-black-coffee", "starter-tea",
        )
    }
}
