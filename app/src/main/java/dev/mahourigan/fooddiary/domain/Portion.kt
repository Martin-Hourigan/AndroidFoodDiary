package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable

enum class PortionSize(val label: String) {
    LITTLE("a little"),
    NORMAL("normal"),
    LOTS("lots"),
}

/**
 * How much of something was in a meal — optional, at either level of detail.
 *
 * Three ways to say it, and you never have to climb past the first:
 *
 * ```
 * avocado                  portion == null
 * avocado — lots           size = LOTS
 * sourdough × 1 slice      amount = 1.0, unit = "slice"
 * ```
 *
 * Unspecified is stored as *absent*, never quietly defaulted to [PortionSize.NORMAL].
 * Defaulting it would invent a number you never gave and then let the analysis
 * reason from it — the app would be making things up. Presence/absence is what
 * the trigger hunt actually runs on, and that works perfectly on a bare
 * ingredient; only dose-response needs this, and it can say how many exposures
 * it had to work with.
 */
@Serializable
data class Portion(
    val size: PortionSize? = null,
    val amount: Double? = null,
    val unit: String? = null,
) {
    val isEmpty: Boolean get() = size == null && amount == null

    companion object {
        fun of(size: PortionSize) = Portion(size = size)

        fun of(amount: Double, unit: String?) = Portion(amount = amount, unit = unit)
    }
}

/**
 * The single coarse axis everything downstream compares on.
 *
 * An exact amount is bucketed against the ingredient's typical serving — half or
 * less is a little, double or more is lots — so the lazy and the careful ways of
 * logging land in the same three buckets and mix freely in one dataset.
 *
 * Null means genuinely unknown, and callers must treat it as such rather than
 * substituting a middle value.
 */
fun Portion?.bucket(ingredient: Ingredient): PortionSize? {
    val portion = this ?: return null
    portion.size?.let { return it }

    val amount = portion.amount ?: return null
    val typical = ingredient.typicalAmount ?: return null
    if (typical <= 0.0) return null

    // Same unit on both sides needs no conversion at all: one slice against a
    // typical of two is a ratio whatever a slice happens to weigh, and asking
    // for grams first would refuse to bucket an ingredient nobody has weighed.
    val unit = portion.unit
    val sameUnit = unit == null || ingredient.defaultUnit == null ||
        unit.equals(ingredient.defaultUnit, ignoreCase = true)

    val ratio = if (sameUnit) {
        amount / typical
    } else {
        // Different units, so go through grams. 30 g of nutritional yeast and
        // 6 tsp of it are the same dose and belong in the same bucket. Only a
        // conversion the app genuinely does not know returns null -- which is
        // still the honest answer for "2 cloves" of something that isn't garlic.
        val amountGrams = Measures.toGrams(amount, unit, ingredient) ?: return null
        val typicalGrams = Measures.toGrams(typical, ingredient.defaultUnit, ingredient) ?: return null
        if (typicalGrams <= 0.0) return null
        amountGrams / typicalGrams
    }
    return when {
        ratio <= 0.5 -> PortionSize.LITTLE
        ratio >= 2.0 -> PortionSize.LOTS
        else -> PortionSize.NORMAL
    }
}

/** How a portion reads next to an ingredient name. Empty when nothing was said. */
fun Portion?.describe(ingredient: Ingredient): String {
    val portion = this ?: return ""
    portion.amount?.let { amount ->
        val unit = portion.unit ?: ingredient.defaultUnit
        return "× " + formatAmount(amount) + if (unit != null) " $unit" else ""
    }
    return portion.size?.label.orEmpty()
}

/** Trailing ".0" on a count of slices reads like a spreadsheet, not a diary. */
internal fun formatAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
