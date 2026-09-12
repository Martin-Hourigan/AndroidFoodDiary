package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Attribute
import dev.mahourigan.fooddiary.domain.Attributes.Alcohol
import dev.mahourigan.fooddiary.domain.Attributes.Allium
import dev.mahourigan.fooddiary.domain.Attributes.Barley
import dev.mahourigan.fooddiary.domain.Attributes.Caffeine
import dev.mahourigan.fooddiary.domain.Attributes.Capsaicin
import dev.mahourigan.fooddiary.domain.Attributes.Carbonation
import dev.mahourigan.fooddiary.domain.Attributes.Casein
import dev.mahourigan.fooddiary.domain.Attributes.Citrus
import dev.mahourigan.fooddiary.domain.Attributes.Dairy
import dev.mahourigan.fooddiary.domain.Attributes.Egg
import dev.mahourigan.fooddiary.domain.Attributes.ExcessFructose
import dev.mahourigan.fooddiary.domain.Attributes.Fermented
import dev.mahourigan.fooddiary.domain.Attributes.Fructan
import dev.mahourigan.fooddiary.domain.Attributes.Gluten
import dev.mahourigan.fooddiary.domain.Attributes.Gos
import dev.mahourigan.fooddiary.domain.Attributes.HighFat
import dev.mahourigan.fooddiary.domain.Attributes.Histamine
import dev.mahourigan.fooddiary.domain.Attributes.Lactose
import dev.mahourigan.fooddiary.domain.Attributes.Mannitol
import dev.mahourigan.fooddiary.domain.Attributes.Nightshade
import dev.mahourigan.fooddiary.domain.Attributes.Oats
import dev.mahourigan.fooddiary.domain.Attributes.Peanut
import dev.mahourigan.fooddiary.domain.Attributes.Rye
import dev.mahourigan.fooddiary.domain.Attributes.SeedOil
import dev.mahourigan.fooddiary.domain.Attributes.Sesame
import dev.mahourigan.fooddiary.domain.Attributes.Sorbitol
import dev.mahourigan.fooddiary.domain.Attributes.Soy
import dev.mahourigan.fooddiary.domain.Attributes.Sulphite
import dev.mahourigan.fooddiary.domain.Attributes.Sweetener
import dev.mahourigan.fooddiary.domain.Attributes.TreeNut
import dev.mahourigan.fooddiary.domain.Attributes.Wheat
import dev.mahourigan.fooddiary.domain.Ingredient
import dev.mahourigan.fooddiary.domain.Measures
import dev.mahourigan.fooddiary.domain.Role
import dev.mahourigan.fooddiary.domain.Roles

/**
 * The bundled ingredient library.
 *
 * An empty library makes the app useless on day one, so it ships with the food
 * an ordinary vegetarian kitchen actually contains, already tagged.
 *
 * **Vegetarian.** No meat, poultry, fish or shellfish; dairy and eggs are in. A
 * trigger list you have to scroll past forty cuts of meat to use is a trigger
 * list you stop using.
 *
 * Two honest limits, surfaced in the app as well as here:
 *
 * 1. The FODMAP tags are a coarse "high / not high" judgement, not Monash
 *    figures. They're a starting point for what to suspect, not a lookup table.
 * 2. Tags describe an ingredient generically, and specific products differ —
 *    soy sauce is usually wheat but tamari isn't, one oat milk has added inulin
 *    and another doesn't. Everything here is editable, and an edit sticks.
 *
 * Where two forms of a food behave differently they are separate entries rather
 * than one averaged one. That's the whole reason parmesan and ricotta aren't
 * both just "cheese": collapsing them would bury the clearest dairy signal
 * available to a person who eats cheese daily.
 */
object SeedIngredients {

    /** Bumped when entries are added; merged in without touching your edits. */
    const val VERSION = 4

    // A set rather than a vararg: Attribute is an inline value class, and Kotlin
    // won't take those as varargs. setOf(...) at each call site is the cost.
    private fun ing(
        name: String,
        attributes: Set<Attribute> = emptySet(),
        unit: String? = null,
        typical: Double? = null,
        aliases: List<String> = emptyList(),
        roles: Set<Role> = emptySet(),
        /**
         * Grams per millilitre, for anything spooned or poured whose suggested
         * unit is *not* a volume.
         *
         * Anything already measured in spoons or cups has its density worked
         * out below from the figure in [SeedEnergy], so this only needs filling
         * in for the awkward ones: a powder best suggested in grams, which the
         * app should still be able to offer a tablespoon of.
         */
        density: Double? = null,
    ): Ingredient {
        val energy = SeedEnergy.values[slug(name)]
        val gramsPerUnit = energy?.second
        return Ingredient(
            id = slug(name),
            name = name,
            attributes = attributes,
            roles = roles,
            aliases = aliases,
            defaultUnit = unit,
            typicalAmount = typical,
            // Kept in its own file so the tagging stays readable here. A test
            // asserts every ingredient is covered, so the two can't drift apart.
            kcalPer100 = energy?.first,
            gramsPerUnit = gramsPerUnit,
            // A unit that is already a volume carries its own density: 5 g per
            // tablespoon is 0.33 g/ml, and that answers teaspoons and cups too.
            // Derived rather than typed out so the two figures cannot disagree.
            densityGPerMl = density ?: Measures.millilitresIn(unit.orEmpty())
                ?.takeIf { it > 0 && (gramsPerUnit ?: 0.0) > 0 }
                ?.let { gramsPerUnit!! / it },
            isUserCreated = false,
        )
    }

    fun slug(name: String): String = name.lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .split('-').filter { it.isNotEmpty() }.joinToString("-")

    val all: List<Ingredient> = buildList {

        // ---- Vegetables: alliums ---------------------------------------------
        // The whole family, separately. Onion and garlic are the most common
        // triggers there are, and "I only had a bit of garlic" is exactly the
        // distinction the portion field exists for.
        add(ing("Onion", setOf(Allium, Fructan), unit = "whole", typical = 0.5, roles = setOf(Roles.Vegetable)))
        add(ing("Red onion", setOf(Allium, Fructan), unit = "whole", typical = 0.5, roles = setOf(Roles.Vegetable)))
        add(ing("Spring onion, white part", setOf(Allium, Fructan), unit = "stalk", typical = 2.0, aliases = listOf("scallion"), roles = setOf(Roles.Vegetable)))
        add(ing("Spring onion, green tops", setOf(Allium), unit = "stalk", typical = 2.0, aliases = listOf("scallion"), roles = setOf(Roles.Vegetable)))
        add(ing("Shallot", setOf(Allium, Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Leek, white part", setOf(Allium, Fructan), unit = "g", typical = 60.0, roles = setOf(Roles.Vegetable)))
        add(ing("Leek, green part", setOf(Allium), unit = "g", typical = 60.0, roles = setOf(Roles.Vegetable)))
        add(ing("Garlic", setOf(Allium, Fructan), unit = "clove", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Garlic powder", setOf(Allium, Fructan), unit = "tsp", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Onion powder", setOf(Allium, Fructan), unit = "tsp", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Chives", setOf(Allium), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Garlic-infused oil", setOf(HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))

        // ---- Vegetables: the rest --------------------------------------------
        add(ing("Tomato", setOf(Nightshade, Histamine), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Cherry tomatoes", setOf(Nightshade, Histamine), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Tinned tomatoes", setOf(Nightshade, Histamine), unit = "g", typical = 200.0, roles = setOf(Roles.Vegetable)))
        add(ing("Tomato paste", setOf(Nightshade, Histamine), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Potato", setOf(Nightshade), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Sweet potato", setOf(Mannitol), unit = "g", typical = 150.0, aliases = listOf("kumara"), roles = setOf(Roles.Vegetable)))
        add(ing("Eggplant", setOf(Nightshade, Histamine), unit = "g", typical = 100.0, aliases = listOf("aubergine"), roles = setOf(Roles.Vegetable)))
        add(ing("Red capsicum", setOf(Nightshade), unit = "whole", typical = 0.5, aliases = listOf("red pepper", "bell pepper"), roles = setOf(Roles.Vegetable)))
        add(ing("Green capsicum", setOf(Nightshade), unit = "whole", typical = 0.5, aliases = listOf("green pepper", "bell pepper"), roles = setOf(Roles.Vegetable)))
        add(ing("Chilli, fresh", setOf(Nightshade, Capsaicin), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Cucumber", unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Lettuce", unit = "g", typical = 50.0, roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Rocket", unit = "g", typical = 30.0, aliases = listOf("arugula"), roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Spinach", setOf(Histamine), unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Kale", unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Silverbeet", unit = "g", typical = 75.0, aliases = listOf("chard", "swiss chard"), roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Cabbage, green", unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable)))
        add(ing("Cabbage, savoy", setOf(Fructan), unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable)))
        add(ing("Red cabbage", unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable)))
        add(ing("Broccoli", setOf(Fructan), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Cauliflower", setOf(Mannitol), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Brussels sprouts", setOf(Fructan), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Carrot", unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Beetroot", setOf(Fructan), unit = "g", typical = 80.0, aliases = listOf("beet"), roles = setOf(Roles.Vegetable)))
        add(ing("Parsnip", unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Swede", setOf(Fructan), unit = "g", typical = 80.0, aliases = listOf("rutabaga"), roles = setOf(Roles.Vegetable)))
        add(ing("Turnip", unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Radish", unit = "g", typical = 40.0, roles = setOf(Roles.Vegetable)))
        add(ing("Celery", setOf(Mannitol), unit = "stick", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Celeriac", unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Fennel", setOf(Fructan), unit = "g", typical = 60.0, roles = setOf(Roles.Vegetable)))
        add(ing("Asparagus", setOf(Fructan, ExcessFructose), unit = "spear", typical = 5.0, roles = setOf(Roles.Vegetable)))
        add(ing("Globe artichoke", setOf(Fructan), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Jerusalem artichoke", setOf(Fructan), unit = "g", typical = 60.0, roles = setOf(Roles.Vegetable)))
        add(ing("Zucchini", unit = "g", typical = 100.0, aliases = listOf("courgette"), roles = setOf(Roles.Vegetable)))
        add(ing("Pumpkin", setOf(Mannitol), unit = "g", typical = 100.0, roles = setOf(Roles.Vegetable)))
        add(ing("Butternut squash", setOf(Mannitol), unit = "g", typical = 100.0, roles = setOf(Roles.Vegetable)))
        add(ing("Green beans", unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Peas", setOf(Fructan, Gos), unit = "g", typical = 80.0, roles = setOf(Roles.Vegetable)))
        add(ing("Snow peas", setOf(Mannitol), unit = "g", typical = 60.0, aliases = listOf("mangetout"), roles = setOf(Roles.Vegetable)))
        add(ing("Sugar snap peas", setOf(ExcessFructose), unit = "g", typical = 60.0, roles = setOf(Roles.Vegetable)))
        add(ing("Sweetcorn", setOf(Sorbitol), unit = "cob", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Mushrooms, button", setOf(Mannitol), unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable)))
        add(ing("Mushrooms, portobello", setOf(Mannitol), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Mushrooms, oyster", unit = "g", typical = 75.0, roles = setOf(Roles.Vegetable)))
        add(ing("Avocado", setOf(Sorbitol, HighFat, Histamine), unit = "whole", typical = 0.5, roles = setOf(Roles.Vegetable, Roles.Spread)))
        add(ing("Olives", setOf(HighFat, Histamine, Sulphite), unit = "g", typical = 30.0, roles = setOf(Roles.Vegetable)))
        add(ing("Sauerkraut", setOf(Fermented, Histamine), unit = "g", typical = 40.0, roles = setOf(Roles.Vegetable)))
        add(ing("Kimchi", setOf(Fermented, Histamine, Capsaicin, Allium), unit = "g", typical = 40.0, roles = setOf(Roles.Vegetable)))
        add(ing("Pickled gherkins", setOf(Fermented, Histamine, Sulphite), unit = "whole", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Seaweed / nori", unit = "sheet", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Bean sprouts", unit = "g", typical = 50.0, roles = setOf(Roles.Vegetable)))
        add(ing("Bok choy", unit = "g", typical = 75.0, aliases = listOf("pak choi"), roles = setOf(Roles.Vegetable, Roles.SaladLeaf)))
        add(ing("Ginger", unit = "tsp", typical = 1.0, roles = setOf(Roles.Vegetable)))
        add(ing("Horseradish", setOf(Capsaicin), unit = "tsp", typical = 1.0, roles = setOf(Roles.Vegetable)))

        // ---- Fruit ------------------------------------------------------------
        // The high-fructose and polyol ones are the point here, but the low ones
        // matter just as much: without berries and citrus in the library there
        // is nothing for "apple" to be contrasted against.
        add(ing("Apple", setOf(ExcessFructose, Sorbitol, Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Pear", setOf(ExcessFructose, Sorbitol), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Banana, ripe", setOf(Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Banana, firm", unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Orange", setOf(Citrus), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Mandarin", setOf(Citrus), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Lemon", setOf(Citrus), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Lime", setOf(Citrus), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Grapefruit", setOf(Citrus), unit = "whole", typical = 0.5, roles = setOf(Roles.Fruit)))
        add(ing("Strawberries", setOf(Histamine), unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Blueberries", unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Raspberries", unit = "g", typical = 60.0, roles = setOf(Roles.Fruit)))
        add(ing("Blackberries", setOf(Sorbitol), unit = "g", typical = 60.0, roles = setOf(Roles.Fruit)))
        add(ing("Grapes", unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Kiwifruit", unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Pineapple", setOf(Histamine), unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Mango", setOf(ExcessFructose), unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Watermelon", setOf(ExcessFructose, Fructan, Mannitol), unit = "g", typical = 150.0, roles = setOf(Roles.Fruit)))
        add(ing("Rockmelon", unit = "g", typical = 120.0, aliases = listOf("cantaloupe"), roles = setOf(Roles.Fruit)))
        add(ing("Peach", setOf(Sorbitol, Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Nectarine", setOf(Sorbitol, Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Apricot", setOf(Sorbitol), unit = "whole", typical = 2.0, roles = setOf(Roles.Fruit)))
        add(ing("Plum", setOf(Sorbitol), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Cherries", setOf(ExcessFructose, Sorbitol), unit = "g", typical = 80.0, roles = setOf(Roles.Fruit)))
        add(ing("Fig, fresh", setOf(ExcessFructose), unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Papaya", unit = "g", typical = 100.0, roles = setOf(Roles.Fruit)))
        add(ing("Passionfruit", unit = "whole", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Pomegranate", unit = "tbsp", typical = 2.0, roles = setOf(Roles.Fruit)))
        add(ing("Rhubarb", unit = "g", typical = 80.0, roles = setOf(Roles.Fruit)))
        add(ing("Dates", setOf(ExcessFructose, Fructan), unit = "whole", typical = 2.0, roles = setOf(Roles.Fruit)))
        add(ing("Raisins / sultanas", setOf(Fructan, ExcessFructose, Sulphite), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Fruit)))
        add(ing("Dried apricots", setOf(Sorbitol, Sulphite), unit = "whole", typical = 3.0, roles = setOf(Roles.Fruit)))
        add(ing("Prunes", setOf(Sorbitol, Sulphite), unit = "whole", typical = 3.0, roles = setOf(Roles.Fruit)))
        add(ing("Dried figs", setOf(Fructan, ExcessFructose), unit = "whole", typical = 2.0, roles = setOf(Roles.Fruit)))
        add(ing("Dried cranberries", setOf(ExcessFructose, Sulphite), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Fruit)))

        // ---- Cheeses ----------------------------------------------------------
        // Named individually, because lactose falls away as cheese ages and these
        // are not interchangeable. Hard aged: effectively none. Soft and fresh: a
        // lot. If dairy is a suspect, this distinction is the most informative
        // thing in the whole library.
        add(ing("Parmesan", setOf(Dairy, Casein, Histamine), unit = "tbsp", typical = 1.0, aliases = listOf("parmigiano"), roles = setOf(Roles.Cheese)))
        add(ing("Pecorino", setOf(Dairy, Casein, Histamine), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Cheese)))
        add(ing("Cheddar, mature", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Cheddar, mild", setOf(Dairy, Casein, HighFat), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Gruyère", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Manchego", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Gouda", setOf(Dairy, Casein, HighFat), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Edam", setOf(Dairy, Casein), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Brie", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Camembert", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Blue cheese", setOf(Dairy, Casein, HighFat, Histamine), unit = "g", typical = 25.0, roles = setOf(Roles.Cheese)))
        add(ing("Feta", setOf(Dairy, Casein, Lactose), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Halloumi", setOf(Dairy, Casein, Lactose, HighFat), unit = "g", typical = 50.0, roles = setOf(Roles.Cheese)))
        add(ing("Mozzarella", setOf(Dairy, Casein, Lactose), unit = "g", typical = 50.0, roles = setOf(Roles.Cheese)))
        add(ing("Bocconcini", setOf(Dairy, Casein, Lactose), unit = "g", typical = 50.0, roles = setOf(Roles.Cheese)))
        add(ing("Ricotta", setOf(Dairy, Casein, Lactose), unit = "g", typical = 60.0, roles = setOf(Roles.Cheese)))
        add(ing("Cottage cheese", setOf(Dairy, Casein, Lactose), unit = "g", typical = 80.0, roles = setOf(Roles.Cheese)))
        add(ing("Cream cheese", setOf(Dairy, Casein, Lactose, HighFat), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Cheese, Roles.Spread)))
        add(ing("Mascarpone", setOf(Dairy, Casein, Lactose, HighFat), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Cheese)))
        add(ing("Goat's cheese", setOf(Dairy, Casein, Lactose), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Paneer", setOf(Dairy, Casein, Lactose), unit = "g", typical = 60.0, roles = setOf(Roles.Cheese)))
        add(ing("Vegetarian hard cheese", setOf(Dairy, Casein, Histamine), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))
        add(ing("Vegan cheese", setOf(HighFat), unit = "g", typical = 30.0, roles = setOf(Roles.Cheese)))

        // ---- Eggs -------------------------------------------------------------
        // Split, because the white is the usual culprit and a whole-egg-only
        // library can never show that.
        add(ing("Egg, whole", setOf(Egg), unit = "egg", typical = 2.0, roles = setOf(Roles.Egg)))
        add(ing("Egg white", setOf(Egg), unit = "egg", typical = 2.0, roles = setOf(Roles.Egg)))
        add(ing("Egg yolk", setOf(Egg, HighFat), unit = "egg", typical = 2.0, roles = setOf(Roles.Egg)))

        // ---- Milks: dairy and alternative -------------------------------------
        // Alternative milks get real attention because swapping between them is
        // invisible in an ordinary diary and they are not equivalent. Going
        // dairy-free and feeling worse is a very common outcome, and oat milk is
        // usually why.
        add(ing("Cow's milk", setOf(Dairy, Casein, Lactose), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Cow's milk, lactose-free", setOf(Dairy, Casein), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Goat's milk", setOf(Dairy, Casein, Lactose), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Oat milk", setOf(Fructan), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Soy milk, whole bean", setOf(Soy, Gos), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Soy milk, protein isolate", setOf(Soy), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Almond milk", setOf(TreeNut), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Cashew milk", setOf(TreeNut, Gos), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Macadamia milk", setOf(TreeNut), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Coconut milk drink", setOf(HighFat), unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Rice milk", unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Hemp milk", unit = "ml", typical = 200.0, roles = setOf(Roles.Milk)))
        add(ing("Chicory root / inulin added", setOf(Fructan), unit = "serving", typical = 1.0, aliases = listOf("inulin", "added fibre"), roles = setOf(Roles.Milk)))

        // ---- Other dairy ------------------------------------------------------
        add(ing("Yoghurt, natural", setOf(Dairy, Casein, Lactose, Fermented), unit = "g", typical = 150.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Yoghurt, Greek", setOf(Dairy, Casein, Lactose, Fermented, HighFat), unit = "g", typical = 150.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Yoghurt, lactose-free", setOf(Dairy, Casein, Fermented), unit = "g", typical = 150.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Yoghurt, coconut", setOf(HighFat, Fermented), unit = "g", typical = 150.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Yoghurt, soy", setOf(Soy, Fermented), unit = "g", typical = 150.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Kefir", setOf(Dairy, Casein, Lactose, Fermented, Histamine), unit = "ml", typical = 200.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Cream", setOf(Dairy, Casein, Lactose, HighFat), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Sour cream", setOf(Dairy, Casein, Lactose, HighFat, Fermented), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Crème fraîche", setOf(Dairy, Casein, Lactose, HighFat), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Yoghurt)))
        add(ing("Butter", setOf(Dairy, HighFat), unit = "tsp", typical = 2.0, roles = setOf(Roles.Oil, Roles.Spread)))
        add(ing("Ghee", setOf(HighFat), unit = "tsp", typical = 2.0, roles = setOf(Roles.Oil)))
        add(ing("Ice cream", setOf(Dairy, Casein, Lactose, HighFat), unit = "scoop", typical = 2.0))
        add(ing("Condensed milk", setOf(Dairy, Casein, Lactose), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Custard", setOf(Dairy, Casein, Lactose, Egg), unit = "g", typical = 100.0))

        // ---- Grains, breads and flours ----------------------------------------
        // Wheat carries gluten *and* fructans, which is the single most important
        // fact in this library: cutting gluten cuts both, so gluten-free grains
        // are here in force to give the analysis something to separate them with.
        add(ing("Wheat bread, white", setOf(Gluten, Wheat, Fructan), unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Wheat bread, wholemeal", setOf(Gluten, Wheat, Fructan), unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Sourdough, wheat", setOf(Gluten, Wheat), unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Sourdough, spelt", setOf(Gluten, Wheat), unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Rye bread", setOf(Gluten, Rye, Fructan), unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Gluten-free bread", unit = "slice", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Pita bread", setOf(Gluten, Wheat, Fructan), unit = "whole", typical = 1.0, roles = setOf(Roles.Grain)))
        add(ing("Tortilla, wheat", setOf(Gluten, Wheat, Fructan), unit = "whole", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Tortilla, corn", unit = "whole", typical = 2.0, roles = setOf(Roles.Grain)))
        add(ing("Naan", setOf(Gluten, Wheat, Fructan, Dairy), unit = "whole", typical = 1.0, roles = setOf(Roles.Grain)))
        add(ing("Croissant", setOf(Gluten, Wheat, Fructan, Dairy, HighFat), unit = "whole", typical = 1.0, roles = setOf(Roles.Grain)))
        add(ing("Pastry", setOf(Gluten, Wheat, Dairy, HighFat), unit = "serving", typical = 1.0, roles = setOf(Roles.Grain)))
        add(ing("Crackers, wheat", setOf(Gluten, Wheat, Fructan), unit = "cracker", typical = 4.0, roles = setOf(Roles.Grain)))
        add(ing("Rice crackers", unit = "cracker", typical = 4.0, roles = setOf(Roles.Grain)))
        add(ing("Pasta, wheat", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Pasta, gluten-free", unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Couscous", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Semolina", setOf(Gluten, Wheat), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Bulgur wheat", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Farro", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Seitan", setOf(Gluten, Wheat), unit = "g", typical = 100.0, aliases = listOf("wheat gluten", "mock duck"), roles = setOf(Roles.Grain)))
        add(ing("Barley", setOf(Gluten, Barley, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Oats, rolled", setOf(Oats, Gluten, Fructan), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Oats, gluten-free", setOf(Oats, Fructan), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Rice, white", unit = "g", typical = 150.0, roles = setOf(Roles.Grain)))
        add(ing("Rice, brown", unit = "g", typical = 150.0, roles = setOf(Roles.Grain)))
        add(ing("Rice, basmati", unit = "g", typical = 150.0, roles = setOf(Roles.Grain)))
        add(ing("Quinoa", unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Buckwheat", unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Millet", unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Polenta", unit = "g", typical = 100.0, aliases = listOf("cornmeal"), roles = setOf(Roles.Grain)))
        add(ing("Rice noodles", unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Soba noodles", setOf(Gluten, Wheat), unit = "g", typical = 100.0, roles = setOf(Roles.Grain)))
        add(ing("Wheat flour", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Spelt flour", setOf(Gluten, Wheat), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Rice flour", unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Chickpea flour", setOf(Gos), unit = "g", typical = 50.0, aliases = listOf("besan", "gram flour"), roles = setOf(Roles.Grain)))
        add(ing("Almond flour", setOf(TreeNut, HighFat), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Cornflour", unit = "tbsp", typical = 1.0, aliases = listOf("cornstarch"), roles = setOf(Roles.Grain)))
        add(ing("Breakfast cereal, wheat", setOf(Gluten, Wheat, Fructan), unit = "g", typical = 40.0, roles = setOf(Roles.Grain)))
        add(ing("Muesli", setOf(Oats, Gluten, Fructan, TreeNut), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))
        add(ing("Granola", setOf(Oats, Gluten, Fructan, TreeNut, HighFat), unit = "g", typical = 50.0, roles = setOf(Roles.Grain)))

        // ---- Legumes and pulses -----------------------------------------------
        // Tinned and dried are separate entries: draining and rinsing tinned
        // pulses genuinely lowers the GOS load, so treating them as one food
        // would blur a difference you can actually act on.
        add(ing("Chickpeas, tinned and rinsed", setOf(Gos), unit = "g", typical = 100.0, aliases = listOf("garbanzo"), roles = setOf(Roles.Legume)))
        add(ing("Chickpeas, dried and cooked", setOf(Gos), unit = "g", typical = 100.0, aliases = listOf("garbanzo"), roles = setOf(Roles.Legume)))
        add(ing("Lentils, red", setOf(Gos), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Lentils, brown", setOf(Gos), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Lentils, puy", setOf(Gos), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Lentils, tinned and rinsed", setOf(Gos), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Kidney beans", setOf(Gos, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Black beans", setOf(Gos, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Cannellini beans", setOf(Gos, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Borlotti beans", setOf(Gos, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Butter beans", setOf(Gos, Fructan), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        // Measured in tins, because that is the number on the shelf. The tin is
        // 400 g gross and drains to about 240 g, so gramsPerUnit carries the
        // difference and you never have to do that sum yourself.
        add(ing("Butter beans, tinned and rinsed", setOf(Gos, Fructan), unit = "tin", typical = 0.5, aliases = listOf("lima beans", "tin of butter beans"), roles = setOf(Roles.Legume)))
        add(ing("Baked beans", setOf(Gos, Fructan, ExcessFructose), unit = "g", typical = 150.0, roles = setOf(Roles.Legume)))
        add(ing("Broad beans", setOf(Gos), unit = "g", typical = 80.0, aliases = listOf("fava"), roles = setOf(Roles.Legume)))
        add(ing("Split peas", setOf(Gos), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Tofu, firm", setOf(Soy), unit = "g", typical = 150.0, roles = setOf(Roles.Legume)))
        add(ing("Tofu, silken", setOf(Soy, Gos), unit = "g", typical = 150.0, roles = setOf(Roles.Legume)))
        add(ing("Tempeh", setOf(Soy, Fermented), unit = "g", typical = 100.0, roles = setOf(Roles.Legume)))
        add(ing("Edamame", setOf(Soy, Gos), unit = "g", typical = 80.0, roles = setOf(Roles.Legume)))
        add(ing("Textured vegetable protein", setOf(Soy, Gos), unit = "g", typical = 60.0, aliases = listOf("tvp", "mince substitute"), roles = setOf(Roles.Legume)))
        add(ing("Falafel", setOf(Gos, HighFat, Allium, Fructan), unit = "ball", typical = 4.0, roles = setOf(Roles.Legume)))
        add(ing("Hummus", setOf(Gos, Sesame, Allium, HighFat), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Legume, Roles.Spread)))
        add(ing("Peanut butter", setOf(Peanut, HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Legume, Roles.Spread)))
        add(ing("Veggie burger patty", setOf(Soy, Gluten, Wheat, Gos), unit = "patty", typical = 1.0, roles = setOf(Roles.Legume)))
        add(ing("Veggie sausage", setOf(Soy, Gluten, Wheat), unit = "sausage", typical = 2.0, roles = setOf(Roles.Legume)))

        // ---- Nuts and seeds ---------------------------------------------------
        add(ing("Almonds", setOf(TreeNut, HighFat), unit = "nut", typical = 10.0, roles = setOf(Roles.NutSeed)))
        add(ing("Cashews", setOf(TreeNut, Gos, Fructan, HighFat), unit = "nut", typical = 10.0, roles = setOf(Roles.NutSeed)))
        add(ing("Pistachios", setOf(TreeNut, Gos, Fructan, HighFat), unit = "nut", typical = 15.0, roles = setOf(Roles.NutSeed)))
        add(ing("Walnuts", setOf(TreeNut, HighFat), unit = "half", typical = 6.0, roles = setOf(Roles.NutSeed)))
        add(ing("Pecans", setOf(TreeNut, HighFat), unit = "half", typical = 6.0, roles = setOf(Roles.NutSeed)))
        add(ing("Hazelnuts", setOf(TreeNut, HighFat), unit = "nut", typical = 10.0, roles = setOf(Roles.NutSeed)))
        add(ing("Macadamias", setOf(TreeNut, HighFat), unit = "nut", typical = 8.0, roles = setOf(Roles.NutSeed)))
        add(ing("Brazil nuts", setOf(TreeNut, HighFat), unit = "nut", typical = 3.0, roles = setOf(Roles.NutSeed)))
        add(ing("Pine nuts", setOf(TreeNut, HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.NutSeed)))
        add(ing("Peanuts", setOf(Peanut, HighFat), unit = "g", typical = 30.0, roles = setOf(Roles.NutSeed)))
        add(ing("Chestnuts", setOf(TreeNut), unit = "g", typical = 40.0, roles = setOf(Roles.NutSeed)))
        add(ing("Pumpkin seeds", setOf(HighFat), unit = "tbsp", typical = 1.0, aliases = listOf("pepitas"), roles = setOf(Roles.NutSeed)))
        add(ing("Sunflower seeds", setOf(HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.NutSeed)))
        add(ing("Sesame seeds", setOf(Sesame), unit = "tbsp", typical = 1.0, roles = setOf(Roles.NutSeed)))
        add(ing("Tahini", setOf(Sesame, HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.NutSeed, Roles.Spread)))
        add(ing("Chia seeds", unit = "tbsp", typical = 1.0, roles = setOf(Roles.NutSeed)))
        add(ing("Flaxseed", unit = "tbsp", typical = 1.0, aliases = listOf("linseed"), roles = setOf(Roles.NutSeed)))
        add(ing("Psyllium husk", unit = "tsp", typical = 1.0, roles = setOf(Roles.NutSeed)))
        add(ing("Coconut, desiccated", setOf(HighFat, Sorbitol), unit = "tbsp", typical = 2.0, roles = setOf(Roles.NutSeed)))
        add(ing("Coconut cream", setOf(HighFat, Sorbitol), unit = "ml", typical = 100.0, roles = setOf(Roles.NutSeed, Roles.Yoghurt)))

        // ---- Fats and oils ----------------------------------------------------
        add(ing("Olive oil", setOf(HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Canola oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, aliases = listOf("rapeseed", "rapeseed oil"), roles = setOf(Roles.Oil)))
        add(ing("Sunflower oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Vegetable oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, aliases = listOf("blended oil", "frying oil"), roles = setOf(Roles.Oil)))
        add(ing("Soybean oil", setOf(HighFat, SeedOil, Soy), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Corn oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Grapeseed oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Rice bran oil", setOf(HighFat, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Coconut oil", setOf(HighFat), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Sesame oil", setOf(Sesame, HighFat, SeedOil), unit = "tsp", typical = 1.0, roles = setOf(Roles.Oil)))
        add(ing("Margarine", setOf(HighFat, SeedOil), unit = "tsp", typical = 2.0, roles = setOf(Roles.Oil, Roles.Spread)))
        add(ing("Mayonnaise", setOf(Egg, HighFat, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil, Roles.Spread, Roles.Sauce)))
        add(ing("Vegan mayonnaise", setOf(HighFat, Soy, SeedOil), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Oil, Roles.Spread, Roles.Sauce)))

        // ---- Store cupboard ---------------------------------------------------
        // The hidden-gluten and hidden-onion shelf. These are the entries most
        // worth having pre-tagged, because nobody thinks of a stock cube as a
        // wheat-and-onion product and it is very often both.
        add(ing("Soy sauce", setOf(Soy, Gluten, Wheat, Fermented), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Tamari", setOf(Soy, Fermented), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Miso paste", setOf(Soy, Fermented, Histamine), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Stock cube", setOf(Allium, Fructan, Gluten, Wheat), unit = "cube", typical = 1.0, aliases = listOf("bouillon"), roles = setOf(Roles.Sauce)))
        add(ing("Stock, low-FODMAP", unit = "ml", typical = 250.0, roles = setOf(Roles.Sauce)))
        add(ing("Vegemite", setOf(Barley, Gluten), unit = "tsp", typical = 1.0, aliases = listOf("marmite", "promite", "yeast extract", "yeast extract spread"), roles = setOf(Roles.Spread)))
        add(ing("Nutritional yeast", unit = "g", typical = 15.0, density = 5.0 / 15.0, aliases = listOf("nooch", "savoury yeast flakes"), roles = setOf(Roles.Sauce)))
        add(ing("Tomato ketchup", setOf(Nightshade, ExcessFructose), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Mustard", unit = "tsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Vinegar, balsamic", setOf(Fermented, Histamine, Sulphite), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Vinegar, white wine", setOf(Fermented, Histamine, Sulphite), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Vinegar, apple cider", setOf(Fermented, Histamine), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Hot sauce", setOf(Capsaicin, Nightshade, Fermented), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Sriracha", setOf(Capsaicin, Nightshade, Allium, Fructan), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Curry paste", setOf(Capsaicin, Allium, Fructan), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Pesto", setOf(TreeNut, Dairy, HighFat, Allium), unit = "tbsp", typical = 2.0, roles = setOf(Roles.Sauce)))
        add(ing("Pasta sauce, jarred", setOf(Nightshade, Allium, Fructan, Histamine), unit = "g", typical = 150.0, roles = setOf(Roles.Sauce)))
        add(ing("Coconut aminos", unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sauce)))
        add(ing("Honey", setOf(ExcessFructose), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener, Roles.Spread)))
        add(ing("Maple syrup", unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Agave syrup", setOf(ExcessFructose), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Sugar", unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("High-fructose corn syrup", setOf(ExcessFructose), unit = "tbsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Xylitol", setOf(Sweetener), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Sorbitol / maltitol", setOf(Sweetener, Sorbitol), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Erythritol", setOf(Sweetener), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Stevia", setOf(Sweetener), unit = "tsp", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Aspartame", setOf(Sweetener), unit = "serving", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Sucralose", setOf(Sweetener), unit = "serving", typical = 1.0, roles = setOf(Roles.Sweetener)))
        add(ing("Sugar-free gum", setOf(Sweetener, Sorbitol), unit = "piece", typical = 2.0))
        add(ing("Dark chocolate", setOf(Caffeine, HighFat, Histamine), unit = "square", typical = 3.0))
        add(ing("Milk chocolate", setOf(Dairy, Casein, Lactose, Caffeine, HighFat), unit = "square", typical = 4.0))
        add(ing("Cocoa powder", setOf(Caffeine), unit = "tbsp", typical = 1.0))
        add(ing("Crisps", setOf(HighFat, Nightshade, SeedOil), unit = "g", typical = 30.0, aliases = listOf("chips", "potato chips")))
        add(ing("Chilli powder", setOf(Capsaicin, Nightshade), unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Paprika", setOf(Nightshade), unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Cayenne", setOf(Capsaicin, Nightshade), unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Cumin", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Coriander seed", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Turmeric", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Cinnamon", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Black pepper", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Oregano", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Basil", unit = "tbsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Parsley", unit = "tbsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Coriander leaf", unit = "tbsp", typical = 1.0, aliases = listOf("cilantro"), roles = setOf(Roles.Herb)))
        add(ing("Mint", unit = "tbsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Thyme", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Rosemary", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Bay leaf", unit = "leaf", typical = 1.0, roles = setOf(Roles.Herb)))
        add(ing("Salt", unit = "tsp", typical = 1.0, roles = setOf(Roles.Herb)))

        // ---- Drinks -----------------------------------------------------------
        // Caffeine, alcohol and carbonation are attributes rather than diary
        // fields precisely so they come in here and need no second entry. The
        // fizz is worth watching in its own right — it is a very commonly missed
        // cause of bloating.
        add(ing("Coffee, black", setOf(Caffeine), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Coffee with milk", setOf(Caffeine, Dairy, Casein, Lactose), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Espresso", setOf(Caffeine), unit = "shot", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Decaf coffee", unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Tea, black", setOf(Caffeine), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Tea, green", setOf(Caffeine), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Matcha", setOf(Caffeine), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Chai", setOf(Caffeine), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Peppermint tea", unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Chamomile tea", unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Ginger tea", unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Fennel tea", setOf(Fructan), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Chicory coffee substitute", setOf(Fructan), unit = "cup", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Cola", setOf(Caffeine, Carbonation), unit = "can", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Diet soft drink", setOf(Caffeine, Carbonation, Sweetener), unit = "can", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Lemonade", setOf(Carbonation), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Sparkling water", setOf(Carbonation), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Kombucha", setOf(Carbonation, Fermented, Histamine), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Apple juice", setOf(ExcessFructose), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Orange juice", setOf(Citrus), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Beer", setOf(Alcohol, Gluten, Barley, Carbonation, Fructan), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Gluten-free beer", setOf(Alcohol, Carbonation), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Red wine", setOf(Alcohol, Histamine, Sulphite), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("White wine", setOf(Alcohol, Histamine, Sulphite), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Sparkling wine", setOf(Alcohol, Carbonation, Histamine, Sulphite), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Cider", setOf(Alcohol, Carbonation, ExcessFructose, Sulphite), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Spirits", setOf(Alcohol), unit = "shot", typical = 1.0, roles = setOf(Roles.Drink)))
        add(ing("Rum and cola", setOf(Alcohol, Caffeine, Carbonation), unit = "glass", typical = 1.0, roles = setOf(Roles.Drink)))
    }

    /**
     * The library merged into what's already stored.
     *
     * Anything you've edited wins, anything you've retired stays gone, and only
     * genuinely new entries are added. That's what makes bumping [VERSION] safe.
     */
    fun mergeInto(existing: List<Ingredient>, retired: Set<String>): List<Ingredient> {
        val known = existing.map { it.id }.toSet()
        return existing + all.filter { it.id !in known && it.id !in retired }
    }
}
