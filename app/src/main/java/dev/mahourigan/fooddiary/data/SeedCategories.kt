package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.Attributes.Fermented
import dev.mahourigan.fooddiary.domain.Attributes.Refined
import dev.mahourigan.fooddiary.domain.Attributes.Sourdough
import dev.mahourigan.fooddiary.domain.Attributes.Wholegrain
import dev.mahourigan.fooddiary.domain.Category
import dev.mahourigan.fooddiary.domain.Facet
import dev.mahourigan.fooddiary.domain.FacetKind
import dev.mahourigan.fooddiary.domain.FacetOption
import dev.mahourigan.fooddiary.domain.Roles
import dev.mahourigan.fooddiary.domain.MealItem

/**
 * The build-it-yourself categories.
 *
 * These replace what used to be a folder of near-identical saved meals. Toast is
 * two questions — which bread, what on it — and answering them is faster than
 * scrolling a list of every combination anyone might eat.
 *
 * Both are meant to be edited. The bread styles in particular are whatever you
 * actually buy.
 */
object SeedCategories {

    const val VERSION = 2

    /**
     * What every list of addable things is called.
     *
     * One name, not a different one per category. "On it", "Made with", "On top"
     * and "Sweetened" were four words for the same idea — things that go in the
     * meal — and splitting them up meant deciding which list a topping belonged
     * to before you could tap it. There is one style question per category and
     * then there are ingredients.
     */
    const val INGREDIENTS = "Ingredients"

    /**
     * What a style facet is called when naming the thing would just repeat the
     * category — "Coffee › Coffee › Black" is nobody's idea of a question.
     *
     * Toast keeps "Bread" and porridge keeps "Oats", because there the facet
     * names something other than the category and that's more use than "Type".
     */
    const val TYPE = "Type"

    private fun id(name: String) = SeedIngredients.slug(name)

    private fun topping(name: String) = FacetOption(label = name, ingredientId = id(name))

    val all: List<Category> = listOf(

        Category(
            id = "cat-toast",
            name = "Toast",
            facets = listOf(
                Facet(
                    name = "Bread",
                    kind = FacetKind.STYLE,
                    // Everything lands on wheat unless an option says otherwise,
                    // because most bread is wheat and saying so shouldn't cost a tap.
                    defaultIngredientId = id("Wheat bread, wholemeal"),
                    options = listOf(
                        // Character, not identity: these describe whatever loaf
                        // it is, and several can be true of one loaf at once.
                        FacetOption("Sourdough", modifiers = setOf(Sourdough, Fermented)),
                        FacetOption("Wholemeal", modifiers = setOf(Wholegrain)),
                        FacetOption("White", modifiers = setOf(Refined)),
                        FacetOption("Multigrain", modifiers = setOf(Wholegrain)),
                        FacetOption("Seeded", modifiers = setOf(Wholegrain)),
                        // These change what the bread *is*, so they carry an
                        // ingredient and win over the default.
                        FacetOption("Rye", ingredientId = id("Rye bread"), modifiers = setOf(Wholegrain)),
                        FacetOption("Gluten-free", ingredientId = id("Gluten-free bread")),
                        FacetOption("Spelt", ingredientId = id("Sourdough, spelt")),
                    ),
                ),
                Facet(
                    name = INGREDIENTS,
                    kind = FacetKind.ITEMS,
                    options = listOf(
                        topping("Butter"),
                        topping("Vegemite"),
                        topping("Avocado"),
                        topping("Peanut butter"),
                        topping("Tahini"),
                        topping("Honey"),
                        topping("Cheddar, mature"),
                        topping("Egg, whole"),
                        topping("Tomato"),
                        topping("Mushrooms, button"),
                        topping("Olive oil"),
                    ),
                    // What else could plausibly go on toast. Shown behind
                    // "more" so the shortlist above stays the shortlist.
                    roles = setOf(
                        Roles.Spread, Roles.Sweetener, Roles.Cheese,
                        Roles.Egg, Roles.Vegetable, Roles.Oil,
                    ),
                ),
            ),
        ),

        Category(
            id = "cat-porridge",
            name = "Porridge",
            facets = listOf(
                Facet(
                    name = "Oats",
                    kind = FacetKind.STYLE,
                    defaultIngredientId = id("Oats, rolled"),
                    options = listOf(
                        FacetOption("Rolled oats", ingredientId = id("Oats, rolled"), modifiers = setOf(Wholegrain)),
                        FacetOption("Gluten-free oats", ingredientId = id("Oats, gluten-free"), modifiers = setOf(Wholegrain)),
                    ),
                ),
                Facet(
                    name = INGREDIENTS,
                    kind = FacetKind.ITEMS,
                    options = listOf(
                        topping("Oat milk"),
                        topping("Almond milk"),
                        topping("Soy milk, whole bean"),
                        topping("Coconut milk drink"),
                        topping("Banana, firm"),
                        topping("Blueberries"),
                        topping("Raspberries"),
                        topping("Honey"),
                        topping("Maple syrup"),
                        topping("Cinnamon"),
                        topping("Almonds"),
                        topping("Peanut butter"),
                        topping("Yoghurt, natural"),
                    ),
                    roles = setOf(
                        Roles.Milk, Roles.Fruit, Roles.NutSeed,
                        Roles.Sweetener, Roles.Spread, Roles.Yoghurt,
                    ),
                ),
            ),
        ),

        // No style question here: black, espresso and decaf are a distinction
        // this household doesn't make, and a question you always answer the same
        // way is just a tap.
        Category(
            id = "cat-coffee",
            name = "Coffee",
            // A coffee contains coffee. The list is what goes *in* it.
            always = listOf(MealItem(ingredientId = id("Coffee, black"))),
            facets = listOf(
                Facet(
                    name = INGREDIENTS,
                    kind = FacetKind.ITEMS,
                    options = listOf(
                        topping("Oat milk"),
                        topping("Almond milk"),
                        topping("Soy milk, whole bean"),
                        topping("Macadamia milk"),
                        topping("Coconut milk drink"),
                        topping("Sugar"),
                        topping("Honey"),
                        topping("Stevia"),
                    ),
                    roles = setOf(Roles.Milk, Roles.Sweetener),
                ),
            ),
        ),
    )
}
