package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Attribute
import dev.mahourigan.fooddiary.domain.EnergyUnit
import dev.mahourigan.fooddiary.ui.theme.ColourTheme
import dev.mahourigan.fooddiary.ui.theme.CustomPalette
import dev.mahourigan.fooddiary.ui.theme.TypeFace
import dev.mahourigan.fooddiary.ui.theme.severityRampOf
import dev.mahourigan.fooddiary.ui.theme.toHsl
import dev.mahourigan.fooddiary.ui.theme.fromHsl
import dev.mahourigan.fooddiary.ui.theme.warnings
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import dev.mahourigan.fooddiary.ui.theme.previewColors
import dev.mahourigan.fooddiary.ui.theme.typeFor
import dev.mahourigan.fooddiary.ui.theme.ThemeMode
import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.domain.TextSize
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Portion
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.Roles
import dev.mahourigan.fooddiary.domain.attributes
import dev.mahourigan.fooddiary.domain.describe
import dev.mahourigan.fooddiary.domain.forPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

/** The way in to everything that isn't the day itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: UiState,
    onCategories: () -> Unit,
    onRecipes: () -> Unit,
    onIngredients: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Categories") },
                supportingContent = {
                    Text(
                        state.snapshot.categories.joinToString(", ") { it.name }
                            .ifBlank { "The things you build a meal from" },
                    )
                },
                modifier = Modifier.clickable(onClick = onCategories),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Recipes") },
                supportingContent = { Text("${state.snapshot.recipes.size} recipes") },
                modifier = Modifier.clickable(onClick = onRecipes),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Ingredients") },
                supportingContent = { Text("${state.snapshot.ingredients.size} in the library, all editable") },
                modifier = Modifier.clickable(onClick = onIngredients),
            )
            HorizontalDivider()
            // No Settings row: the cog on the day screen is the way in. This
            // list is the food you have described to the app, and the units and
            // the theme were only ever in here because there was nowhere else.
            ListItem(
                headlineContent = { Text("Export") },
                supportingContent = { Text("Copy the whole diary as JSON") },
                modifier = Modifier.clickable(onClick = onExport),
            )
            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("About the tagging", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // These caveats belong in the app, not just in the plan.
                        // Someone acting on a FODMAP tag deserves to know how
                        // coarse it is.
                        "The FODMAP tags are a coarse high/not-high judgement, not certified figures — a " +
                            "starting point for what to suspect rather than a lookup table.\n\n" +
                            "Tags describe an ingredient generically, and products differ: soy sauce is " +
                            "usually wheat but tamari isn't, one oat milk has added inulin and another " +
                            "doesn't. Correct anything that's wrong for what you buy — your edits stick, " +
                            "including through app updates.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Text(
                "Nothing you log ever leaves the phone, and no analysis is done by anything " +
                    "other than arithmetic over your own entries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(Modifier.height(8.dp))
            // This paragraph exists because the sentence above used to end
            // "there's no network permission", and that stopped being true the
            // day the updater was added. Saying exactly what the one request
            // is, and when it happens, is the only honest version.
            Text(
                "The app makes one kind of network request, and only when you tap Check for " +
                    "updates in Settings: it asks GitHub whether a newer version exists. It " +
                    "sends nothing — no diary, no identifier, not even a record that you asked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    state: UiState,
    onEdit: (Recipe) -> Unit,
    onNew: () -> Unit,
    onToggleFavourite: (Recipe) -> Unit,
    onDelete: (Recipe) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New recipe")
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(state.snapshot.recipes.forPicker(), key = { it.id }) { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(recipe) }
                        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            recipe.items.joinToString(", ") { state.nameOf(it.ingredientId).lowercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(4.dp))
                        AttributeRow(recipe.attributes(state.byId), max = 6)
                    }
                    IconButton(onClick = { onToggleFavourite(recipe) }) {
                        Icon(
                            if (recipe.isFavourite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favourite",
                        )
                    }
                    IconButton(onClick = { onDelete(recipe) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * Editing a recipe.
 *
 * Note what this screen deliberately cannot do: change what's already been
 * logged. Entries carry their own resolved items, so tidying a recipe up here
 * never rewrites a single meal you've eaten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    state: UiState,
    recipe: Recipe,
    onSave: (Recipe) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(recipe.name) }
    var tags by remember { mutableStateOf(recipe.tags) }
    var newTag by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(recipe.items) }
    var notes by remember { mutableStateOf(recipe.notes) }
    val unit = state.energyUnit
    val shownRecipeKcal = recipe.kcal?.let { Math.round(unit.from(it.toDouble())).toString() }.orEmpty()
    var kcal by remember { mutableStateOf(shownRecipeKcal) }
    var serves by remember { mutableStateOf(recipe.serves?.toString().orEmpty()) }
    var query by remember { mutableStateOf("") }
    var portionFor by remember { mutableStateOf<MealItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipe.name.isBlank()) "New recipe" else recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                recipe.copy(
                                    name = name.trim(),
                                    tags = tags,
                                    items = items,
                                    notes = notes,
                                    serves = serves.trim().toIntOrNull()?.takeIf { it > 1 },
                                    kcal = if (kcal == shownRecipeKcal) {
                                        recipe.kcal
                                    } else {
                                        kcal.trim().toIntOrNull()
                                            ?.let { Math.round(unit.toKcal(it.toDouble())).toInt() }
                                    },
                                ),
                            )
                        },
                        enabled = name.isNotBlank() && items.isNotEmpty(),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {

        // Pinned, for the same reason as the log screen: a search field inside
        // a scrolling list has the keyboard sitting over its own results.
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Add an ingredient") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        HorizontalDivider()

        if (query.isNotBlank()) {
            val matches = state.snapshot.ingredients
                .filter { candidate -> candidate.matches(query) && items.none { it.ingredientId == candidate.id } }
                .sortedBy { it.name.length }
                .take(40)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(matches, key = { "add-${it.id}" }) { ingredient ->
                    ListItem(
                        headlineContent = { Text(ingredient.name) },
                        supportingContent = { AttributeRow(ingredient.attributes, max = 5) },
                        modifier = Modifier.clickable {
                            items = items + MealItem(ingredientId = ingredient.id)
                            query = ""
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                )

                // A cooked dish is written the way it's cooked and eaten a
                // portion at a time, so this is what stops one dinner being
                // logged as four.
                OutlinedTextField(
                    value = serves,
                    onValueChange = { serves = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Serves — optional") },
                    placeholder = { Text("1") },
                    supportingText = {
                        Text(
                            "The amounts above are the whole dish. Logging it adds one " +
                                "serving, so set this for anything you cook in a batch.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )

                val estimate = state.energyOf(items)
                val portions = serves.toIntOrNull()?.coerceAtLeast(1) ?: 1
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("${unit.label} for the whole thing — optional") },
                    placeholder = { Text(if (estimate.isKnown) estimate.describe(unit) else "optional") },
                    supportingText = {
                        Text(
                            buildString {
                                append(
                                    "Set this for something you cooked and worked out once. It is " +
                                        "then used as-is instead of re-estimating from the ingredients.",
                                )
                                // The number you type here is for the pot; the
                                // number you'll see on the day is a quarter of
                                // it. Say so rather than letting that surprise
                                // you later.
                                if (portions > 1) {
                                    val each = estimate.kcal?.let { it / portions }
                                    val shown = kcal.toIntOrNull()?.let { it / portions }
                                        ?: each?.let { Math.round(unit.from(it)) }
                                    if (shown != null) {
                                        append(" One serving of $portions is about $shown ${unit.suffix}.")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )

                TagEditor(
                    tags = tags,
                    known = state.knownTags(),
                    onChange = { tags = it },
                    newTag = newTag,
                    onNewTag = { newTag = it },
                    note = "Each tag is a chip in the picker. A recipe tagged Cafe " +
                        "shows up behind Cafe as the whole thing — its ingredients don't.",
                )
            }

            items(items, key = { it.ingredientId }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { portionFor = item }
                        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.nameOf(item.ingredientId), style = MaterialTheme.typography.bodyMedium)
                        val ingredient = state.ingredient(item.ingredientId)
                        val described = if (ingredient != null) item.portion.describe(ingredient) else ""
                        if (described.isNotEmpty()) {
                            Text(
                                described,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { items = items.filterNot { it.ingredientId == item.ingredientId } }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }

            item {
                val attributes = state.attributesOf(items)
                if (attributes.isNotEmpty()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Carries", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        AttributeRow(attributes)
                    }
                }
            }
        }
        }
    }

    portionFor?.let { item ->
        RecipePortionDialog(
            name = state.nameOf(item.ingredientId),
            ingredient = state.ingredient(item.ingredientId),
            portion = item.portion,
            onDismiss = { portionFor = null },
            onSet = { portion ->
                items = items.map {
                    if (it.ingredientId == item.ingredientId) it.copy(portion = portion?.takeUnless { p -> p.isEmpty }) else it
                }
                portionFor = null
            },
        )
    }
}

@Composable
private fun RecipePortionDialog(
    name: String,
    ingredient: Ingredient?,
    portion: Portion?,
    onDismiss: () -> Unit,
    onSet: (Portion?) -> Unit,
) {
    var amount by remember { mutableStateOf(portion?.amount?.let { trimAmount(it) } ?: "") }
    val unit = portion?.unit ?: ingredient?.defaultUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column {
                Text(
                    if (unit != null) "How much, in $unit — optional" else "How much — optional",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(amount.toDoubleOrNull()?.let { Portion.of(it, unit) }) }) { Text("Set") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSet(null) }) { Text("Nothing") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsScreen(
    state: UiState,
    onEdit: (Ingredient) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingredients") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New ingredient")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )

            val matches = state.snapshot.ingredients.filter { it.matches(query) }.sortedBy { it.name }

            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(matches, key = { it.id }) { ingredient ->
                    ListItem(
                        headlineContent = { Text(ingredient.name) },
                        supportingContent = { AttributeRow(ingredient.attributes, max = 8) },
                        modifier = Modifier.clickable { onEdit(ingredient) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/**
 * Editing an ingredient's attributes.
 *
 * The most consequential screen in the app for accuracy: a wrong tag here
 * quietly corrupts every conclusion the analysis will ever draw. Hence the note
 * on each attribute, and hence everything being editable rather than baked in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditScreen(
    ingredient: Ingredient,
    usage: dev.mahourigan.fooddiary.data.IngredientUsage,
    energyUnit: EnergyUnit,
    knownTags: List<String>,
    onSave: (Ingredient) -> Unit,
    onDelete: (Ingredient) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(ingredient.name) }
    var attributes by remember { mutableStateOf(ingredient.attributes) }
    var unit by remember { mutableStateOf(ingredient.defaultUnit.orEmpty()) }
    var typical by remember { mutableStateOf(ingredient.typicalAmount?.let { trimAmount(it) } ?: "") }
    // Energy per 100 g never needs decimals, and a converted figure would
    // otherwise show as 3698.656 in a box you are invited to type into.
    val shownKcal = ingredient.kcalPer100?.let { Math.round(energyUnit.from(it)).toString() } ?: ""
    var kcal by remember { mutableStateOf(shownKcal) }
    var roles by remember { mutableStateOf(ingredient.roles) }
    var tags by remember { mutableStateOf(ingredient.tags) }
    var newTag by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf(ingredient.gramsPerUnit?.let { trimAmount(it) } ?: "") }
    var custom by remember { mutableStateOf("") }
    var confirmingDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ingredient.name.isBlank()) "New ingredient" else ingredient.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                ingredient.copy(
                                    id = ingredient.id.ifBlank {
                                        dev.mahourigan.fooddiary.data.SeedIngredients.slug(name).ifBlank {
                                            UUID.randomUUID().toString()
                                        }
                                    },
                                    name = name.trim(),
                                    attributes = attributes,
                                    roles = roles,
                                    tags = tags,
                                    defaultUnit = unit.trim().ifBlank { null },
                                    typicalAmount = typical.toDoubleOrNull(),
                                    // An untouched field keeps the stored value
                                    // exactly. Converting a rounded number back
                                    // would nudge it every time the screen was
                                    // opened and saved in the other unit.
                                    kcalPer100 = if (kcal == shownKcal) {
                                        ingredient.kcalPer100
                                    } else {
                                        kcal.toDoubleOrNull()?.let { energyUnit.toKcal(it) }
                                    },
                                    gramsPerUnit = grams.toDoubleOrNull(),
                                    isUserCreated = true,
                                ),
                            )
                        },
                        enabled = name.isNotBlank(),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("One of these is a…") },
                    placeholder = { Text("slice") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = typical,
                    onValueChange = { typical = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Normally") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "The unit drives the portion stepper, and the normal serving is what \"a little\" and " +
                    "\"lots\" get measured against.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(energyUnit.per100Label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("One ${unit.ifBlank { "unit" }} weighs") },
                    suffix = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                // Saying where the bundled numbers came from matters: they're my
                // estimates, not a database, and someone acting on a calorie
                // total deserves to know that before they trust it.
                "Per 100g is the figure on the packet. The weight of one unit is what turns \"2 slices\" " +
                    "into calories. The bundled numbers are estimates — correct anything you weigh.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            TagEditor(
                tags = tags,
                known = knownTags,
                onChange = { tags = it },
                newTag = newTag,
                onNewTag = { newTag = it },
                note = "Where this shows up in the picker. A hash brown tagged Cafe " +
                    "sits beside the cafe orders; nothing is guessed from what you " +
                    "eat it with.",
            )

            Spacer(Modifier.height(16.dp))
            Text("What it's for", style = MaterialTheme.typography.titleSmall)
            Text(
                // Worth drawing the line explicitly: two sets of chips on one
                // screen otherwise look like the same kind of thing.
                "Only decides which category lists offer it. Nothing here reaches the analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Roles.known.forEach { info ->
                    FilterChip(
                        selected = info.role in roles,
                        onClick = {
                            roles = if (info.role in roles) roles - info.role else roles + info.role
                        },
                        label = { Text(info.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Attributes.byGroup().forEach { (group, infos) ->
                Text(group.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    infos.forEach { info ->
                        FilterChip(
                            selected = info.attribute in attributes,
                            onClick = {
                                attributes = if (info.attribute in attributes) {
                                    attributes - info.attribute
                                } else {
                                    attributes + info.attribute
                                }
                            },
                            label = { Text(info.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // The selected attributes' notes, shown together. This is where the
            // gluten/fructan point actually reaches the person tagging wheat.
            val notes = attributes.mapNotNull { Attributes.info(it) }.filter { it.note.isNotBlank() }
            if (notes.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        notes.forEach { info ->
                            Text(info.label, style = MaterialTheme.typography.labelMedium)
                            Text(info.note, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            val customAttributes = attributes.filter { it.isCustom }
            if (customAttributes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    customAttributes.forEach { attribute ->
                        FilterChip(
                            selected = true,
                            onClick = { attributes = attributes - attribute },
                            label = { Text(Attributes.label(attribute), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Your own attribute") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (custom.isNotBlank()) {
                            attributes = attributes + Attribute.custom(custom)
                            custom = ""
                        }
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Add attribute") }
            }

            if (ingredient.id.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = { confirmingDelete = true }) { Text("Delete from library") }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${ingredient.name}?") },
            text = {
                Text(
                    buildString {
                        if (usage.isUsed) {
                            // Saying this plainly is the point: entries hold ids
                            // and their own item lists, so the log is safe. The
                            // user should not have to guess that.
                            append("It's in ${usage.mealCount} logged meals and ${usage.recipeCount} recipes. ")
                            append("Those stay exactly as they are — logged meals keep their own record of what you ate. ")
                        }
                        append("It just won't be offered when adding to a meal, and won't come back in a later update.")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete(ingredient) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun trimAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/**
 * Display preferences.
 *
 * Only energy so far. Everything is stored in kcal whichever way this is set,
 * so switching it is reversible and can't put the log into two units at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onSetEnergyUnit: (EnergyUnit) -> Unit,
    onSetColours: (ColourTheme) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetTextSize: (TextSize) -> Unit,
    onSetDisplayFace: (TypeFace) -> Unit,
    onSetBodyFace: (TypeFace) -> Unit,
    onSetCustomPalette: (CustomPalette) -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.snapshot.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text("Energy", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnergyUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = settings.energyUnit == unit,
                        onClick = { onSetEnergyUnit(unit) },
                        label = { Text(unit.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Changes what’s shown everywhere, including the box you type a " +
                    "figure into. Stored values don’t change, so switching back " +
                    "gets you exactly what you had.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---- Lettering ----------------------------------------------------
            //
            // Its own section, above colour, because it is the half that decides
            // whether the diary is readable. Two faces picked independently: the
            // scale corrections ride on the faces, so any pairing lands at a
            // sensible size without anybody choosing one.
            Spacer(Modifier.height(26.dp))
            Text("Lettering", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                "Headings and body text are chosen separately, and separately from " +
                    "the colours. Every face is shown in itself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            FaceGroup(
                title = "Headings",
                chosen = settings.displayFace,
                textSize = settings.textSize,
                forBody = false,
                onPick = onSetDisplayFace,
            )
            Spacer(Modifier.height(14.dp))
            FaceGroup(
                title = "Body text",
                chosen = settings.bodyFace,
                textSize = settings.textSize,
                forBody = true,
                onPick = onSetBodyFace,
            )

            Spacer(Modifier.height(16.dp))
            Text("Text size", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextSize.entries.forEach { size ->
                    FilterChip(
                        selected = settings.textSize == size,
                        onClick = { onSetTextSize(size) },
                        label = { Text(size.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Scales both faces by the same amount, so each keeps its own " +
                    "proportions. This is on top of your phone’s font size rather " +
                    "than instead of it — if you have already made text bigger " +
                    "everywhere, you get both.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---- Colour --------------------------------------------------------
            Spacer(Modifier.height(28.dp))
            Text("Colour", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                "Paper, ink and paper texture. Nothing here changes the lettering.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            val custom = settings.custom
            val onCustom = settings.colours == ColourTheme.CUSTOM

            // The light/dark switch belongs to the five built-in themes. A
            // custom palette is one ground with no twin, so rather than leave a
            // control that silently does nothing, it says why it is gone.
            if (onCustom) {
                Text(
                    "Light and dark don’t apply to a custom palette — the paper " +
                        "you pick is the paper. It currently reads as " +
                        (if (custom.isDark) "dark." else "light."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { option ->
                        FilterChip(
                            selected = settings.themeMode == option,
                            onClick = { onSetThemeMode(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "The same theme in different colours. The lettering is set " +
                        "above and doesn’t change with it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ColourTheme.entries.forEach { theme ->
                ThemeOption(
                    theme = theme,
                    dark = dark,
                    custom = custom,
                    displayFace = settings.displayFace,
                    bodyFace = settings.bodyFace,
                    textSize = settings.textSize,
                    selected = settings.colours == theme,
                    onSelect = { onSetColours(theme) },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (onCustom) {
                Spacer(Modifier.height(6.dp))
                CustomPaletteEditor(palette = custom, onChange = onSetCustomPalette)
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            // Last, because it is housekeeping rather than configuration.
            UpdateRow()
        }
    }
}

/**
 * The faces on offer for one slot, each drawn in itself.
 *
 * The same seven appear for headings and for body text. Nothing is hidden from
 * either list — blackletter body text is a legitimate thing to want and the
 * app is not going to argue — but [TypeFace.readableAsBody] earns one line of
 * warning under the body list, because the face that looks best on this screen
 * is not always the one you can read an ingredient list in at seven in the
 * morning.
 */
@Composable
private fun FaceGroup(
    title: String,
    chosen: TypeFace,
    textSize: TextSize,
    forBody: Boolean,
    onPick: (TypeFace) -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))

    TypeFace.entries.forEach { face ->
        val selected = face == chosen
        // Drawn in the face it is offering, at the size that face will
        // actually be set at, so the row is the sample.
        // Both lines in the offered face, not just the name. One word tells
        // you almost nothing about a typeface — what you need to judge is a
        // sentence of it at the size you will read sentences at, which is
        // exactly what the note is.
        val own = typeFor(display = face, body = face, textSize = textSize)
        val sample = if (forBody) own.bodyLarge else own.titleMedium

        Surface(
            onClick = { onPick(face) },
            shape = MaterialTheme.shapes.small,
            color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(face.label, style = sample)
                    Text(
                        face.note,
                        style = own.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (forBody && !chosen.readableAsBody) {
        Text(
            "⚠ ${chosen.label} is a display face. A whole day’s log set in it is " +
                "hard going — but it is your diary.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/**
 * One colour theme in the picker, painted in its own colours.
 *
 * Set in whichever lettering is currently chosen rather than its own, which is
 * the point of the split: these rows are now about colour, and showing five
 * different faces here would say the opposite.
 */
@Composable
private fun ThemeOption(
    theme: ColourTheme,
    dark: Boolean,
    custom: CustomPalette,
    displayFace: TypeFace,
    bodyFace: TypeFace,
    textSize: TextSize,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = previewColors(theme, dark, custom)
    val ground = colors[0]
    val ink = colors[1]
    val type = typeFor(displayFace, bodyFace, textSize)

    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.small,
        color = ground,
        contentColor = ink,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else ink.copy(alpha = 0.22f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.label, style = type.titleMedium, color = ink)
                Text(
                    theme.blurb,
                    style = type.bodyMedium,
                    color = ink.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.width(10.dp))
            // mild → very severe, in this theme’s inks
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                severityRampOf(theme, dark, custom).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(width = 9.dp, height = 22.dp)
                            .background(swatch, MaterialTheme.shapes.extraSmall),
                    )
                }
            }
            if (selected) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The three swatches, and what the app says about them.
 *
 * Three and not nineteen: paper, ink and one accent are choices a person can
 * actually make, and everything else is worked out from them. What is *not*
 * worked out from them is the severity ramp or the live-row edge — those are
 * relit to fixed contrast targets against your paper, because they carry
 * meaning rather than mood.
 *
 * Text contrast is checked and reported and then left to you. The numbers are
 * shown because "this looks a bit faint" is an argument and "3.1:1" is not.
 */
@Composable
private fun CustomPaletteEditor(palette: CustomPalette, onChange: (CustomPalette) -> Unit) {
    var open by remember { mutableStateOf<String?>(null) }
    val warnings = palette.warnings()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(14.dp),
    ) {
        Text("Your swatches", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            "Everything else — surfaces, outlines, containers — is worked out " +
                "from these three.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        SwatchRow(
            name = "Paper",
            hint = "The background, and what decides light or dark",
            value = palette.ground,
            expanded = open == "ground",
            onToggle = { open = if (open == "ground") null else "ground" },
            onChange = { onChange(palette.copy(ground = it)) },
        )
        SwatchRow(
            name = "Ink",
            hint = "Body text, and every muted shade faded from it",
            value = palette.ink,
            expanded = open == "ink",
            onToggle = { open = if (open == "ink") null else "ink" },
            onChange = { onChange(palette.copy(ink = it)) },
        )
        SwatchRow(
            name = "Accent",
            hint = "Buttons, selection, the edge on an open episode",
            value = palette.accent,
            expanded = open == "accent",
            onToggle = { open = if (open == "accent") null else "accent" },
            onChange = { onChange(palette.copy(accent = it)) },
        )

        if (warnings.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            warnings.forEach { warning ->
                Text(
                    warning.what,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    warning.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = palette.ignoreContrast,
                    onCheckedChange = { onChange(palette.copy(ignoreContrast = it)) },
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Use it anyway",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!palette.ignoreContrast) {
                Text(
                    "The colours are already applied either way — this only stops " +
                        "the app mentioning it again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "✓ Contrast is fine on all three.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One swatch: a tap opens hue, saturation and lightness under it. */
@Composable
private fun SwatchRow(
    name: String,
    hint: String,
    value: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (Long) -> Unit,
) {
    val colour = Color(value)
    val hsl = remember(value) { toHsl(colour) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colour, MaterialTheme.shapes.small)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.shapes.small,
                    ),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            // The hint wraps and the hex must not: without the gap and the
            // fixed line, a long hint runs straight into "#7A3B52".
            Text(
                hexOf(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(6.dp))
            var h by remember(value) { mutableFloatStateOf(hsl[0]) }
            var sat by remember(value) { mutableFloatStateOf(hsl[1]) }
            var l by remember(value) { mutableFloatStateOf(hsl[2]) }

            fun push() = onChange(fromHsl(h, sat, l))

            SliderRow("Hue", h / 360f) { h = it * 360f; push() }
            SliderRow("Saturation", sat) { sat = it; push() }
            SliderRow("Lightness", l) { l = it; push() }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp),
        )
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
    }
}

private fun hexOf(value: Long): String {
    val rgb = (value and 0xFFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

/**
 * Free-text tags, on a recipe or an ingredient.
 *
 * The same control for both, because they share one namespace: tagging a hash
 * brown "Cafe" has to put it beside the café orders, and that only works if
 * both ends spell it the same way. Hence the chips for tags already in use —
 * typing is how a second spelling of Cafe gets created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditor(
    tags: Set<String>,
    known: List<String>,
    onChange: (Set<String>) -> Unit,
    newTag: String,
    onNewTag: (String) -> Unit,
    note: String,
) {
    fun toggle(tag: String) {
        val already = tags.any { it.equals(tag, ignoreCase = true) }
        onChange(
            if (already) tags.filterNot { it.equals(tag, ignoreCase = true) }.toSet() else tags + tag,
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Tags", style = MaterialTheme.typography.titleSmall)
        Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))

        // Everything already in use, plus anything on this one that isn't.
        val offered = (known + tags).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
        if (offered.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                offered.forEach { tag ->
                    FilterChip(
                        selected = tags.any { it.equals(tag, ignoreCase = true) },
                        onClick = { toggle(tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        OutlinedTextField(
            value = newTag,
            onValueChange = onNewTag,
            label = { Text("Add a tag") },
            placeholder = { Text("Cafe, Soups, Stews…") },
            singleLine = true,
            trailingIcon = {
                if (newTag.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onChange(tags + newTag.trim())
                            onNewTag("")
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
