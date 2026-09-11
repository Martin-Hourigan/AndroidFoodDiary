package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.TextSize
import dev.mahourigan.fooddiary.data.DiarySnapshot
import dev.mahourigan.fooddiary.data.Repositories
import dev.mahourigan.fooddiary.data.SeedIngredients
import dev.mahourigan.fooddiary.domain.Attribute
import dev.mahourigan.fooddiary.domain.Basis
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.DayEvent
import dev.mahourigan.fooddiary.domain.DayLog
import dev.mahourigan.fooddiary.domain.Energy
import dev.mahourigan.fooddiary.domain.EnergyUnit
import dev.mahourigan.fooddiary.ui.theme.ColourTheme
import dev.mahourigan.fooddiary.ui.theme.CustomPalette
import dev.mahourigan.fooddiary.ui.theme.TypeFace
import dev.mahourigan.fooddiary.ui.theme.ThemeMode
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.MealType
import dev.mahourigan.fooddiary.domain.Portion
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.Severity
import dev.mahourigan.fooddiary.domain.StoolEntry
import dev.mahourigan.fooddiary.domain.SymptomEntry
import dev.mahourigan.fooddiary.domain.SymptomType
import dev.mahourigan.fooddiary.domain.analysis.Suggestion
import dev.mahourigan.fooddiary.domain.analysis.Suggestions
import dev.mahourigan.fooddiary.domain.attributes
import dev.mahourigan.fooddiary.domain.closedAt
import dev.mahourigan.fooddiary.domain.dayTimeline
import dev.mahourigan.fooddiary.domain.amountIn
import dev.mahourigan.fooddiary.domain.energy
import dev.mahourigan.fooddiary.domain.kcalPerServing
import dev.mahourigan.fooddiary.domain.perServing
import dev.mahourigan.fooddiary.domain.resolve
import dev.mahourigan.fooddiary.domain.openEpisodes
import dev.mahourigan.fooddiary.domain.withSeverityChange
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Whether a meal also becomes something saved.
 *
 * Asked after the fact rather than up front: you are logging dinner, not
 * maintaining a database, and the library should grow out of what you actually
 * ate. [JUST_ONCE] is the default because most nights it's the truth.
 *
 * There used to be a third answer — update the recipe you started from — and it
 * is gone deliberately. Editing a recipe belongs in Recipes, not behind
 * a radio button you meet while recording your dinner, where one wrong tap
 * rewrote the recipe.
 */
enum class SaveBack { JUST_ONCE, NEW_RECIPE }

/** What, if anything, the save sheet asks about the saved library. */
enum class SaveCard { NONE, KEEP_AS_MEAL, SAVE_BACK }

/**
 * Where you are in logging a meal.
 *
 * Choosing breakfast is a step, not a setting on a form: it decides what you're
 * shown next, and once it's answered it stops taking up room at the top of every
 * screen.
 */
enum class MealStage { CHOOSING_TYPE, PICKING }

/** How the time was arrived at. */
enum class TimeChoice { NOW, EXACT, VAGUE }

data class MealDraft(
    val editingId: String? = null,
    val bases: List<Basis> = emptyList(),
    val items: List<MealItem> = emptyList(),
    val at: LocalDateTime = LocalDateTime.now(),
    val mealType: MealType? = null,
    val timeChoice: TimeChoice = TimeChoice.NOW,
    val notes: String = "",
    val saveBack: SaveBack = SaveBack.JUST_ONCE,
    val newRecipeName: String = "",

    /** Which tags a newly saved meal carries. */
    val newRecipeTags: Set<String> = emptySet(),

    /** Typed calories for this meal, kept as text while the field is open. */
    val kcal: String = "",

    val stage: MealStage = MealStage.CHOOSING_TYPE,

    /** Which folder the picker is showing: [] at the top, ["Cafe"] inside one. */
    val path: List<String> = emptyList(),


    /** The category being answered, if any. */
    val categoryId: String? = null,

    /**
     * Inside a category: your saved ones, or the questions.
     *
     * Null means "not decided yet", which is what lets the sheet open on the
     * usuals when there are any and go straight to the questions when there
     * aren't — rather than showing a toggle over an empty list.
     */
    val buildingCategory: Boolean? = null,

    /**
     * Facet name to chosen option keys, for the open category.
     *
     * Kept apart from [items] while the category is open so that unticking
     * "sourdough" genuinely takes it back off, rather than leaving a stray
     * ingredient behind that you'd have to hunt for.
     */
    val facetChoices: Map<String, Set<String>> = emptyMap(),
) {
    val isEditingExisting: Boolean get() = editingId != null

    fun contains(ingredientId: String) = items.any { it.ingredientId == ingredientId }

    val baseIds: Set<String> get() = bases.mapTo(mutableSetOf()) { it.recipeId }

    val itemIds: Set<String> get() = items.mapTo(mutableSetOf()) { it.ingredientId }

    /** What the recipe(s) said, for the +/− display while you're still editing. */
    val expectedItems: List<MealItem>
        get() = bases.flatMap { it.itemsAtLogTime }.distinctBy { it.ingredientId }

    val isCustomised: Boolean
        get() = bases.isNotEmpty() && items.map { it.ingredientId to it.portion } !=
            expectedItems.map { it.ingredientId to it.portion }

    /**
     * Which question the save sheet should ask about the library, if any.
     *
     * Three cases, and the middle one is easy to lose. Built from ingredients:
     * one question, keep it or don't. Started from something saved *and
     * changed*: three answers, because updating the original is now on the
     * table. Started from something saved and left alone: **no question at
     * all** — asking what to do about a change nobody made is the app
     * inventing work.
     */
    val saveCard: SaveCard
        get() = when {
            bases.isEmpty() -> if (items.isEmpty()) SaveCard.NONE else SaveCard.KEEP_AS_MEAL
            isCustomised || bases.size > 1 -> SaveCard.SAVE_BACK
            else -> SaveCard.NONE
        }

    val title: String get() = bases.joinToString(" + ") { it.name }
}

data class UiState(
    val snapshot: DiarySnapshot = DiarySnapshot(),
    val date: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
    val loading: Boolean = true,
) {
    val byId: Map<String, Ingredient> get() = snapshot.ingredientsById

    val isToday: Boolean get() = date == now.toLocalDate()

    val timeline: List<DayEvent>
        get() = dayTimeline(
            meals = snapshot.meals,
            symptoms = snapshot.symptoms,
            stools = snapshot.stools,
            meds = snapshot.meds,
            date = date,
            now = now,
        )

    val open: List<SymptomEntry> get() = openEpisodes(snapshot.symptoms)

    val dayLog: DayLog get() = snapshot.dayLog(date)

    /** Every tag in use, so a second spelling of "Cafe" is one tap away from not happening. */
    fun knownTags(): List<String> =
        (snapshot.recipes.flatMap { it.tags } + snapshot.ingredients.flatMap { it.tags })
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

    fun ingredient(id: String): Ingredient? = byId[id]

    /** Falls back to the raw id, so an entry mentioning a deleted ingredient is still readable. */
    fun nameOf(id: String): String = byId[id]?.name ?: id

    /** Whichever unit energy is being shown in. Storage is always kcal. */
    val energyUnit: EnergyUnit get() = snapshot.settings.energyUnit

    /** Calories for a meal, using any figure set on the recipes it was built from. */
    fun energyOf(meal: MealEntry): Energy =
        meal.energy(byId) { id -> snapshot.recipesById[id]?.kcalPerServing }

    /** Calories for a list of items being assembled, before it is a meal. */
    fun energyOf(items: List<MealItem>): Energy =
        MealEntry(items = items).energy(byId) { null }

    /** Includes the per-meal modifiers, so ticking sourdough shows up here. */
    fun attributesOf(items: List<MealItem>): Set<Attribute> =
        items.flatMapTo(mutableSetOf()) { byId[it.ingredientId]?.attributes.orEmpty() + it.modifiers }

    fun category(id: String?): Category? = id?.let { key -> snapshot.categories.firstOrNull { it.id == key } }

    /** The top of the picker: categories and tag chips, ranked by what you eat. */
    fun categories(draft: MealDraft): List<Suggestion> = Suggestions.categories(
        categories = snapshot.categories,
        recipes = snapshot.recipes,
        ingredients = snapshot.ingredients,
        meals = snapshot.meals,
        mealType = draft.mealType,
    )


    /**
     * What to offer next inside a folder, given what's already in the meal.
     *
     * Recomputed as the draft changes, which is what turns a folder into a list
     * led by what you actually put with the thing you just picked.
     */
    fun suggestions(draft: MealDraft): List<Suggestion> = Suggestions.forPicker(
        recipes = snapshot.recipes,
        ingredients = snapshot.ingredients,
        meals = snapshot.meals,
        mealType = draft.mealType,
        currentItems = draft.itemIds,
        currentPath = draft.path,
    )

    /** Saved things tagged with this category's name — "the usual". */
    fun usuals(category: Category, draft: MealDraft): List<Recipe> =
        Suggestions.usualsFor(category, snapshot.recipes, snapshot.meals, draft.mealType)

    fun carriersOf(attribute: Attribute, items: List<MealItem>): List<Ingredient> =
        items.mapNotNull { byId[it.ingredientId] }.filter { attribute in it.attributes }.distinctBy { it.id }
}

class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = Repositories.diary(app)

    private val viewedDate = MutableStateFlow(LocalDate.now())
    private val now = MutableStateFlow(LocalDateTime.now())
    private val loading = MutableStateFlow(true)

    private val _draft = MutableStateFlow<MealDraft?>(null)
    val draft: StateFlow<MealDraft?> = _draft.asStateFlow()

    val state: StateFlow<UiState> = combine(
        repository.snapshot,
        viewedDate,
        now,
        loading,
    ) { snapshot, date, clock, isLoading ->
        UiState(snapshot = snapshot, date = date, now = clock, loading = isLoading)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            repository.load()
            loading.value = false
        }

        // Open episodes show a running duration, and the day rolls over at
        // midnight whether the app is looking or not. A minute is fine: nothing
        // here is displayed to the second.
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                now.value = LocalDateTime.now()
            }
        }
    }

    fun showDate(date: LocalDate) {
        viewedDate.value = date
    }

    fun refreshClock() {
        now.value = LocalDateTime.now()
    }

    // ---- Building a meal ----------------------------------------------------

    /** Opens the flow at its first step: which meal is this. */
    fun startMeal() {
        val at = LocalDateTime.now().onViewedDate()
        _draft.value = MealDraft(
            at = at,
            // A suggestion, not a decision — the clock is usually right and the
            // chip is pre-highlighted, but it still takes a tap to confirm.
            mealType = MealType.fromTime(at.toLocalTime()),
            stage = MealStage.CHOOSING_TYPE,
        )
    }

    /** Answers "which meal is this" and moves on to what's in it. */
    fun chooseMealType(type: MealType) = updateDraft {
        it.copy(mealType = type, stage = MealStage.PICKING, path = emptyList())
    }

    /** Back to the meal-type step, keeping whatever is already in the meal. */
    fun backToMealType() = updateDraft { it.copy(stage = MealStage.CHOOSING_TYPE) }

    fun setTimeNow() = updateDraft {
        it.copy(at = LocalDateTime.now().onViewedDate(), timeChoice = TimeChoice.NOW)
    }

    fun setTimeExact(at: LocalDateTime) = updateDraft {
        it.copy(at = at, timeChoice = TimeChoice.EXACT)
    }

    /**
     * "Sometime that morning" — no clock was consulted.
     *
     * Stored as the meal type's usual hour and flagged, rather than left blank:
     * a meal with no time at all can't be lined up against a symptom, and every
     * lag window is measured from it.
     */
    fun setTimeVague() = updateDraft { draft ->
        val hour = (draft.mealType ?: MealType.SNACK).typicalHour
        draft.copy(
            at = draft.at.toLocalDate().atTime(hour, 0),
            timeChoice = TimeChoice.VAGUE,
        )
    }

    /**
     * Adds a saved thing to the meal being built.
     *
     * The snapshot is taken here, at the moment of logging, and never refreshed.
     * That's what keeps the `+ avocado / − red onion` display honest after the
     * recipe is later edited.
     *
     * Adding rather than replacing: tapping Toast then Avocado smash should give
     * you both, which is the whole point of assembling a meal from parts. Adding
     * one twice is a mis-tap, so it's ignored.
     */
    fun addRecipe(recipe: Recipe) = updateDraft { draft ->
        if (recipe.id in draft.baseIds) return@updateDraft draft

        // One serving, not the pot. Both the items and the snapshot use the
        // same divided list, or the draft would read as customised the moment
        // it was added.
        val serving = recipe.perServing()
        val newItems = serving.filterNot { it.ingredientId in draft.itemIds }
        // Stays in the folder. You picked the bread; the butter and the Vegemite
        // are the next two taps and they're in here with it. Going back out to
        // the categories after every single tap would make assembling a meal
        // feel like fighting the app.
        draft.copy(
            bases = draft.bases + Basis(recipeId = recipe.id, name = recipe.name, itemsAtLogTime = serving),
            items = draft.items + newItems,
        )
    }

    /** Removes a part, and with it any ingredient nothing else brought. */
    fun removeBase(recipeId: String) = updateDraft { draft ->
        val going = draft.bases.firstOrNull { it.recipeId == recipeId } ?: return@updateDraft draft
        val remaining = draft.bases.filterNot { it.recipeId == recipeId }

        // Only drop an ingredient if it came from this part and isn't in another,
        // and wasn't added by hand — removing "toast" must not take away the
        // avocado you put on it.
        val keptElsewhere = remaining.flatMap { it.itemsAtLogTime }.map { it.ingredientId }.toSet()
        val addedByHand = draft.items.map { it.ingredientId }.toSet() -
            draft.bases.flatMap { it.itemsAtLogTime }.map { it.ingredientId }.toSet()

        val dropping = going.itemsAtLogTime.map { it.ingredientId }.toSet() - keptElsewhere - addedByHand

        draft.copy(bases = remaining, items = draft.items.filterNot { it.ingredientId in dropping })
    }

    fun browse(path: List<String>) = updateDraft { it.copy(path = path) }


    fun openCategory(category: Category) = updateDraft {
        it.copy(categoryId = category.id, facetChoices = emptyMap())
    }

    /** Ticks or unticks one option in a facet. */
    fun toggleFacetOption(facet: Facet, option: FacetOption) = updateDraft { draft ->
        val current = draft.facetChoices[facet.name].orEmpty()
        val next = if (option.key in current) current - option.key else current + option.key
        draft.copy(facetChoices = draft.facetChoices + (facet.name to next))
    }

    /**
     * Folds the open category's answers into the meal and closes it.
     *
     * Anything the category contributed that's already in the meal is skipped
     * rather than duplicated — butter on toast twice is one butter.
     */
    fun commitCategory() = updateDraft { draft ->
        val category = repository.snapshot.value.categories.firstOrNull { it.id == draft.categoryId }
            ?: return@updateDraft draft.copy(
                categoryId = null,
                facetChoices = emptyMap(),
                buildingCategory = null,
            )

        val resolved = category.resolve(draft.facetChoices, repository.snapshot.value.ingredientsById)
        val existing = draft.itemIds
        draft.copy(
            items = draft.items + resolved.filterNot { it.ingredientId in existing },
            categoryId = null,
            facetChoices = emptyMap(),
            buildingCategory = null,
            // Offer the category as a tag when this gets saved: a toast you
            // built and keep should turn up under Toast next time, and that
            // only happens if it carries the tag.
            newRecipeTags = draft.newRecipeTags + category.name,
        )
    }

    fun cancelCategory() =
        updateDraft { it.copy(categoryId = null, facetChoices = emptyMap(), buildingCategory = null) }

    fun showCategoryBuild(building: Boolean) = updateDraft { it.copy(buildingCategory = building) }

    /**
     * Adds an option to a facet and ticks it.
     *
     * Kept, not just used once: you added it because you eat it, so it should be
     * a chip next time rather than something to search for again. Ticking it
     * immediately matters too — you opened this to record a meal, not to
     * maintain a list.
     */
    fun addFacetOption(category: Category, facet: Facet, option: FacetOption) {
        val already = category.facets
            .firstOrNull { it.name == facet.name }
            ?.options.orEmpty()
            .any { it.key == option.key }

        if (!already) {
            val updated = category.copy(
                facets = category.facets.map {
                    if (it.name == facet.name) it.copy(options = it.options + option) else it
                },
            )
            viewModelScope.launch { repository.upsertCategory(updated) }
        }

        updateDraft { draft ->
            val current = draft.facetChoices[facet.name].orEmpty()
            draft.copy(facetChoices = draft.facetChoices + (facet.name to (current + option.key)))
        }
    }

    /**
     * Makes an ingredient that isn't in the library yet, then offers it.
     *
     * It arrives with no attributes, which is worth being plain about: an
     * untagged ingredient is invisible to the analysis. Better than refusing to
     * let you log what you ate, though — the entry is true either way, and the
     * tagging can be caught up later in the library.
     */
    fun createIngredient(name: String): Ingredient {
        val trimmed = name.trim()
        val existing = repository.snapshot.value.ingredients
            .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing

        val ingredient = Ingredient(
            id = SeedIngredients.slug(trimmed).ifBlank { UUID.randomUUID().toString() },
            name = trimmed,
            isUserCreated = true,
        )
        viewModelScope.launch { repository.upsertIngredient(ingredient) }
        return ingredient
    }

    /** Reopens a logged meal for editing, exactly as stored. */
    fun editMeal(meal: MealEntry) {
        _draft.value = MealDraft(
            editingId = meal.id,
            bases = meal.bases,
            items = meal.items,
            at = meal.at,
            mealType = meal.mealType,
            timeChoice = if (meal.timeApproximate) TimeChoice.VAGUE else TimeChoice.EXACT,
            // The draft holds what is on screen, so it is in whichever unit is
            // on show; storage stays kcal at both ends.
            kcal = meal.kcalOverride
                ?.let { Math.round(energyUnit.from(it.toDouble())).toString() }
                .orEmpty(),
            notes = meal.notes,
            // Straight to the contents: editing an existing meal means changing
            // what was in it, not being asked again which meal it was.
            stage = MealStage.PICKING,
        )
    }

    fun cancelDraft() {
        _draft.value = null
    }

    fun updateDraft(block: (MealDraft) -> MealDraft) {
        _draft.value = _draft.value?.let(block)
    }

    fun addToDraft(ingredient: Ingredient) = updateDraft { draft ->
        if (draft.contains(ingredient.id)) draft
        else draft.copy(items = draft.items + MealItem(ingredientId = ingredient.id))
    }

    fun removeFromDraft(ingredientId: String) = updateDraft { draft ->
        draft.copy(items = draft.items.filterNot { it.ingredientId == ingredientId })
    }

    fun setPortion(ingredientId: String, portion: Portion?) = updateDraft { draft ->
        draft.copy(
            items = draft.items.map {
                if (it.ingredientId == ingredientId) it.copy(portion = portion?.takeUnless { p -> p.isEmpty }) else it
            },
        )
    }

    /**
     * Commits the draft, and handles what to do about the recipe.
     *
     * The entry is written with its items fully resolved regardless of the
     * save-back choice — updating a recipe and recording a meal are two separate
     * things, and conflating them is how a log stops being evidence.
     */
    fun saveDraft() {
        // Saving straight from an open category shouldn't quietly drop what you
        // just ticked.
        if (_draft.value?.categoryId != null) commitCategory()

        val draft = _draft.value ?: return
        if (draft.items.isEmpty()) return

        val meal = MealEntry(
            id = draft.editingId ?: UUID.randomUUID().toString(),
            at = draft.at,
            mealType = draft.mealType ?: MealType.fromTime(draft.at.toLocalTime()),
            bases = draft.bases,
            items = draft.items,
            timeApproximate = draft.timeChoice == TimeChoice.VAGUE,
            // The box arrives pre-filled with the worked-out figure, so a typed
            // number only counts as *yours* if it differs from that. Otherwise
            // every meal would be stored as a stated total: the honest "~"
            // would vanish, and correcting an ingredient's energy later would
            // no longer reach the meals that used it.
            kcalOverride = draft.kcal.trim().toIntOrNull()
                ?.takeIf { typed -> typed.toLong() != computedEnergy(draft).amountIn(energyUnit) }
                ?.let { Math.round(energyUnit.toKcal(it.toDouble())).toInt() },
            notes = draft.notes,
        )

        viewModelScope.launch {
            if (draft.isEditingExisting) {
                repository.upsertMeal(meal)
            } else {
                repository.logMeal(meal, LocalDateTime.now())
            }

            when (draft.saveBack) {
                SaveBack.JUST_ONCE -> Unit

                SaveBack.NEW_RECIPE -> {
                    val name = draft.newRecipeName.trim().ifBlank { suggestedName(draft) }
                    if (name.isNotBlank()) {
                        // Tags you picked win. Failing that a combination
                        // inherits the tags of the part it was built on, so it
                        // lands where you'd look for it rather than untagged.
                        val inherited = draft.bases.firstOrNull()?.recipeId
                            ?.let { repository.snapshot.value.recipesById[it]?.tags }
                            .orEmpty()
                        repository.upsertRecipe(
                            Recipe(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                items = draft.items,
                                tags = draft.newRecipeTags.ifEmpty { inherited },
                                lastUsedAt = LocalDateTime.now(),
                                timesUsed = 1,
                            ),
                        )
                    }
                }
            }
            _draft.value = null
        }
    }

    fun deleteMeal(meal: MealEntry) = viewModelScope.launch { repository.deleteMeal(meal.id) }

    // ---- Symptoms -----------------------------------------------------------

    /** "I'm bloated — start timing." Two taps and the clock is running. */
    fun startSymptom(type: SymptomType, severity: Severity, notes: String = "") {
        val at = LocalDateTime.now()
        viewModelScope.launch {
            repository.upsertSymptom(
                SymptomEntry.startNow(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    severity = severity,
                    now = at,
                    notes = notes,
                ),
            )
        }
    }

    /** "It started about half four and lasted a couple of hours." */
    fun recordSymptom(
        type: SymptomType,
        severity: Severity,
        startedAt: LocalDateTime,
        endedAt: LocalDateTime?,
        notes: String = "",
    ) {
        viewModelScope.launch {
            repository.upsertSymptom(
                SymptomEntry(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    startedAt = startedAt,
                    endedAt = endedAt?.let { maxOf(it, startedAt) },
                    severityPoints = listOf(dev.mahourigan.fooddiary.domain.SeverityPoint(startedAt, severity)),
                    notes = notes,
                ),
            )
        }
    }

    fun updateSymptom(entry: SymptomEntry) = viewModelScope.launch { repository.upsertSymptom(entry) }

    fun stopSymptom(entry: SymptomEntry, at: LocalDateTime = LocalDateTime.now()) =
        viewModelScope.launch { repository.upsertSymptom(entry.closedAt(at)) }

    fun changeSeverity(entry: SymptomEntry, severity: Severity) = viewModelScope.launch {
        repository.upsertSymptom(entry.withSeverityChange(severity, LocalDateTime.now()))
    }

    /** "I forgot to close it" — kept as an episode, but its duration stops counting. */
    fun distrustDuration(entry: SymptomEntry) = viewModelScope.launch {
        repository.upsertSymptom(entry.copy(endedAt = entry.startedAt, durationTrusted = false))
    }

    fun deleteSymptom(entry: SymptomEntry) = viewModelScope.launch { repository.deleteSymptom(entry.id) }

    // ---- Stools -------------------------------------------------------------

    fun logStool(bristol: Int, urgency: Severity?, at: LocalDateTime = LocalDateTime.now(), notes: String = "") {
        viewModelScope.launch {
            repository.upsertStool(
                StoolEntry(
                    id = UUID.randomUUID().toString(),
                    at = at,
                    bristol = bristol,
                    urgency = urgency,
                    notes = notes,
                ),
            )
        }
    }

    fun deleteStool(entry: StoolEntry) = viewModelScope.launch { repository.deleteStool(entry.id) }

    // ---- Day log ------------------------------------------------------------

    fun saveDayLog(day: DayLog) = viewModelScope.launch { repository.upsertDayLog(day) }

    // ---- Library ------------------------------------------------------------

    fun saveCategory(category: Category) = viewModelScope.launch { repository.upsertCategory(category) }

    fun removeCategory(category: Category) = viewModelScope.launch { repository.deleteCategory(category.id) }

    fun saveRecipe(recipe: Recipe) = viewModelScope.launch { repository.upsertRecipe(recipe) }

    fun deleteRecipe(recipe: Recipe) = viewModelScope.launch { repository.deleteRecipe(recipe.id) }

    fun saveIngredient(ingredient: Ingredient) = viewModelScope.launch { repository.upsertIngredient(ingredient) }

    fun deleteIngredient(ingredient: Ingredient) = viewModelScope.launch { repository.deleteIngredient(ingredient.id) }

    fun ingredientUsage(id: String) = repository.ingredientUsage(id)

    private val energyUnit: EnergyUnit get() = repository.snapshot.value.settings.energyUnit

    /** What the ingredients add up to — the same figure the sheet pre-fills. */
    private fun computedEnergy(draft: MealDraft): Energy =
        MealEntry(items = draft.items).energy(repository.snapshot.value.ingredientsById) { null }

    /**
     * What to call a meal you didn't start from a recipe.
     *
     * Built from what is in it, because there is nothing else to go on. Only
     * ever a default: it is the placeholder in the name box, and what gets used
     * if you tick "save this" and type nothing. Silently saving nothing because
     * a field was blank would be worse than an inelegant name.
     */
    fun suggestedName(draft: MealDraft): String {
        if (draft.title.isNotBlank()) return draft.title
        val byId = repository.snapshot.value.ingredientsById
        val names = draft.items.map { byId[it.ingredientId]?.name ?: it.ingredientId }
        return when {
            names.isEmpty() -> ""
            names.size <= 3 -> names.joinToString(", ")
            else -> names.take(3).joinToString(", ") + " and ${names.size - 3} more"
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setColours(theme: ColourTheme) = viewModelScope.launch { repository.setColours(theme) }

    fun setDisplayFace(face: TypeFace) = viewModelScope.launch { repository.setDisplayFace(face) }

    fun setBodyFace(face: TypeFace) = viewModelScope.launch { repository.setBodyFace(face) }

    fun setCustomPalette(palette: CustomPalette) =
        viewModelScope.launch { repository.setCustomPalette(palette) }

    fun setEnergyUnit(unit: EnergyUnit) = viewModelScope.launch { repository.setEnergyUnit(unit) }

    fun setTextSize(size: TextSize) = viewModelScope.launch { repository.setTextSize(size) }

    fun exportJson(): String = repository.exportJson()

    /**
     * Logging on a day you're looking back at keeps that day's date but the
     * current time of day, which is nearly always what's meant — you're filling
     * in yesterday's dinner, not claiming to have eaten it just now.
     */
    private fun LocalDateTime.onViewedDate(): LocalDateTime {
        val date = viewedDate.value
        return if (date == toLocalDate()) this else date.atTime(toLocalTime())
    }

    /** Convenience for the screens, which mostly want the attribute set of a meal. */
    fun attributesOf(meal: MealEntry): Set<Attribute> = meal.attributes(repository.snapshot.value.ingredientsById)
}
