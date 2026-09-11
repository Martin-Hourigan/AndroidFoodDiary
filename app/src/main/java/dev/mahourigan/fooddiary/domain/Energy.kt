package dev.mahourigan.fooddiary.domain

/**
 * Calories, at whatever precision the entry actually supports.
 *
 * Three levels, each overriding the one below, which is the same shape as
 * portions on purpose:
 *
 * 1. **A number you typed** on the meal, or on the recipe you made. Exact,
 *    because you knew it.
 * 2. **An amount you gave** on an item — 40 g of cheddar — turned into calories
 *    through the ingredient's per-100 figure.
 * 3. **The ingredient's usual serving**, when you said nothing. A guess, and
 *    labelled as one.
 *
 * The third case is why every total is reported with [assumed] rather than as a
 * bare number. A meal assembled from typical servings is easily ±20%, and a
 * figure that looks precise but isn't is worse than an obvious estimate: you'd
 * make decisions on it.
 */
data class Energy(
    val kcal: Double?,

    /** True when any part of the total came from an assumed serving. */
    val assumed: Boolean = false,

    /** Items with no energy data at all, so the total is short by an unknown amount. */
    val unknownItems: Int = 0,

    /** True when the figure was typed rather than worked out. */
    val stated: Boolean = false,
) {
    val isKnown: Boolean get() = kcal != null

    /** True when the number is worth showing without a caveat. */
    val isExact: Boolean get() = stated || (!assumed && unknownItems == 0)

    companion object {
        val Unknown = Energy(kcal = null)
    }
}

/**
 * How much of this ingredient the portion amounts to, in grams.
 *
 * Null when it can't be worked out — no data, or an amount in a unit that can't
 * be converted. Guessing a conversion would put an invented number into a total
 * that people will read as fact.
 */
fun Portion?.grams(ingredient: Ingredient): Double? {
    val perUnit = ingredient.gramsPerUnit ?: return null

    // Nothing said: fall back to what one serving usually is.
    val portion = this ?: return ingredient.typicalAmount?.let { it * perUnit }

    portion.amount?.let { amount ->
        val unit = portion.unit ?: ingredient.defaultUnit
        return when {
            // Already a weight or a volume, so it needs no conversion.
            unit.equals("g", ignoreCase = true) || unit.equals("ml", ignoreCase = true) -> amount
            unit == null || unit.equals(ingredient.defaultUnit, ignoreCase = true) -> amount * perUnit
            else -> null
        }
    }

    val typical = ingredient.typicalAmount ?: return null
    val factor = when (portion.size) {
        PortionSize.LITTLE -> 0.5
        PortionSize.LOTS -> 2.0
        PortionSize.NORMAL, null -> 1.0
    }
    return typical * perUnit * factor
}

/** The energy in one item of a meal. */
fun MealItem.energy(byId: Map<String, Ingredient>): Energy {
    val ingredient = byId[ingredientId] ?: return Energy(kcal = null, unknownItems = 1)
    val per100 = ingredient.kcalPer100 ?: return Energy(kcal = null, unknownItems = 1)
    val grams = portion.grams(ingredient) ?: return Energy(kcal = null, unknownItems = 1)

    // A coarse size is still a guess about how much, so it counts as assumed.
    val guessed = portion == null || portion.amount == null
    return Energy(kcal = grams / 100.0 * per100, assumed = guessed)
}

private fun List<Energy>.total(): Energy {
    val known = filter { it.isKnown }
    return Energy(
        kcal = if (known.isEmpty()) null else known.sumOf { it.kcal!! },
        assumed = known.any { it.assumed },
        unknownItems = sumOf { it.unknownItems },
    )
}

/**
 * What a meal came to.
 *
 * A typed number wins outright. Otherwise any part that was a saved recipe with
 * its own figure contributes that, and its ingredients are skipped so they can't
 * be counted twice — the whole point of putting a number on a recipe is that you
 * worked it out once and don't want it re-estimated.
 */
fun MealEntry.energy(byId: Map<String, Ingredient>, recipeKcal: (String) -> Int?): Energy {
    kcalOverride?.let { return Energy(kcal = it.toDouble(), stated = true) }

    val statedBases = bases.mapNotNull { basis -> recipeKcal(basis.recipeId)?.let { basis to it } }
    val covered = statedBases.flatMap { (basis, _) -> basis.itemsAtLogTime.map { it.ingredientId } }.toSet()

    val fromBases = statedBases.map { (_, kcal) -> Energy(kcal = kcal.toDouble(), stated = true) }
    val fromItems = items.filterNot { it.ingredientId in covered }.map { it.energy(byId) }

    val total = (fromBases + fromItems).total()
    // Only the stated parts were stated; if anything was estimated the whole
    // thing is an estimate.
    return total.copy(stated = fromItems.isEmpty() && fromBases.isNotEmpty())
}

/** The energy in a saved recipe, from its own figure or from its ingredients. */
fun Recipe.energy(byId: Map<String, Ingredient>): Energy {
    kcal?.let { return Energy(kcal = it.toDouble(), stated = true) }
    return items.map { it.energy(byId) }.total()
}

/**
 * How a total reads.
 *
 * Rounded to 10 once it's an estimate, because the last digit of a guess is
 * noise pretending to be information.
 */
/**
 * The figure on show, as a plain number in [unit].
 *
 * Shared with [describe] so the two can never disagree — this is what gets
 * pre-filled into the calorie box, and a field showing 4900 under a total
 * reading 4911 would be its own little bug.
 */
fun Energy.amountIn(unit: EnergyUnit): Long? {
    val k = kcal ?: return null
    val shown = unit.from(k)
    if (isExact) return Math.round(shown)
    // An estimate is rounded coarsely, because the last digit of a guess is
    // noise dressed as information.
    val step = unit.estimateStep
    return Math.round(shown / step) * step.toLong()
}

fun Energy.describe(unit: EnergyUnit = EnergyUnit.KCAL): String {
    val amount = amountIn(unit) ?: return ""
    return if (isExact) "$amount ${unit.suffix}" else "~$amount ${unit.suffix}"
}
