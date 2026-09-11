package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Attribute
import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.alsoOffers
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.MealType
import dev.mahourigan.fooddiary.domain.Portion
import dev.mahourigan.fooddiary.domain.PortionSize
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.analysis.Suggestion
import dev.mahourigan.fooddiary.domain.amountIn
import dev.mahourigan.fooddiary.domain.describe
import dev.mahourigan.fooddiary.domain.forPicker
import dev.mahourigan.fooddiary.domain.resolve
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

/**
 * Building a meal, as a sequence of taps rather than a form.
 *
 * ```
 * Breakfast  →  Toast ›  →  Sourdough  →  Butter  →  Vegemite  →  Save
 *            →  Drinks ›  →  Coffee    →  Oat milk
 * ```
 *
 * Two rules make it feel like that:
 *
 * **Each step decides the next.** Choosing breakfast decides which categories
 * you see; choosing Toast decides what's inside it. Once answered, a step stops
 * taking up room — the meal type isn't a chip row pinned to every screen.
 *
 * **Things live where you'd reach for them.** Butter is inside Toast, not
 * alongside it, because it's something you put on toast. Nothing declares that:
 * it's the bundled hint on day one and your own log after that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMealScreen(
    state: UiState,
    draft: MealDraft,
    onUpdate: ((MealDraft) -> MealDraft) -> Unit,
    onChooseMealType: (MealType) -> Unit,
    onBackToMealType: () -> Unit,
    onAddRecipe: (Recipe) -> Unit,
    onRemoveBase: (String) -> Unit,
    onBrowse: (List<String>) -> Unit,
    onOpenCategory: (Category) -> Unit,
    onToggleFacet: (Facet, FacetOption) -> Unit,
    onAddFacetOption: (Category, Facet, FacetOption) -> Unit,
    onCreateIngredient: (String) -> Ingredient,
    onShowCategoryBuild: (Boolean) -> Unit,
    onCommitCategory: () -> Unit,
    onCancelCategory: () -> Unit,
    onAddIngredient: (Ingredient) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onSetPortion: (String, Portion?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var portionFor by remember { mutableStateOf<MealItem?>(null) }
    var showAllItems by remember { mutableStateOf(false) }
    // Something added by search while a category was open. Offered to the
    // category's list, because that is exactly the moment you find out its
    // shortlist is missing something.
    var offerToCategory by remember { mutableStateOf<Ingredient?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(headingFor(draft), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (draft.stage == MealStage.PICKING) {
                        TextButton(onClick = onSave, enabled = draft.items.isNotEmpty()) { Text("Save") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {

            if (draft.stage == MealStage.CHOOSING_TYPE) {
                MealTypeStep(draft = draft, onChoose = onChooseMealType)
                return@Column
            }

            // What's in the meal so far, always visible while picking. Compact
            // on purpose: it's a receipt, not the working area, and it must not
            // push the next tap off the screen.
            if (draft.bases.isNotEmpty() || draft.items.isNotEmpty()) {
                InMealStrip(
                    state = state,
                    draft = draft,
                    expanded = showAllItems,
                    onToggleExpanded = { showAllItems = !showAllItems },
                    onRemoveBase = onRemoveBase,
                    onRemoveIngredient = onRemoveIngredient,
                    onEditPortion = { portionFor = it },
                )
                HorizontalDivider()
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(if (draft.items.isEmpty()) "Search, or tap below" else "Add something else") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            )
            HorizontalDivider()

            if (query.isNotBlank()) {
                SearchResults(
                    state = state,
                    draft = draft,
                    query = query,
                    onAddRecipe = { onAddRecipe(it); query = "" },
                    onAddIngredient = { ingredient ->
                        onAddIngredient(ingredient)
                        query = ""
                        val open = state.category(draft.categoryId)
                        val listed = open?.facets.orEmpty()
                            .flatMap { it.options }
                            .any { it.ingredientId == ingredient.id }
                        if (open != null && !listed) offerToCategory = ingredient
                    },
                )
                return@Column
            }

            val category = state.category(draft.categoryId)

            offerToCategory?.let { ingredient ->
                val into = category?.facets?.firstOrNull { it.kind == FacetKind.ITEMS }
                if (category == null || into == null) {
                    offerToCategory = null
                } else {
                    AlertDialog(
                        onDismissRequest = { offerToCategory = null },
                        title = { Text("Add to ${category.name}?") },
                        text = {
                            Text(
                                "${ingredient.name} is in this meal either way. This is " +
                                    "about whether it shows up as a chip next time you " +
                                    "open ${category.name}.",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onAddFacetOption(
                                        category,
                                        into,
                                        FacetOption(ingredient.name, ingredientId = ingredient.id),
                                    )
                                    offerToCategory = null
                                },
                            ) { Text("Add it") }
                        },
                        dismissButton = {
                            TextButton(onClick = { offerToCategory = null }) { Text("Not now") }
                        },
                    )
                }
            }
            if (category != null) {
                CategorySheet(
                    state = state,
                    draft = draft,
                    category = category,
                    onToggle = onToggleFacet,
                    onAddOption = { facet, option -> onAddFacetOption(category, facet, option) },
                    onCreateIngredient = onCreateIngredient,
                    onAddRecipe = onAddRecipe,
                    onShowBuild = onShowCategoryBuild,
                    onDone = onCommitCategory,
                    onBack = onCancelCategory,
                )
                return@Column
            }

            if (draft.path.isNotEmpty()) {
                Picker(
                    state = state,
                    draft = draft,
                    onBrowse = onBrowse,
                    onBackToMealType = onBackToMealType,
                    onAddRecipe = onAddRecipe,
                    onAddIngredient = onAddIngredient,
                )
                return@Column
            }

            TopLevel(
                state = state,
                draft = draft,
                onBackToMealType = onBackToMealType,
                onOpenCategory = onOpenCategory,
                onBrowse = onBrowse,
                onAddRecipe = onAddRecipe,
            )
        }
    }

    portionFor?.let { item ->
        PortionDialog(
            name = state.nameOf(item.ingredientId),
            ingredient = state.ingredient(item.ingredientId),
            portion = item.portion,
            onDismiss = { portionFor = null },
            onSet = { portion ->
                onSetPortion(item.ingredientId, portion)
                portionFor = null
            },
        )
    }
}

private fun headingFor(draft: MealDraft): String = when {
    draft.stage == MealStage.CHOOSING_TYPE -> if (draft.isEditingExisting) "Which meal?" else "Log a meal"
    draft.path.isNotEmpty() -> draft.path.last()
    else -> draft.mealType?.label ?: "Log a meal"
}

/**
 * Step one: which meal is this.
 *
 * A category, not a clock setting — it decides what you're offered next. The
 * time is asked for on the way out, where it belongs, because you nearly always
 * know what you ate before you care what time it was.
 */
@Composable
private fun MealTypeStep(draft: MealDraft, onChoose: (MealType) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(MealType.entries.toList(), key = { it.name }) { type ->
            ListItem(
                headlineContent = {
                    Text(type.label, style = MaterialTheme.typography.titleMedium)
                },
                trailingContent = {
                    // The clock's guess, marked rather than applied. It's right
                    // most of the time and it still costs one tap to confirm,
                    // which is the tap that opens the right list.
                    if (draft.mealType == type) {
                        Text(
                            "likely",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.clickable { onChoose(type) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** The meal so far: the saved parts, then the loose ingredients. */
@Composable
private fun InMealStrip(
    state: UiState,
    draft: MealDraft,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRemoveBase: (String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onEditPortion: (MealItem) -> Unit,
) {
    // Ingredients the saved parts brought are hidden by default — you added
    // "Toast, sourdough", you don't need to be shown "sourdough" underneath it.
    // They're one tap away when a portion needs changing.
    val fromBases = draft.expectedItems.map { it.ingredientId }.toSet()
    val loose = draft.items.filterNot { it.ingredientId in fromBases }
    val shown = if (expanded) draft.items else loose

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            draft.bases.forEach { basis ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveBase(basis.recipeId) },
                    label = { Text(basis.name, style = MaterialTheme.typography.labelMedium) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    },
                )
            }

            shown.forEach { item ->
                InputChip(
                    selected = false,
                    onClick = { onEditPortion(item) },
                    label = {
                        Text(describeItem(state, item), style = MaterialTheme.typography.labelMedium)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemoveIngredient(item.ingredientId) },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                        }
                    },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val attributes = state.attributesOf(draft.items)
            if (attributes.isNotEmpty()) {
                AttributeRow(attributes, max = 5, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }

            val energy = state.energyOf(draft.items)
            if (energy.isKnown) {
                Text(
                    energy.describe(state.energyUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (draft.bases.isNotEmpty() && fromBases.isNotEmpty()) {
                TextButton(onClick = onToggleExpanded) {
                    Text(
                        when {
                            expanded -> "Hide ingredients"
                            fromBases.size == 1 -> "1 ingredient"
                            else -> "${fromBases.size} ingredients"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

/**
 * The top of the picker: how you want to find the food, then the food.
 *
 * **Build** is categories — Toast, Porridge, Coffee — because most of what you
 * eat is assembled rather than looked up. Whole saved meals used to sit here as
 * a flat list underneath, which made the first screen a jumble of two different
 * kinds of thing; they now live under **Recent** and **Saved**, which is where
 * you'd go looking for them anyway.
 */
@Composable
private fun TopLevel(
    state: UiState,
    draft: MealDraft,
    onBackToMealType: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onBrowse: (List<String>) -> Unit,
    onAddRecipe: (Recipe) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBackToMealType)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(draft.mealType?.label.orEmpty(), style = MaterialTheme.typography.titleSmall)
        }
        HorizontalDivider()

        // No tabs. Every chip here is the same kind of thing — a door you tap
        // to narrow down — and "Recipes" is the last of them rather than a
        // separate mode you switch into.
        run {
            val entries = state.categories(draft)
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            entries.forEach { entry ->
                                when (entry) {
                                    is Suggestion.BuildIt -> AssistChip(
                                        onClick = { onOpenCategory(entry.category) },
                                        label = { Text(entry.label) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    )

                                    is Suggestion.Group -> AssistChip(
                                        onClick = { onBrowse(entry.path) },
                                        label = { Text(entry.label) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    )

                                    else -> Unit
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun RecipeList(
    state: UiState,
    recipes: List<Recipe>,
    empty: String,
    onAddRecipe: (Recipe) -> Unit,
) {
    if (recipes.isEmpty()) {
        Text(
            empty,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeRow(state = state, recipe = recipe, onAdd = { onAddRecipe(recipe) })
        }
    }
}

/**
 * One saved thing, name over what's in it.
 *
 * Shared with the category sheet's "the usual" list, so a saved toast looks the
 * same wherever you meet it.
 */
@Composable
private fun RecipeRow(state: UiState, recipe: Recipe, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(recipe.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                recipe.items.joinToString(", ") { state.nameOf(it.ingredientId).lowercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * A category, as a couple of questions.
 *
 * The style facet is multi-select on purpose: a wholemeal sourdough multigrain
 * loaf is one bread with three characteristics, so those tick together and
 * resolve to a single ingredient carrying all three. Ticking them as separate
 * breads would say you ate three loaves, which would be both untrue and quietly
 * ruinous for the analysis.
 */
@Composable
private fun CategorySheet(
    state: UiState,
    draft: MealDraft,
    category: Category,
    onToggle: (Facet, FacetOption) -> Unit,
    onAddOption: (Facet, FacetOption) -> Unit,
    onCreateIngredient: (String) -> Ingredient,
    onAddRecipe: (Recipe) -> Unit,
    onShowBuild: (Boolean) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    // Saved things tagged with this category's name. Most mornings it's the same
    // toast, so those come first and the questions are one tap away — not the
    // other way round.
    val usuals = state.usuals(category, draft)

    // Undecided opens on the usuals when there are any. With none, the toggle
    // would be a switch over an empty list, so the questions are all there is.
    val building = draft.buildingCategory ?: usuals.isEmpty()
    val resolved = category.resolve(draft.facetChoices, state.snapshot.ingredientsById)
    var addingTo by remember { mutableStateOf<Facet?>(null) }
    // Which facet has its role overflow open, by name. One at a time.
    var expanded by remember { mutableStateOf<String?>(null) }
    // A search box per section, keyed by facet name. Kept here rather than in
    // each row so scrolling a long list doesn't lose what you typed.
    var queries by remember { mutableStateOf(mapOf<String, String>()) }

    addingTo?.let { facet ->
        AddOptionDialog(
            state = state,
            facet = facet,
            onDismiss = { addingTo = null },
            onCreateIngredient = onCreateIngredient,
            onAdd = { option ->
                onAddOption(facet, option)
                addingTo = null
            },
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    listOfNotNull(draft.mealType?.label, category.name).joinToString(" › "),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            HorizontalDivider()

            // Two named halves rather than one unlabelled button. A toggle can
            // say which side you're on and that there is another one; a lone
            // pill can say neither, and its label has to change meaning under
            // your thumb to get you back.
            //
            // Only shown when there's something saved — otherwise it's a switch
            // over an empty list.
            if (usuals.isNotEmpty()) {
                // One joined control, not two chips side by side. Separate chips
                // read as two independent things you could tick; a switch reads
                // as one thing with two positions, which is what this is — and
                // it shows the position you *aren't* in, so the other half is
                // discoverable without being pressed.
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    SegmentedButton(
                        selected = !building,
                        onClick = { onShowBuild(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text("The usual", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = building,
                        onClick = { onShowBuild(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("Build one", style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (!building) {
            items(usuals, key = { "usual-${it.id}" }) { recipe ->
                RecipeRow(state = state, recipe = recipe, onAdd = { onAddRecipe(recipe) })
            }
            return@LazyColumn
        }

        category.facets.forEach { facet ->
            item(key = "facet-${facet.name}") {
                val chosen = draft.facetChoices[facet.name].orEmpty()
                val query = queries[facet.name].orEmpty()

                // The shortlist, then everything the facet subscribes to by
                // role. Alphabetical throughout: the seeded order was a guess
                // at what you reach for most, and a guess is worse than a rule
                // you can predict once a list is long enough to scan.
                val shortlist = facet.options.sortedBy { it.label.lowercase() }
                val overflow = facet.alsoOffers(state.snapshot.ingredients)
                    .map { FacetOption(it.name, ingredientId = it.id) }

                // A pick made from the overflow has to stay visible after the
                // overflow closes, or collapsing it would look like the tick
                // was thrown away.
                val pinned = overflow.filter { it.key in chosen }
                val visible = when {
                    query.isNotBlank() -> (shortlist + overflow)
                        .filter { it.matches(query, state) }
                        .sortedBy { it.label.lowercase() }

                    expanded == facet.name -> (shortlist + overflow).sortedBy { it.label.lowercase() }
                    else -> (shortlist + pinned).sortedBy { it.label.lowercase() }
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            facet.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (facet.kind == FacetKind.STYLE) {
                            // Worth saying once per screen: the ticks describe
                            // one thing, they don't add several.
                            Text(
                                "describes one",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Only where there's enough to be worth searching. A search
                    // box over eight breads is furniture.
                    if (shortlist.size + overflow.size > SEARCHABLE_FROM) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = { typed -> queries = queries + (facet.name to typed) },
                            placeholder = {
                                Text(
                                    "Search ${facet.name.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { queries = queries + (facet.name to "") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        visible.forEach { option ->
                            FilterChip(
                                selected = option.key in chosen,
                                onClick = { onToggle(facet, option) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                            )
                        }

                        // The seeded lists are a starting point, not the menu.
                        // Without this the categories are only ever as good as
                        // whatever shipped, and the first unusual bread sends
                        // you back to searching.
                        AssistChip(
                            onClick = { addingTo = facet },
                            label = { Text("Add", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )

                        // While searching you're already looking at everything,
                        // so offering to show more would be a lie.
                        if (overflow.isNotEmpty() && query.isBlank()) {
                            AssistChip(
                                onClick = { expanded = if (expanded == facet.name) null else facet.name },
                                label = {
                                    Text(
                                        if (expanded == facet.name) "Fewer" else "More (${overflow.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                            )
                        }
                    }

                    if (visible.isEmpty()) {
                        Text(
                            "Nothing matching. Use + Add to put it in the list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item {
            Column(modifier = Modifier.padding(12.dp)) {
                if (resolved.isNotEmpty()) {
                    Text(
                        resolved.joinToString(", ") { item ->
                            val name = state.nameOf(item.ingredientId)
                            val made = item.modifiers.joinToString(", ") { Attributes.label(it).lowercase() }
                            if (made.isNotEmpty()) "$name ($made)" else name
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AttributeRow(state.attributesOf(resolved), max = 8, modifier = Modifier.weight(1f))
                        val energy = state.energyOf(resolved)
                        if (energy.isKnown) {
                            Text(
                                energy.describe(state.energyUnit),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = onDone,
                    enabled = resolved.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (resolved.isEmpty()) "Pick something" else "Add to the meal") }
            }
        }
    }
}

/**
 * Adding an option to a facet.
 *
 * Two different things, kept apart because they behave differently. Picking an
 * *ingredient* adds a thing — a new topping, or on a style facet a different
 * bread, which then decides what the loaf is. Typing a *description* adds a way
 * something was made, which describes whatever loaf you picked rather than
 * replacing it. That second one only makes sense on a style facet, so it's only
 * offered there.
 */
@Composable
internal fun AddOptionDialog(
    state: UiState,
    facet: Facet,
    onDismiss: () -> Unit,
    onCreateIngredient: (String) -> Ingredient,
    onAdd: (FacetOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var descriptor by remember { mutableStateOf("") }

    val existing = facet.options.mapNotNull { it.ingredientId }.toSet()
    val matches = state.snapshot.ingredients
        .filter { it.matches(query) && it.id !in existing }
        .sortedBy { it.name.length }
        .take(8)

    val exact = query.isNotBlank() &&
        state.snapshot.ingredients.none { it.name.equals(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${facet.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(if (facet.kind == FacetKind.STYLE) "A different one" else "Something to add") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (query.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    matches.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAdd(FacetOption(label = ingredient.name, ingredientId = ingredient.id))
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            Column {
                                Text(ingredient.name, style = MaterialTheme.typography.bodyMedium)
                                AttributeRow(ingredient.attributes, max = 4)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    if (exact) {
                        TextButton(
                            onClick = {
                                val made = onCreateIngredient(query)
                                onAdd(FacetOption(label = made.name, ingredientId = made.id))
                            },
                        ) { Text("Create \"${query.trim()}\"") }
                        Text(
                            // Said out loud, because an untagged ingredient is
                            // invisible to the analysis and it would be worse to
                            // let that happen quietly.
                            "It'll have no attributes yet, so it won't count towards anything until you " +
                                "tag it under Library → Ingredients.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (facet.kind == FacetKind.STYLE) {
                    Spacer(Modifier.height(14.dp))
                    Text("Or how it was made", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Describes whatever you picked rather than replacing it — \"long-fermented\", " +
                            "\"par-baked\", \"from the bakery\".",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = descriptor,
                            onValueChange = { descriptor = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                val label = descriptor.trim()
                                if (label.isNotBlank()) {
                                    onAdd(
                                        FacetOption(
                                            label = label,
                                            modifiers = setOf(Attribute.custom(label)),
                                        ),
                                    )
                                }
                            },
                            enabled = descriptor.isNotBlank(),
                        ) { Icon(Icons.Default.Add, contentDescription = "Add description") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/**
 * The narrowing grid, for folders of recipes and for the door to all of them.
 *
 * Categories don't come through here — they're questions, not lists.
 */
@Composable
private fun Picker(
    state: UiState,
    draft: MealDraft,
    onBrowse: (List<String>) -> Unit,
    onBackToMealType: () -> Unit,
    onAddRecipe: (Recipe) -> Unit,
    onAddIngredient: (Ingredient) -> Unit,
) {
    val suggestions = state.suggestions(draft)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (draft.path.isEmpty()) onBackToMealType() else onBrowse(draft.path.dropLast(1))
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    (listOfNotNull(draft.mealType?.label) + draft.path).joinToString(" › "),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            HorizontalDivider()
        }

        item {
            Text(
                when {
                    draft.path.isNotEmpty() -> "In ${draft.path.last()}"
                    draft.items.isEmpty() -> "Start with"
                    else -> "Add more"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestions.forEach { suggestion ->
                    when (suggestion) {
                        is Suggestion.Group -> AssistChip(
                            onClick = { onBrowse(suggestion.path) },
                            label = { Text(suggestion.label) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )

                        is Suggestion.SavedMeal -> AssistChip(
                            onClick = { onAddRecipe(suggestion.recipe) },
                            label = { Text(suggestion.label) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )

                        is Suggestion.Loose -> AssistChip(
                            onClick = { onAddIngredient(suggestion.ingredient) },
                            label = { Text(suggestion.label) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )

                        // Categories are offered at the top level only; inside a
                        // folder there is nothing to build.
                        is Suggestion.BuildIt -> Unit
                    }
                }
            }
        }

        if (suggestions.isEmpty()) {
            item {
                Text(
                    if (draft.path.isEmpty()) {
                        "Nothing saved yet — search above to build a meal, and you'll be offered the " +
                            "chance to keep it."
                    } else {
                        "Nothing else in here yet. Search above; whatever you add will be offered here " +
                            "next time."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Getting from Toast to Drinks without going back up a level.
        if (draft.path.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { onBrowse(emptyList()) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                ) { Text("Add from somewhere else") }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: UiState,
    draft: MealDraft,
    query: String,
    onAddRecipe: (Recipe) -> Unit,
    onAddIngredient: (Ingredient) -> Unit,
) {
    val recipes = state.snapshot.recipes
        .filter { it.matches(query) && it.id !in draft.baseIds }
        .sortedBy { it.name.length }
        .take(8)

    val ingredients = state.snapshot.ingredients
        .filter { it.matches(query) && !draft.contains(it.id) }
        .sortedBy { it.name.length }
        .take(40)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (recipes.isNotEmpty()) {
            item {
                Text(
                    "Saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
                )
            }
            items(recipes, key = { "r-${it.id}" }) { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddRecipe(recipe) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(recipe.name, style = MaterialTheme.typography.bodyMedium)
                        recipe.tags.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                it.sorted().joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (ingredients.isNotEmpty()) {
            item {
                Text(
                    "Ingredients",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
                )
            }
            items(ingredients, key = { "i-${it.id}" }) { ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddIngredient(ingredient) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ingredient.name, style = MaterialTheme.typography.bodyMedium)
                        AttributeRow(ingredient.attributes, max = 5)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (recipes.isEmpty() && ingredients.isEmpty()) {
            item {
                Text(
                    "Nothing in the library matches. Add it under Library → Ingredients, with its " +
                        "attributes, and it'll be there every time after that.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

/**
 * How much — at whichever level of detail you feel like.
 *
 * "Nothing" is a first-class option and the one that clears the field, because
 * an unspecified portion has to stay genuinely unspecified rather than being
 * quietly recorded as normal.
 */
@Composable
private fun PortionDialog(
    name: String,
    ingredient: Ingredient?,
    portion: Portion?,
    onDismiss: () -> Unit,
    onSet: (Portion?) -> Unit,
) {
    var amount by remember { mutableStateOf(portion?.amount?.let { formatAmountField(it) } ?: "") }
    val unit = portion?.unit ?: ingredient?.defaultUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column {
                Text("Roughly", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PortionSize.entries.forEach { size ->
                        FilterChip(
                            selected = portion?.size == size && portion.amount == null,
                            onClick = { onSet(Portion.of(size)) },
                            label = { Text(size.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    if (unit != null) "Or exactly, in $unit" else "Or exactly",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(110.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    if (unit != null) Text(unit, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    onSet(if (value == null) portion else Portion.of(value, unit))
                },
            ) { Text("Set") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSet(null) }) { Text("Nothing") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/**
 * The way out: when, any notes, and what to do about the library.
 *
 * The time is asked here rather than up front because you know what you ate
 * before you care what time it was, and most of the time the answer is "just
 * now" and needs no thought at all.
 */
@Composable
fun SaveMealSheet(
    state: UiState,
    draft: MealDraft,
    onUpdate: ((MealDraft) -> MealDraft) -> Unit,
    onTimeNow: () -> Unit,
    onTimeExact: (LocalDateTime) -> Unit,
    onTimeVague: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    suggestedName: String,
) {
    val combination = draft.bases.size > 1
    var picking by remember { mutableStateOf(false) }

    // Tags already in use, plus the meal type — "Dinner" is the one people
    // reach for first and it would be odd to have to type it out.
    val offeredTags = (state.knownTags() + listOfNotNull(draft.mealType?.label))
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When was it?") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = draft.timeChoice == TimeChoice.NOW,
                        onClick = onTimeNow,
                        label = { Text("Just now", style = MaterialTheme.typography.labelMedium) },
                    )
                    FilterChip(
                        selected = draft.timeChoice == TimeChoice.EXACT,
                        onClick = { picking = true },
                        label = {
                            Text(
                                if (draft.timeChoice == TimeChoice.EXACT) formatTime(draft.at) else "At…",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                    FilterChip(
                        selected = draft.timeChoice == TimeChoice.VAGUE,
                        onClick = onTimeVague,
                        label = {
                            Text(
                                draft.mealType?.vagueLabel ?: "Not sure",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }

                if (draft.timeChoice == TimeChoice.VAGUE) {
                    Text(
                        // Said plainly rather than hidden: the whole analysis
                        // measures from the meal time, so a vague one is a real
                        // cost even though it beats not logging at all.
                        "Recorded as about ${formatTime(draft.at)} and marked approximate. Symptoms are " +
                            "lined up against meal times, so this one will count for less.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // The last of the three levels: a number you actually know beats
                // anything worked out from servings.
                val estimate = state.energyOf(draft.items)
                val computed = estimate.amountIn(state.energyUnit)?.toString()

                // Show the worked-out figure rather than an empty box with a
                // ghost of it. You can type over it, and leaving it alone means
                // exactly what an empty box used to mean — see below.
                LaunchedEffect(computed) {
                    if (computed != null && draft.kcal.isBlank()) {
                        onUpdate { it.copy(kcal = computed) }
                    }
                }
                val edited = computed != null && draft.kcal != computed

                OutlinedTextField(
                    value = draft.kcal,
                    onValueChange = { entered ->
                        onUpdate { it.copy(kcal = entered.filter { c -> c.isDigit() }.take(6)) }
                    },
                    label = { Text(state.energyUnit.label) },
                    placeholder = { Text("optional") },
                    supportingText = {
                        Text(
                            when {
                                computed == null ->
                                    "Nothing to work it out from — none of these have energy data."

                                edited && draft.kcal.isNotBlank() ->
                                    "Yours, used as given instead of the ingredients."

                                edited ->
                                    "Cleared, so this meal will have no figure at all."

                                else ->
                                    "Worked out from the ingredients. Type over it to set your own."
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { notes -> onUpdate { it.copy(notes = notes) } },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                // One question either way, now that changing a saved meal is
                // something you do in Saved meals rather than while logging
                // dinner: keep this, or don't. See MealDraft.saveCard for when
                // it's asked at all.
                val card = draft.saveCard

                if (card != SaveCard.NONE) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onUpdate {
                                        it.copy(
                                            saveBack = if (it.saveBack == SaveBack.NEW_RECIPE) {
                                                SaveBack.JUST_ONCE
                                            } else {
                                                SaveBack.NEW_RECIPE
                                            },
                                        )
                                    }
                                },
                            ) {
                                Checkbox(
                                    checked = draft.saveBack == SaveBack.NEW_RECIPE,
                                    onCheckedChange = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when {
                                        card == SaveCard.KEEP_AS_MEAL -> "Save this as a meal"
                                        combination -> "Save this combination as a meal"
                                        else -> "Save this version as a new meal"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Text(
                                if (card == SaveCard.KEEP_AS_MEAL) {
                                    "So you can add it in one tap next time. Today's is logged either way."
                                } else {
                                    // Said plainly, because the old version of
                                    // this screen offered to overwrite the
                                    // recipe and that was a mistake waiting to
                                    // happen while you were logging dinner.
                                    "Today's is logged as you built it either way, and " +
                                        "${draft.title} is left alone — saved meals are " +
                                        "edited in Saved meals."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (draft.saveBack == SaveBack.NEW_RECIPE) {
                                OutlinedTextField(
                                    value = draft.newRecipeName,
                                    onValueChange = { name -> onUpdate { it.copy(newRecipeName = name) } },
                                    label = { Text("Call it") },
                                    // Said in the supporting line rather than as
                                    // a placeholder: with a label, Material only
                                    // shows a placeholder once the field has
                                    // focus, and what you get by leaving it alone
                                    // is exactly what you'd want to see first.
                                    supportingText = if (draft.newRecipeName.isBlank() && suggestedName.isNotBlank()) {
                                        {
                                            Text(
                                                "Left blank: \"$suggestedName\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 2,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                )

                                // Only tags already in use. Inventing one is a
                                // job for the recipe editor — here you are
                                // logging dinner, and a free-text box is how
                                // "cafe" and "Cafe" become two chips.
                                if (offeredTags.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tags — optional",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        offeredTags.forEach { tag ->
                                            val on = draft.newRecipeTags.any { it.equals(tag, ignoreCase = true) }
                                            FilterChip(
                                                selected = on,
                                                onClick = {
                                                    onUpdate { d ->
                                                        d.copy(
                                                            newRecipeTags = if (on) {
                                                                d.newRecipeTags.filterNot {
                                                                    it.equals(tag, ignoreCase = true)
                                                                }.toSet()
                                                            } else {
                                                                d.newRecipeTags + tag
                                                            },
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(tag, style = MaterialTheme.typography.labelMedium)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )

    if (picking) {
        ClockDialog(
            initial = draft.at,
            onDismiss = { picking = false },
            onPick = { hour, minute ->
                onTimeExact(draft.at.withHour(hour).withMinute(minute))
                picking = false
            },
        )
    }
}

private fun formatAmountField(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/** Long enough that scanning it beats reading it, so it earns a search box. */
private const val SEARCHABLE_FROM = 8

/**
 * Does this option answer to what was typed?
 *
 * Matches the label you see and, where the option names an ingredient, that
 * ingredient's own aliases — so searching "aubergine" finds Eggplant here just
 * as it does in the main search.
 */
private fun FacetOption.matches(query: String, state: UiState): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    if (label.lowercase().contains(q)) return true
    return ingredientId?.let { state.ingredient(it)?.matches(q) } ?: false
}
