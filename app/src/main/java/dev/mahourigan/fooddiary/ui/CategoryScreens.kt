package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.Roles
import dev.mahourigan.fooddiary.domain.Ingredient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp

/**
 * The categories, as a list.
 *
 * Adding an option from inside the picker is the fast path, but nothing there
 * takes one away, and a hidden long-press is a poor place to keep the only way
 * to undo something. This is that place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: UiState,
    onEdit: (Category) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New category")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            items(state.snapshot.categories, key = { it.id }) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    supportingContent = {
                        Text(
                            category.facets.joinToString(" · ") { "${it.name} (${it.options.size})" },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.clickable { onEdit(category) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (state.snapshot.categories.isEmpty()) {
                item {
                    Text(
                        "No categories. One is a couple of questions — which bread, what went on it — " +
                            "which beats saving every combination as a meal of its own.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Everything a category offers, in full, with a way to take things out.
 *
 * Removing an option only stops it being offered. Meals already logged hold
 * their own resolved ingredients, so nothing removed here can change what the
 * diary says you ate — which is worth saying on the screen, not just here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    state: UiState,
    category: Category,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onCreateIngredient: (String) -> Ingredient,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var facets by remember { mutableStateOf(category.facets) }
    var always by remember { mutableStateOf(category.always) }
    var addingTo by remember { mutableStateOf<Facet?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    fun edit(facet: Facet, change: (Facet) -> Facet) {
        facets = facets.map { if (it.name == facet.name) change(it) else it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.name.ifBlank { "New category" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(category.copy(name = name.trim(), always = always, facets = facets)) },
                        enabled = name.isNotBlank(),
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }

            // Shown even though you never tick it: an ingredient that appears in
            // every entry without being visible anywhere is the kind of thing
            // you'd never find when it turns out to be wrong.
            if (always.isNotEmpty()) {
                item(key = "always-head") {
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp)) {
                        Text("Always includes", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "In every ${name.lowercase().ifBlank { "one" }}, without asking.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(always, key = { "always-${it.ingredientId}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            state.nameOf(item.ingredientId),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { always = always - item }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove ${state.nameOf(item.ingredientId)}",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            facets.forEach { facet ->
                item(key = "head-${facet.name}") {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(facet.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (facet.kind == FacetKind.STYLE) {
                                    "Describes one thing — several can be true at once."
                                } else {
                                    "Adds one ingredient each."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { addingTo = facet }) { Text("Add") }
                    }

                    // What the list offers on top of the options below. A
                    // subscription is the difference between writing peanut
                    // butter into three categories and tagging it once.
                    if (facet.kind == FacetKind.ITEMS) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                "Also offers, behind \"more\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Roles.known.forEach { info ->
                                    FilterChip(
                                        selected = info.role in facet.roles,
                                        onClick = {
                                            edit(facet) {
                                                it.copy(
                                                    roles = if (info.role in it.roles) {
                                                        it.roles - info.role
                                                    } else {
                                                        it.roles + info.role
                                                    },
                                                )
                                            }
                                        },
                                        label = { Text(info.label, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }
                    }
                }

                items(facet.options, key = { "opt-${facet.name}-${it.key}" }) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.bodyMedium)

                            // What it actually does, since a label alone doesn't
                            // say whether it adds a thing or describes one.
                            val ingredientId = option.ingredientId
                            val detail = when {
                                option.modifiers.isNotEmpty() ->
                                    option.modifiers.joinToString(", ") { Attributes.label(it).lowercase() }

                                ingredientId != null -> state.nameOf(ingredientId)
                                else -> ""
                            }
                            if (detail.isNotEmpty() && !detail.equals(option.label, ignoreCase = true)) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { edit(facet) { it.copy(options = it.options - option) } }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove ${option.label}",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (facet.options.isEmpty()) {
                    item(key = "empty-${facet.name}") {
                        Text(
                            "Nothing here yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Taking an option out only stops it being offered. Meals you've already logged keep " +
                        "their own record of what you ate.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.padding(4.dp)) {
                    Text("Delete this category")
                }
            }
        }
    }

    addingTo?.let { facet ->
        AddOptionDialog(
            state = state,
            facet = facet,
            onDismiss = { addingTo = null },
            onCreateIngredient = onCreateIngredient,
            onAdd = { option ->
                edit(facet) { existing ->
                    if (existing.options.any { it.key == option.key }) existing
                    else existing.copy(options = existing.options + option)
                }
                addingTo = null
            },
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${category.name}?") },
            text = {
                Text(
                    "It stops being offered when you log a meal. Nothing already logged changes — " +
                        "entries hold their own ingredients.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete(category) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}
