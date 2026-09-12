package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.ui.theme.ColourTheme
import dev.mahourigan.fooddiary.ui.theme.TypeFace
import dev.mahourigan.fooddiary.domain.Role
import dev.mahourigan.fooddiary.domain.Roles
import dev.mahourigan.fooddiary.domain.Basis
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Measures
import dev.mahourigan.fooddiary.domain.Recipe
import dev.mahourigan.fooddiary.domain.resolve
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/**
 * Reading an older file.
 *
 * The log is evidence, so a migration that mangles it is worse than one that
 * never ships. These check the two things that would be silently destructive:
 * losing what a meal was built from, and losing an ingredient because its name
 * changed underneath its id.
 */
class MigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private suspend fun loadFrom(raw: String): DiarySnapshot {
        val file = File(folder.newFolder(), "diary.json")
        file.writeText(raw)
        return LocalDiaryRepository(file).run {
            load()
            snapshot.value
        }
    }

    @Test
    fun `a spoon conversion becomes a density the whole library can use`() = runTest {
        // 15 -> 16. The figure was already there: 14 g per tablespoon of olive
        // oil. Written as a density it is 0.93 g/ml, which also answers
        // teaspoons and cups -- and happens to be the right number for oil,
        // which is a useful sign the seeded figures were not invented.
        val v15 = """
            {
              "schemaVersion": 15,
              "ingredients": [
                {"id":"olive-oil","name":"Olive oil","defaultUnit":"tbsp","gramsPerUnit":14.0},
                {"id":"garlic","name":"Garlic","defaultUnit":"clove","gramsPerUnit":3.0}
              ],
              "recipes": [], "meals": [], "seedVersion": 4
            }
        """.trimIndent()

        val loaded = loadFrom(v15)
        val oil = loaded.ingredients.first { it.id == "olive-oil" }
        assertEquals(0.933, oil.densityGPerMl!!, 0.001)

        // A teaspoon nobody entered now answers correctly.
        assertEquals(4.67, Measures.gramsIn("tsp", oil)!!, 0.01)

        // A clove is not a volume, so garlic gets no density and no spoons.
        val garlic = loaded.ingredients.first { it.id == "garlic" }
        assertNull(garlic.densityGPerMl)
        assertNull(Measures.gramsIn("tbsp", garlic))
    }

    @Test
    fun `a v1 file's single basis becomes a list`() = runTest {
        val v1 = """
            {
              "schemaVersion": 1,
              "ingredients": [{"id":"tomato","name":"Tomato"}],
              "recipes": [],
              "meals": [
                {
                  "id": "m1",
                  "at": "2026-09-01T12:40",
                  "basis": {"recipeId":"greek","name":"Greek salad","itemsAtLogTime":[{"ingredientId":"tomato"}]},
                  "items": [{"ingredientId":"tomato"}]
                }
              ],
              "seedVersion": 1
            }
        """.trimIndent()

        val loaded = loadFrom(v1)
        val meal = loaded.meals.single()

        assertEquals(1, meal.bases.size)
        assertEquals("Greek salad", meal.bases.single().name)
        // The old field is cleared so it can't drift out of step with the list.
        assertEquals(null, meal.legacyBasis)
        assertEquals(DiarySnapshot.CURRENT_SCHEMA, loaded.schemaVersion)
    }

    @Test
    fun `a v14 diary keeps the lettering its theme used to imply`() = runTest {
        // Colour and lettering used to be one choice. Somebody on Cyrodiil had
        // never picked Petrock - the theme picked it for them - so defaulting
        // the new fields would silently move them onto Fraunces the first time
        // they opened the app. The old pairing is read back out of the theme.
        val v14 = """
            {
              "schemaVersion": 14,
              "settings": {"style":"CYRODIIL","energyUnit":"KCAL"},
              "ingredients": [],
              "recipes": [],
              "meals": [],
              "seedVersion": 4
            }
        """.trimIndent()

        val loaded = loadFrom(v14)

        assertEquals(ColourTheme.CYRODIIL, loaded.settings.colours)
        assertEquals(TypeFace.PETROCK, loaded.settings.displayFace)
        assertEquals(TypeFace.PETROCK, loaded.settings.bodyFace)
    }

    @Test
    fun `a v14 diary on the default theme keeps the default pair`() = runTest {
        val v14 = """
            {
              "schemaVersion": 14,
              "settings": {"style":"GRIMOIRE"},
              "ingredients": [],
              "recipes": [],
              "meals": [],
              "seedVersion": 4
            }
        """.trimIndent()

        val loaded = loadFrom(v14)

        // Grimoire set headings in blackletter over a quiet serif body, and
        // that pairing is now two independent settings rather than one.
        assertEquals(TypeFace.GRENZE, loaded.settings.displayFace)
        assertEquals(TypeFace.CARDO, loaded.settings.bodyFace)
    }

    @Test
    fun `a diary already at the current schema keeps the faces it recorded`() = runTest {
        // The other half of the migration: once faces are in the file they are
        // the truth, and the theme must not reach back in and overwrite them.
        val current = """
            {
              "schemaVersion": 15,
              "settings": {"style":"CYRODIIL","displayFace":"FRAUNCES","bodyFace":"ARCHIVO"},
              "ingredients": [],
              "recipes": [],
              "meals": [],
              "seedVersion": 4
            }
        """.trimIndent()

        val loaded = loadFrom(current)

        assertEquals(ColourTheme.CYRODIIL, loaded.settings.colours)
        assertEquals(TypeFace.FRAUNCES, loaded.settings.displayFace)
        assertEquals(TypeFace.ARCHIVO, loaded.settings.bodyFace)
    }

    @Test
    fun `a renamed ingredient is followed through the log`() = runTest {
        // Ids are derived from names, so renaming "Yeast extract spread" to
        // "Vegemite" would otherwise leave last week's toast pointing at nothing
        // and quietly drop its gluten from every count.
        val v3 = """
            {
              "schemaVersion": 3,
              "ingredients": [{"id":"yeast-extract-spread","name":"Yeast extract spread"}],
              "recipes": [
                {"id":"r1","name":"Toast","items":[{"ingredientId":"yeast-extract-spread"}],
                 "suggests":["yeast-extract-spread"]}
              ],
              "meals": [
                {
                  "id": "m1",
                  "at": "2026-09-01T08:00",
                  "bases": [{"recipeId":"r1","name":"Toast","itemsAtLogTime":[{"ingredientId":"yeast-extract-spread"}]}],
                  "items": [{"ingredientId":"yeast-extract-spread"}]
                }
              ],
              "seedVersion": 1,
              "retiredIngredientIds": ["yeast-extract-spread"]
            }
        """.trimIndent()

        val loaded = loadFrom(v3)
        val meal = loaded.meals.single()

        assertEquals("vegemite", meal.items.single().ingredientId)
        assertEquals("vegemite", meal.bases.single().itemsAtLogTime.single().ingredientId)

        val recipe = loaded.recipes.first { it.id == "r1" }
        assertEquals("vegemite", recipe.items.single().ingredientId)
        assertEquals(listOf("vegemite"), recipe.suggests)

        assertTrue("vegemite" in loaded.retiredIngredientIds)
        assertFalse("yeast-extract-spread" in loaded.retiredIngredientIds)

        // The name has to move too. Renaming the id alone would leave the day
        // view still saying "Yeast extract spread" indefinitely.
        assertEquals("Vegemite", loaded.ingredientsById.getValue("vegemite").name)
    }

    @Test
    fun `rapeseed oil becomes canola, and still answers to rapeseed`() = runTest {
        val v8 = """
            {
              "schemaVersion": 8,
              "ingredients": [{"id":"rapeseed-oil","name":"Rapeseed oil"}],
              "recipes": [], "seedVersion": 1,
              "meals": [
                {"id":"m1","at":"2026-09-01T19:00","items":[{"ingredientId":"rapeseed-oil"}]}
              ]
            }
        """.trimIndent()

        val loaded = loadFrom(v8)

        assertEquals("canola-oil", loaded.meals.single().items.single().ingredientId)
        val oil = loaded.ingredientsById.getValue("canola-oil")
        assertEquals("Canola oil", oil.name)
        // Searching the old word still finds it — a rename shouldn't make
        // something unfindable for anyone who learnt it the other way.
        assertTrue(oil.matches("rapeseed"))
    }

    @Test
    fun `a renamed ingredient you had edited keeps your name`() = runTest {
        val v3 = """
            {
              "schemaVersion": 3,
              "ingredients": [
                {"id":"yeast-extract-spread","name":"Vegemite, the salty one","isUserCreated":true}
              ],
              "recipes": [], "meals": [], "seedVersion": 1
            }
        """.trimIndent()

        val loaded = loadFrom(v3)
        assertEquals("Vegemite, the salty one", loaded.ingredientsById.getValue("vegemite").name)
    }

    @Test
    fun `the components categories replaced are retired, but yours are not`() = runTest {
        val v2 = """
            {
              "schemaVersion": 2,
              "ingredients": [{"id":"tomato","name":"Tomato"}],
              "recipes": [
                {"id":"starter-toast-sourdough","name":"Toast, sourdough","isUserCreated":false},
                {"id":"mine","name":"My breakfast","isUserCreated":true}
              ],
              "meals": [],
              "seedVersion": 1
            }
        """.trimIndent()

        val loaded = loadFrom(v2)
        val ids = loaded.recipes.map { it.id }

        assertFalse("starter-toast-sourdough" in ids)
        assertTrue("mine" in ids)
    }

    @Test
    fun `a category's several item lists become one, keeping what you added`() = runTest {
        val v4 = """
            {
              "schemaVersion": 4,
              "ingredients": [{"id":"butter","name":"Butter"}],
              "recipes": [], "meals": [],
              "categories": [
                {
                  "id": "cat-coffee",
                  "name": "Coffee",
                  "facets": [
                    {"name":"Coffee","kind":"STYLE","defaultIngredientId":"coffee-black",
                     "options":[{"label":"Black","ingredientId":"coffee-black"}]},
                    {"name":"Milk","kind":"ITEMS","options":[{"label":"Oat milk","ingredientId":"oat-milk"}]},
                    {"name":"Sweetened","kind":"ITEMS","options":[{"label":"Honey","ingredientId":"honey"}]}
                  ]
                }
              ],
              "seedVersion": 1, "categorySeedVersion": 1
            }
        """.trimIndent()

        val loaded = loadFrom(v4)
        val coffee = loaded.categories.first { it.id == "cat-coffee" }

        // The two item lists become one, and coffee's style question goes
        // entirely — but the coffee it defaulted to survives, now as something
        // the category simply includes rather than a tick. A Coffee category
        // that can't add any coffee would be worse than the question.
        assertEquals(listOf("Ingredients"), coffee.facets.map { it.name })
        assertEquals(listOf("coffee-black"), coffee.always.map { it.ingredientId })
        assertEquals(listOf("oat-milk", "honey"), coffee.facets.single().options.map { it.ingredientId })
    }

    @Test
    fun `what a category always includes lands in the meal without being ticked`() = runTest {
        val loaded = loadFrom("""{"schemaVersion": 8, "ingredients": [], "recipes": [], "meals": []}""")
        val coffee = loaded.categories.first { it.id == "cat-coffee" }

        assertEquals(listOf("coffee-black"), coffee.resolve(emptyMap()).map { it.ingredientId })
    }

    @Test
    fun `dairy milk comes out of the pick lists but stays in the library`() = runTest {
        val v6 = """
            {
              "schemaVersion": 6,
              "ingredients": [
                {"id":"cow-s-milk","name":"Cow's milk"},
                {"id":"oat-milk","name":"Oat milk"}
              ],
              "recipes": [], "meals": [],
              "categories": [
                {
                  "id": "cat-porridge",
                  "name": "Porridge",
                  "facets": [
                    {"name":"Ingredients","kind":"ITEMS","options":[
                      {"label":"Cow's milk","ingredientId":"cow-s-milk"},
                      {"label":"Oat milk","ingredientId":"oat-milk"}
                    ]}
                  ]
                }
              ],
              "seedVersion": 1, "categorySeedVersion": 1
            }
        """.trimIndent()

        val loaded = loadFrom(v6)
        val porridge = loaded.categories.first { it.id == "cat-porridge" }

        assertEquals(listOf("oat-milk"), porridge.facets.single().options.map { it.ingredientId })
        // Still searchable — a café coffee comes with dairy milk and that has to
        // stay loggable.
        assertTrue(loaded.ingredients.any { it.id == "cow-s-milk" })
    }

    @Test
    fun `a style facet that names something other than its category is left alone`() = runTest {
        // "Toast › Bread" is a real question and more use than "Type".
        val v5 = """
            {
              "schemaVersion": 5,
              "ingredients": [], "recipes": [], "meals": [],
              "categories": [
                {
                  "id": "cat-toast",
                  "name": "Toast",
                  "facets": [
                    {"name":"Bread","kind":"STYLE","defaultIngredientId":"wheat-bread","options":[]},
                    {"name":"Ingredients","kind":"ITEMS","options":[{"label":"Butter","ingredientId":"butter"}]}
                  ]
                }
              ],
              "seedVersion": 1, "categorySeedVersion": 1
            }
        """.trimIndent()

        val toast = loadFrom(v5).categories.first { it.id == "cat-toast" }
        assertEquals(listOf("Bread", "Ingredients"), toast.facets.map { it.name })
    }

    @Test
    fun `a bundled ingredient picks up an attribute the seed has since gained`() = runTest {
        // The seed merge only ever adds whole new ingredients, so without this
        // a library merged in before seed oils were tagged would keep counting
        // canola as nothing but fat — and the one trial you wanted to run would
        // quietly have no data behind it.
        val v10 = """
            {
              "schemaVersion": 10,
              "ingredients": [{"id":"canola-oil","name":"Canola oil","attributes":["high-fat"]}],
              "recipes": [], "meals": [], "seedVersion": 1
            }
        """.trimIndent()

        val oil = loadFrom(v10).ingredientsById.getValue("canola-oil")
        assertTrue(Attributes.SeedOil in oil.attributes)
        assertTrue(Attributes.HighFat in oil.attributes)
    }

    @Test
    fun `an ingredient you have edited keeps the attributes you gave it`() = runTest {
        // Editing marks an entry yours, which is also how taking an attribute
        // off deliberately stays off. Re-adding it on the next update would be
        // the app arguing with you about your own library.
        val v10 = """
            {
              "schemaVersion": 10,
              "ingredients": [
                {"id":"mayonnaise","name":"Mayonnaise","attributes":["egg"],"isUserCreated":true}
              ],
              "recipes": [], "meals": [], "seedVersion": 1
            }
        """.trimIndent()

        val mayo = loadFrom(v10).ingredientsById.getValue("mayonnaise")
        assertEquals(setOf(Attributes.Egg), mayo.attributes)
    }

    @Test
    fun `a bundled ingredient picks up roles, and a facet its subscriptions`() = runTest {
        // Roles arrived after the first releases, so an existing library has
        // none. Without this a category's list would silently stay as short as
        // whatever shipped, and tagging an ingredient would do nothing.
        val v11 = """
            {
              "schemaVersion": 11,
              "ingredients": [{"id":"peanut-butter","name":"Peanut butter","attributes":["peanut"]}],
              "recipes": [], "meals": [], "seedVersion": 3, "categorySeedVersion": 2,
              "categories": [
                {
                  "id": "cat-toast",
                  "name": "Toast",
                  "facets": [
                    {"name":"Ingredients","kind":"ITEMS","options":[{"label":"Butter","ingredientId":"butter"}]}
                  ]
                }
              ]
            }
        """.trimIndent()

        val loaded = loadFrom(v11)

        assertTrue(Roles.Spread in loaded.ingredientsById.getValue("peanut-butter").roles)

        val toast = loaded.categories.first { it.id == "cat-toast" }
        val ingredients = toast.facets.single()
        assertTrue(Roles.Spread in ingredients.roles)
        // The shortlist is untouched. A subscription says what else is
        // eligible; it doesn't rewrite what you chose to put on the screen.
        assertEquals(listOf("butter"), ingredients.options.map { it.ingredientId })
    }

    @Test
    fun `a category you made yourself keeps its own subscriptions`() = runTest {
        val v11 = """
            {
              "schemaVersion": 11,
              "ingredients": [], "recipes": [], "meals": [],
              "seedVersion": 3, "categorySeedVersion": 2,
              "categories": [
                {
                  "id": "cat-toast", "name": "Toast", "isUserCreated": true,
                  "facets": [{"name":"Ingredients","kind":"ITEMS","options":[]}]
                }
              ]
            }
        """.trimIndent()

        val toast = loadFrom(v11).categories.first { it.id == "cat-toast" }
        assertEquals(emptySet<Role>(), toast.facets.single().roles)
    }

    @Test
    fun `a current file is left exactly as it is`() = runTest {
        val current = DiarySnapshot(
            schemaVersion = DiarySnapshot.CURRENT_SCHEMA,
            ingredients = listOf(Ingredient(id = "vegemite", name = "Vegemite")),
            recipes = listOf(Recipe(id = "r1", name = "Toast", items = listOf(MealItem("vegemite")))),
            meals = listOf(
                MealEntry(
                    id = "m1",
                    at = LocalDateTime.of(2026, 9, 1, 8, 0),
                    bases = listOf(Basis("r1", "Toast", listOf(MealItem("vegemite")))),
                    items = listOf(MealItem("vegemite")),
                ),
            ),
            seedVersion = SeedIngredients.VERSION,
            categorySeedVersion = SeedCategories.VERSION,
        )

        val loaded = loadFrom(json.encodeToString(current))

        assertEquals(current.meals, loaded.meals)
        assertEquals(current.recipes, loaded.recipes)
    }
}
