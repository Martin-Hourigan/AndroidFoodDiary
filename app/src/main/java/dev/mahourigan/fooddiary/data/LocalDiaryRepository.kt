package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.TextSize
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.DayLog
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.EnergyUnit
import dev.mahourigan.fooddiary.ui.theme.ColourTheme
import dev.mahourigan.fooddiary.ui.theme.CustomPalette
import dev.mahourigan.fooddiary.ui.theme.TypeFace
import dev.mahourigan.fooddiary.ui.theme.ThemeMode
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Measures
import dev.mahourigan.fooddiary.domain.MedEntry
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.StoolEntry
import dev.mahourigan.fooddiary.domain.SymptomEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/**
 * The on-device store: one JSON file, held in memory, written on every change.
 *
 * Same shape as the tasks app, and for the same reason — the export and the
 * store are literally the same format, so what you back up is what the app
 * runs on and you can read it yourself.
 *
 * A diary does grow in a way a task list doesn't: three meals a day for three
 * years is a couple of megabytes, and rewriting that on every tap is fine but
 * not free. [DiarySnapshot.schemaVersion] exists from the first release so
 * sharding the log by year later is a migration rather than a rescue.
 */
class LocalDiaryRepository(private val file: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val writeLock = Mutex()
    private val _snapshot = MutableStateFlow(DiarySnapshot())
    val snapshot: StateFlow<DiarySnapshot> = _snapshot.asStateFlow()

    /**
     * Reads the file, seeding a fresh diary when there isn't one.
     *
     * Must be awaited before the UI reads [snapshot], or the first frame renders
     * an empty diary and then flickers.
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        val stored = runCatching {
            if (file.exists()) json.decodeFromString<DiarySnapshot>(file.readText()) else null
        }.getOrNull()

        val seeded = when (stored) {
            null -> DiarySnapshot(
                ingredients = SeedIngredients.all,
                recipes = StarterRecipes.all,
                categories = SeedCategories.all,
                seedVersion = SeedIngredients.VERSION,
                categorySeedVersion = SeedCategories.VERSION,
            )

            // A newer build with more in the bundled library merges the additions
            // in without touching anything you've edited or retired.
            else -> migrate(stored).let { current ->
                var merged = current
                if (merged.seedVersion < SeedIngredients.VERSION) {
                    merged = merged.copy(
                        ingredients = SeedIngredients.mergeInto(merged.ingredients, merged.retiredIngredientIds),
                        seedVersion = SeedIngredients.VERSION,
                    )
                }
                if (merged.categorySeedVersion < SeedCategories.VERSION) {
                    // Same rule as ingredients: add what's new, leave alone
                    // anything already there so an edited category survives.
                    val known = merged.categories.map { it.id }.toSet()
                    merged = merged.copy(
                        categories = merged.categories + SeedCategories.all.filterNot { it.id in known },
                        categorySeedVersion = SeedCategories.VERSION,
                    )
                }
                merged
            }
        }

        _snapshot.value = seeded
        if (stored != seeded) persist(seeded)
    }

    /**
     * Brings an older file up to [DiarySnapshot.CURRENT_SCHEMA].
     *
     * Written to run in order and to be a no-op once it has, so it stays safe to
     * call on every load. The log is evidence — a migration that mangles it is
     * worse than one that never ships.
     */
    private fun migrate(stored: DiarySnapshot): DiarySnapshot {
        if (stored.schemaVersion >= DiarySnapshot.CURRENT_SCHEMA) return stored

        // 1 → 2: fold the single basis into the list and clear the old field.
        val meals = stored.meals.map { meal ->
            val legacy = meal.legacyBasis
            if (legacy != null && meal.bases.isEmpty()) {
                meal.copy(bases = listOf(legacy), legacyBasis = null)
            } else {
                meal.copy(legacyBasis = null)
            }
        }

        // 2 → 3: drop the bundled components that categories replaced. Anything
        // you edited or made yourself is left alone, and logged meals are
        // untouched either way — they carry their own copy of what was eaten.
        val recipes = stored.recipes.filterNot {
            it.id in DiarySnapshot.RETIRED_STARTERS && !it.isUserCreated
        }

        // 3 → 4: follow renamed ingredients everywhere an id is held.
        val renames = DiarySnapshot.RENAMED_INGREDIENTS
        fun rename(id: String) = renames[id] ?: id

        // 4 → 5: fold a category's several item lists into one. Options are
        // concatenated in the order they appeared and de-duplicated, so anything
        // added by hand survives the merge — losing those would be losing work.
        val categories = stored.categories.map { category ->
            val items = category.facets.filter { it.kind == FacetKind.ITEMS }
            if (items.size <= 1 && items.all { it.name == SeedCategories.INGREDIENTS }) return@map category

            val merged = Facet(
                name = SeedCategories.INGREDIENTS,
                kind = FacetKind.ITEMS,
                options = items.flatMap { it.options }.distinctBy { it.key },
            )
            category.copy(
                facets = category.facets.filter { it.kind != FacetKind.ITEMS } +
                    listOfNotNull(merged.takeIf { it.options.isNotEmpty() }),
            )
        }.map { category ->
            // 5 → 6: a style facet named after its own category asks you
            // "Coffee › Coffee › Black", which is not a question anyone means.
            category.copy(
                facets = category.facets.map { facet ->
                    if (facet.kind == FacetKind.STYLE && facet.name.equals(category.name, ignoreCase = true)) {
                        facet.copy(name = SeedCategories.TYPE)
                    } else {
                        facet
                    }
                },
            )
        }.map { category ->
            // 6 → 7, part one: coffee's style question goes, and whatever it
            // defaulted to becomes an ordinary ingredient. Dropping the facet
            // without doing that would leave a Coffee category that can't add
            // any coffee.
            if (category.id != "cat-coffee") return@map category
            val style = category.facets.firstOrNull { it.kind == FacetKind.STYLE } ?: return@map category
            val coffee = FacetOption("Coffee", ingredientId = style.defaultIngredientId)

            category.copy(
                facets = category.facets.filter { it.kind != FacetKind.STYLE }.map { facet ->
                    if (facet.kind == FacetKind.ITEMS && facet.options.none { it.key == coffee.key }) {
                        facet.copy(options = listOf(coffee) + facet.options)
                    } else {
                        facet
                    }
                },
            )
        }.map { category ->
            // 6 → 7, part two. Only out of the pick lists — the ingredient stays
            // in the library, because a café coffee still comes with dairy milk
            // and that needs to be loggable.
            category.copy(
                facets = category.facets.map { facet ->
                    facet.copy(
                        options = facet.options.filterNot {
                            it.ingredientId in DiarySnapshot.DROPPED_CATEGORY_OPTIONS
                        },
                    )
                },
            )
        }.map { category ->
            // 7 → 8: the coffee in Coffee stops being a tick. An option you
            // always choose isn't a choice, so it moves to what the category
            // simply includes.
            if (category.id != "cat-coffee" || category.always.isNotEmpty()) return@map category

            val coffee = category.facets
                .flatMap { it.options }
                .firstOrNull { it.label.equals("Coffee", ignoreCase = true) && it.ingredientId != null }
                ?: return@map category

            category.copy(
                always = listOf(MealItem(ingredientId = coffee.ingredientId!!)),
                facets = category.facets.map { facet ->
                    facet.copy(options = facet.options.filterNot { it.key == coffee.key })
                },
            )
        }.map { category ->
            // 11 -> 12: a bundled facet takes the seed's role subscriptions.
            // Options are left exactly as they are — the shortlist is yours,
            // and this only says what else is eligible behind "more".
            if (category.isUserCreated) return@map category
            val seeded = SeedCategories.all.firstOrNull { it.id == category.id } ?: return@map category
            category.copy(
                facets = category.facets.map { facet ->
                    val from = seeded.facets.firstOrNull { it.name == facet.name } ?: return@map facet
                    if (from.roles.all { it in facet.roles }) facet else facet.copy(roles = facet.roles + from.roles)
                },
            )
        }

        // 14 -> 15: colour and lettering come apart. Until now one setting
        // chose both, so a diary that predates the split has no faces recorded
        // and the defaults would silently move somebody off Petrock and onto
        // Fraunces the first time they opened the app. Reading the old pairing
        // back out of the theme they were on keeps them exactly where they
        // were, and from here the two move independently.
        //
        // This is why the schema version exists rather than a nullable field:
        // "no face recorded" and "chose Fraunces" are the same bytes on disk,
        // and only the version tells them apart.
        val settings = if (stored.schemaVersion < 15) {
            val (display, body) = TypeFace.pairFor(stored.settings.colours)
            stored.settings.copy(displayFace = display, bodyFace = body)
        } else {
            stored.settings
        }

        return stored.copy(
            schemaVersion = DiarySnapshot.CURRENT_SCHEMA,
            settings = settings,
            categories = categories,
            meals = meals.map { meal ->
                meal.copy(
                    items = meal.items.map { it.copy(ingredientId = rename(it.ingredientId)) },
                    bases = meal.bases.map { basis ->
                        basis.copy(
                            itemsAtLogTime = basis.itemsAtLogTime.map {
                                it.copy(ingredientId = rename(it.ingredientId))
                            },
                        )
                    },
                )
            },
            recipes = recipes.map { recipe ->
                // 13 -> 14: the single nested folder becomes flat tags. Split on
                // the slash so "Cafe/Kettle Black" keeps both halves and
                // browsing either one still finds it.
                val fromGroup = recipe.legacyGroup
                    ?.split('/')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                recipe.copy(
                    items = recipe.items.map { it.copy(ingredientId = rename(it.ingredientId)) },
                    suggests = recipe.suggests.map(::rename),
                    tags = (recipe.tags + fromGroup).distinctBy { it.lowercase() }.toSet(),
                    legacyGroup = null,
                )
            },
            // A bundled ingredient gets the seed's new name as well as its new
            // id — moving the id alone would leave "Vegemite" still displayed as
            // "Yeast extract spread" forever. One you've edited keeps your name;
            // a rename in the seed shouldn't overwrite your own wording.
            ingredients = stored.ingredients.map { ingredient ->
                if (ingredient.id !in renames) return@map ingredient
                val newId = rename(ingredient.id)
                val seeded = SeedIngredients.all.firstOrNull { it.id == newId }
                if (seeded != null && !ingredient.isUserCreated) seeded else ingredient.copy(id = newId)
            }.map { ingredient ->
                // 10 -> 11: a bundled ingredient picks up any attribute the seed
                // has since gained, so adding one doesn't need its own migration
                // and doesn't quietly apply to new installs only. Union rather
                // than replace, and only where you've never edited the entry:
                // editing marks it yours, which is also how deliberately taking
                // an attribute off stays taken off.
                if (ingredient.isUserCreated) return@map ingredient
                val seeded = SeedIngredients.all.firstOrNull { it.id == ingredient.id } ?: return@map ingredient
                // 11 -> 12 rides along: roles arrived the same way and are
                // adopted on the same terms.
                // Aliases ride along too. Without them a search word added to
                // the seed later — "nooch" for nutritional yeast — would only
                // ever work on a fresh install.
                if (seeded.attributes.all { it in ingredient.attributes } &&
                    seeded.roles.all { it in ingredient.roles } &&
                    seeded.aliases.all { it in ingredient.aliases }
                ) {
                    return@map ingredient
                }
                ingredient.copy(
                    attributes = ingredient.attributes + seeded.attributes,
                    roles = ingredient.roles + seeded.roles,
                    aliases = (ingredient.aliases + seeded.aliases).distinct(),
                )
            }.map { ingredient ->
                // 15 -> 16: a single grams-per-spoon figure becomes a density.
                //
                // Every ingredient measured by volume already carried what one
                // of its own unit weighed -- 5 g per tablespoon of nooch. That
                // is really 0.33 g/ml, and a density answers teaspoons and cups
                // as well, including units nobody has added yet. Recovering it
                // is pure arithmetic over a figure already present, so nothing
                // is invented and no existing entry changes meaning.
                //
                // Runs for edited ingredients too, unlike the adoptions above:
                // this is not the seed pushing new opinions onto your data, it
                // is the same number written in a more useful form.
                if (ingredient.densityGPerMl != null) return@map ingredient
                val density = Measures.impliedDensity(ingredient) ?: return@map ingredient
                ingredient.copy(densityGPerMl = density)
            },
            retiredIngredientIds = stored.retiredIngredientIds.map(::rename).toSet(),
        )
    }

    private suspend fun persist(snapshot: DiarySnapshot) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            // Written beside the real file and moved into place, so a crash
            // half-way through leaves the previous good copy rather than a
            // truncated one. Losing today's lunch is bad; losing the diary is
            // the whole point of the app gone.
            val scratch = File(file.parentFile, file.name + ".tmp")
            scratch.parentFile?.mkdirs()
            scratch.writeText(json.encodeToString(snapshot))
            if (!scratch.renameTo(file)) {
                file.writeText(scratch.readText())
                scratch.delete()
            }
        }
    }

    private suspend fun mutate(block: (DiarySnapshot) -> DiarySnapshot) {
        val updated = block(_snapshot.value)
        _snapshot.value = updated
        persist(updated)
    }

    // ---- Meals --------------------------------------------------------------

    suspend fun upsertMeal(meal: MealEntry) = mutate { state ->
        state.copy(meals = state.meals.upsert(meal) { it.id == meal.id })
    }

    suspend fun deleteMeal(id: String) = mutate { state ->
        state.copy(meals = state.meals.filterNot { it.id == id })
    }

    /**
     * Records a meal and credits the recipe it came from.
     *
     * The usage counters are what make the picker useful, and they have to move
     * with the log rather than with an explicit "I use this a lot" toggle
     * nobody would ever set.
     */
    suspend fun logMeal(meal: MealEntry, now: LocalDateTime) = mutate { state ->
        // Every part used gets the credit, so toast and its topping both climb
        // the picker rather than only whichever was tapped first.
        val used = meal.bases.map { it.recipeId }.toSet()
        state.copy(
            meals = state.meals.upsert(meal) { it.id == meal.id },
            recipes = if (used.isEmpty()) {
                state.recipes
            } else {
                state.recipes.map {
                    if (it.id in used) it.copy(timesUsed = it.timesUsed + 1, lastUsedAt = now) else it
                }
            },
        )
    }

    // ---- Symptoms, stools, meds --------------------------------------------

    suspend fun upsertSymptom(entry: SymptomEntry) = mutate { state ->
        state.copy(symptoms = state.symptoms.upsert(entry) { it.id == entry.id })
    }

    suspend fun deleteSymptom(id: String) = mutate { state ->
        state.copy(symptoms = state.symptoms.filterNot { it.id == id })
    }

    suspend fun upsertStool(entry: StoolEntry) = mutate { state ->
        state.copy(stools = state.stools.upsert(entry) { it.id == entry.id })
    }

    suspend fun deleteStool(id: String) = mutate { state ->
        state.copy(stools = state.stools.filterNot { it.id == id })
    }

    suspend fun upsertMed(entry: MedEntry) = mutate { state ->
        state.copy(meds = state.meds.upsert(entry) { it.id == entry.id })
    }

    suspend fun deleteMed(id: String) = mutate { state ->
        state.copy(meds = state.meds.filterNot { it.id == id })
    }

    // ---- Day log ------------------------------------------------------------

    suspend fun upsertDayLog(day: DayLog) = mutate { state ->
        // An emptied-out day is removed rather than stored blank, so "nothing
        // recorded" and "recorded as nothing" stay distinguishable — the
        // analysis has to be able to tell a missing answer from a zero.
        val without = state.days.filterNot { it.date == day.date }
        state.copy(days = if (day.isEmpty) without else without + day)
    }

    // ---- Recipes ------------------------------------------------------------

    suspend fun upsertRecipe(recipe: Recipe) = mutate { state ->
        state.copy(recipes = state.recipes.upsert(recipe) { it.id == recipe.id })
    }

    suspend fun deleteRecipe(id: String) = mutate { state ->
        // Logged meals keep their own copy of the items and their basis
        // snapshot, so deleting a recipe can never damage the log. That is the
        // whole reason entries are stored resolved.
        state.copy(recipes = state.recipes.filterNot { it.id == id })
    }

    // ---- Categories ---------------------------------------------------------

    suspend fun upsertCategory(category: Category) = mutate { state ->
        state.copy(categories = state.categories.upsert(category) { it.id == category.id })
    }

    suspend fun deleteCategory(id: String) = mutate { state ->
        state.copy(categories = state.categories.filterNot { it.id == id })
    }

    // ---- Ingredients --------------------------------------------------------

    suspend fun upsertIngredient(ingredient: Ingredient) = mutate { state ->
        state.copy(ingredients = state.ingredients.upsert(ingredient) { it.id == ingredient.id })
    }

    /**
     * Removes an ingredient from the library.
     *
     * Past meals referencing it are left alone: they hold ids, and an entry
     * mentioning something no longer in the library is still a true record of
     * what was eaten. The UI shows the id rather than dropping the item.
     */
    suspend fun deleteIngredient(id: String) = mutate { state ->
        state.copy(
            ingredients = state.ingredients.filterNot { it.id == id },
            retiredIngredientIds = state.retiredIngredientIds + id,
        )
    }

    fun ingredientUsage(id: String): IngredientUsage {
        val state = _snapshot.value
        return IngredientUsage(
            mealCount = state.meals.count { meal -> meal.items.any { it.ingredientId == id } },
            recipeCount = state.recipes.count { recipe -> recipe.items.any { it.ingredientId == id } },
        )
    }

    // ---- Export / import ----------------------------------------------------

    fun exportJson(): String = json.encodeToString(_snapshot.value)

    /**
     * Replaces everything from an export, or returns false if [raw] isn't one.
     *
     * Refusing bad input matters more than the happy path: this overwrites the
     * entire diary, and pasting the wrong thing should be a message rather than
     * an empty app.
     */
    suspend fun setThemeMode(mode: ThemeMode) = mutate { state ->
        state.copy(settings = state.settings.copy(themeMode = mode))
    }

    suspend fun setColours(theme: ColourTheme) = mutate { state ->
        state.copy(settings = state.settings.copy(colours = theme))
    }

    suspend fun setDisplayFace(face: TypeFace) = mutate { state ->
        state.copy(settings = state.settings.copy(displayFace = face))
    }

    suspend fun setBodyFace(face: TypeFace) = mutate { state ->
        state.copy(settings = state.settings.copy(bodyFace = face))
    }

    suspend fun setCustomPalette(palette: CustomPalette) = mutate { state ->
        // Choosing a swatch selects Custom as well. Editing colours you cannot
        // see is a puzzle, and there is no other reason to be on this screen.
        state.copy(
            settings = state.settings.copy(
                custom = palette,
                colours = ColourTheme.CUSTOM,
            ),
        )
    }

    suspend fun setTextSize(size: TextSize) = mutate { state ->
        state.copy(settings = state.settings.copy(textSize = size))
    }

    suspend fun setEnergyUnit(unit: EnergyUnit) = mutate { state ->
        state.copy(settings = state.settings.copy(energyUnit = unit))
    }

    suspend fun importJson(raw: String): Boolean {
        val parsed = runCatching { json.decodeFromString<DiarySnapshot>(raw) }.getOrNull()
            ?: return false
        if (parsed.ingredients.isEmpty()) return false
        mutate { parsed }
        return true
    }
}

data class IngredientUsage(val mealCount: Int, val recipeCount: Int) {
    val isUsed: Boolean get() = mealCount > 0 || recipeCount > 0
}

/** Replace in place if it's already there, otherwise append. */
private inline fun <T> List<T>.upsert(value: T, matching: (T) -> Boolean): List<T> {
    val index = indexOfFirst(matching)
    return if (index >= 0) toMutableList().apply { this[index] = value } else this + value
}
