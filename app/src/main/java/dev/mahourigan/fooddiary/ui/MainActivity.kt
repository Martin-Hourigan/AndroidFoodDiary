package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.Recipe
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.mahourigan.fooddiary.ui.theme.FoodDiaryTheme
import dev.mahourigan.fooddiary.ui.theme.LocalDiaryPalette
import dev.mahourigan.fooddiary.ui.theme.diaryPaper
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The style is stored in the diary, so the theme has to come from
            // the same view model everything else reads. Calling viewModel()
            // here and again inside the app returns the one instance.
            val viewModel: DiaryViewModel = viewModel()
            val settings by viewModel.state.collectAsState()

            val chosen = settings.snapshot.settings
            FoodDiaryTheme(
                theme = chosen.colours,
                mode = chosen.themeMode,
                displayFace = chosen.displayFace,
                bodyFace = chosen.bodyFace,
                textSize = chosen.textSize,
                custom = chosen.custom,
            ) {
                // The system draws the clock and battery over our background and
                // picks neither colour itself, so without this a light theme
                // gets white-on-white status icons.
                val view = LocalView.current
                val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = lightBackground
                            isAppearanceLightNavigationBars = lightBackground
                        }
                    }
                }

                val palette = LocalDiaryPalette.current
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .diaryPaper(MaterialTheme.colorScheme.background, palette),
                    ) {
                        FoodDiaryApp(viewModel)
                    }
                }
            }
        }
    }
}

private enum class Screen {
    TODAY, LOG_MEAL, LOG_SYMPTOM, LOG_STOOL, DAY_LOG,
    LIBRARY, CATEGORIES, CATEGORY_EDIT, RECIPES, RECIPE_EDIT,
    INGREDIENTS, INGREDIENT_EDIT, EXPORT, SETTINGS,
}

/**
 * Navigation, such as it is: one enum and an explicit way back.
 *
 * Same as the other two apps — a handful of screens with a known hierarchy
 * doesn't need a navigation library, and adding one would mean a dependency, a
 * graph, and route strings to keep in sync for no gain.
 */
@Composable
private fun FoodDiaryApp(viewModel: DiaryViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val draft by viewModel.draft.collectAsState()

    var screen by remember { mutableStateOf(Screen.TODAY) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var confirmingSave by remember { mutableStateOf(false) }

    val up: () -> Unit = {
        screen = when (screen) {
            Screen.CATEGORY_EDIT -> Screen.CATEGORIES
            Screen.RECIPE_EDIT -> Screen.RECIPES
            Screen.INGREDIENT_EDIT -> Screen.INGREDIENTS
            Screen.CATEGORIES, Screen.RECIPES, Screen.INGREDIENTS, Screen.EXPORT -> Screen.LIBRARY
            // Settings is reached from the cog on the day screen, so back from
            // it goes there rather than into a library it no longer sits in.
            else -> Screen.TODAY
        }
    }

    BackHandler(enabled = screen != Screen.TODAY) {
        // Inside a folder, back steps out of it rather than throwing the meal
        // away — you narrowed by mistake, you didn't change your mind about dinner.
        val current = draft
        val path = current?.path.orEmpty()
        when {
            confirmingSave -> confirmingSave = false
            screen == Screen.LOG_MEAL && current?.categoryId != null -> viewModel.cancelCategory()
            // Back retraces the steps you took: out of the folder, then back to
            // the meal-type question, and only then out of the meal entirely.
            // Losing a half-built dinner because you narrowed by mistake would
            // be maddening.
            screen == Screen.LOG_MEAL && path.isNotEmpty() -> viewModel.browse(path.dropLast(1))
            screen == Screen.LOG_MEAL && current?.stage == MealStage.PICKING -> viewModel.backToMealType()
            screen == Screen.LOG_MEAL -> {
                viewModel.cancelDraft()
                up()
            }
            else -> up()
        }
    }

    if (state.loading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        return
    }

    when (screen) {
        Screen.TODAY -> TodayScreen(
            state = state,
            onShowDate = viewModel::showDate,
            onLogMeal = {
                viewModel.refreshClock()
                viewModel.startMeal()
                screen = Screen.LOG_MEAL
            },
            onLogSymptom = {
                viewModel.refreshClock()
                screen = Screen.LOG_SYMPTOM
            },
            onLogStool = {
                viewModel.refreshClock()
                screen = Screen.LOG_STOOL
            },
            onEditMeal = { meal ->
                viewModel.editMeal(meal)
                screen = Screen.LOG_MEAL
            },
            onDeleteMeal = viewModel::deleteMeal,
            onStopSymptom = { viewModel.stopSymptom(it) },
            onChangeSeverity = viewModel::changeSeverity,
            onDistrustDuration = viewModel::distrustDuration,
            onDeleteSymptom = viewModel::deleteSymptom,
            onDeleteStool = viewModel::deleteStool,
            onOpenDayLog = { screen = Screen.DAY_LOG },
            onOpenLibrary = { screen = Screen.LIBRARY },
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        Screen.LOG_MEAL -> {
            val current = draft
            if (current == null) {
                screen = Screen.TODAY
            } else {
                LogMealScreen(
                    state = state,
                    draft = current,
                    onUpdate = viewModel::updateDraft,
                    onChooseMealType = viewModel::chooseMealType,
                    onBackToMealType = viewModel::backToMealType,
                    onAddRecipe = viewModel::addRecipe,
                    onRemoveBase = viewModel::removeBase,
                    onBrowse = viewModel::browse,
                    onOpenCategory = viewModel::openCategory,
                    onToggleFacet = viewModel::toggleFacetOption,
                    onAddFacetOption = viewModel::addFacetOption,
                    onCreateIngredient = viewModel::createIngredient,
                    onCommitCategory = viewModel::commitCategory,
                    onShowCategoryBuild = viewModel::showCategoryBuild,
                    onCancelCategory = viewModel::cancelCategory,
                    onAddIngredient = viewModel::addToDraft,
                    onRemoveIngredient = viewModel::removeFromDraft,
                    onSetPortion = viewModel::setPortion,
                    onSave = { confirmingSave = true },
                    onCancel = {
                        viewModel.cancelDraft()
                        screen = Screen.TODAY
                    },
                )

                // The time, notes and the library question all live on the way
                // out — building the meal should be taps only, with nothing to
                // read or type until you've finished.
                if (confirmingSave) {
                    SaveMealSheet(
                        state = state,
                        draft = current,
                        onUpdate = viewModel::updateDraft,
                        onTimeNow = viewModel::setTimeNow,
                        onTimeExact = viewModel::setTimeExact,
                        onTimeVague = viewModel::setTimeVague,
                        onSave = {
                            viewModel.saveDraft()
                            confirmingSave = false
                            screen = Screen.TODAY
                        },
                        onDismiss = { confirmingSave = false },
                        suggestedName = viewModel.suggestedName(current),
                    )
                }
            }
        }

        Screen.LOG_SYMPTOM -> LogSymptomScreen(
            state = state,
            onStartNow = { type, severity, notes ->
                viewModel.startSymptom(type, severity, notes)
                screen = Screen.TODAY
            },
            onRecord = { type, severity, startedAt, endedAt, notes ->
                viewModel.recordSymptom(type, severity, startedAt, endedAt, notes)
                screen = Screen.TODAY
            },
            onCancel = { screen = Screen.TODAY },
        )

        Screen.LOG_STOOL -> LogStoolScreen(
            state = state,
            onSave = { bristol, urgency, at, notes ->
                viewModel.logStool(bristol, urgency, at, notes)
                screen = Screen.TODAY
            },
            onCancel = { screen = Screen.TODAY },
        )

        Screen.DAY_LOG -> DayLogScreen(
            state = state,
            onSave = { day ->
                viewModel.saveDayLog(day)
                screen = Screen.TODAY
            },
            onBack = up,
        )

        Screen.LIBRARY -> LibraryScreen(
            state = state,
            onCategories = { screen = Screen.CATEGORIES },
            onRecipes = { screen = Screen.RECIPES },
            onIngredients = { screen = Screen.INGREDIENTS },
            onExport = { screen = Screen.EXPORT },
            onBack = up,
        )

        Screen.CATEGORIES -> CategoriesScreen(
            state = state,
            onEdit = { category ->
                editingCategory = category
                screen = Screen.CATEGORY_EDIT
            },
            onNew = {
                editingCategory = Category(id = UUID.randomUUID().toString())
                screen = Screen.CATEGORY_EDIT
            },
            onBack = up,
        )

        Screen.CATEGORY_EDIT -> {
            val category = editingCategory
            if (category == null) {
                screen = Screen.CATEGORIES
            } else {
                CategoryEditScreen(
                    state = state,
                    category = category,
                    onSave = {
                        viewModel.saveCategory(it)
                        editingCategory = null
                        screen = Screen.CATEGORIES
                    },
                    onDelete = {
                        viewModel.removeCategory(it)
                        editingCategory = null
                        screen = Screen.CATEGORIES
                    },
                    onCreateIngredient = viewModel::createIngredient,
                    onBack = up,
                )
            }
        }

        Screen.RECIPES -> RecipesScreen(
            state = state,
            onEdit = { recipe ->
                editingRecipe = recipe
                screen = Screen.RECIPE_EDIT
            },
            onNew = {
                editingRecipe = Recipe(id = UUID.randomUUID().toString())
                screen = Screen.RECIPE_EDIT
            },
            onToggleFavourite = { viewModel.saveRecipe(it.copy(isFavourite = !it.isFavourite)) },
            onDelete = viewModel::deleteRecipe,
            onBack = up,
        )

        Screen.RECIPE_EDIT -> {
            val recipe = editingRecipe
            if (recipe == null) {
                screen = Screen.RECIPES
            } else {
                RecipeEditScreen(
                    state = state,
                    recipe = recipe,
                    onSave = {
                        viewModel.saveRecipe(it)
                        editingRecipe = null
                        screen = Screen.RECIPES
                    },
                    onBack = up,
                )
            }
        }

        Screen.INGREDIENTS -> IngredientsScreen(
            state = state,
            onEdit = { ingredient ->
                editingIngredient = ingredient
                screen = Screen.INGREDIENT_EDIT
            },
            onNew = {
                editingIngredient = Ingredient()
                screen = Screen.INGREDIENT_EDIT
            },
            onBack = up,
        )

        Screen.INGREDIENT_EDIT -> {
            val ingredient = editingIngredient
            if (ingredient == null) {
                screen = Screen.INGREDIENTS
            } else {
                IngredientEditScreen(
                    ingredient = ingredient,
                    usage = viewModel.ingredientUsage(ingredient.id),
                    energyUnit = state.energyUnit,
                    knownTags = state.knownTags(),
                    onSave = {
                        viewModel.saveIngredient(it)
                        editingIngredient = null
                        screen = Screen.INGREDIENTS
                    },
                    onDelete = {
                        viewModel.deleteIngredient(it)
                        editingIngredient = null
                        screen = Screen.INGREDIENTS
                    },
                    onBack = up,
                )
            }
        }

        Screen.EXPORT -> ExportScreen(json = viewModel.exportJson(), onBack = up)

        Screen.SETTINGS -> SettingsScreen(
            state = state,
            onSetEnergyUnit = viewModel::setEnergyUnit,
            onSetColours = viewModel::setColours,
            onSetThemeMode = viewModel::setThemeMode,
            onSetTextSize = viewModel::setTextSize,
            onSetDisplayFace = viewModel::setDisplayFace,
            onSetBodyFace = viewModel::setBodyFace,
            onSetCustomPalette = viewModel::setCustomPalette,
            onBack = up,
        )
    }
}

/**
 * The diary as text.
 *
 * The same JSON the app runs on, not a rendered report — so a backup is exactly
 * what the store holds, and you can read it yourself. A dietitian-facing summary
 * is a later job and a different shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreen(json: String, onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Food diary", json))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy to clipboard") }

            Text(
                "${json.length / 1024} KB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Text(
                json.take(4000),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        }
    }
}
