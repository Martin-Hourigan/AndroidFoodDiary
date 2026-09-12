package dev.mahourigan.fooddiary.domain

/**
 * The three different ways a measure knows what it weighs.
 *
 * They are genuinely different mechanisms rather than three names for one, and
 * only one of them needs a number stored per unit:
 *
 * - [WEIGHT] already *is* grams. Nothing to convert.
 * - [VOLUME] is millilitres, and grams follow from the ingredient's density.
 *   One number unlocks every spoon and cup at once.
 * - [COUNT] is a thing, and a thing has no volume you can infer. Each one needs
 *   its own weight, and it only means anything where the things are consistently
 *   sized — a banana, a clove, a slice. Not a spoonful of powder.
 */
enum class MeasureKind { WEIGHT, VOLUME, COUNT }

/** One way of measuring one ingredient, and what a single unit of it weighs. */
data class Measure(
    val unit: String,
    val kind: MeasureKind,
    /** Grams in one of these, for this ingredient. */
    val gramsEach: Double,
) {
    /** Whether this was worked out rather than written down. */
    val isDerived: Boolean get() = kind == MeasureKind.VOLUME && unit != "ml"
}

/**
 * Measures, and the arithmetic between them.
 *
 * The thing this exists to prevent: treating a unit name as if it carried a
 * weight. A tablespoon of chives is 3 g and a tablespoon of maple syrup is 20 g
 * — nearly sevenfold — so "tbsp" can never mean a number on its own. The
 * conversion belongs to the ingredient, always.
 */
object Measures {

    /**
     * Volume units the app understands, in millilitres.
     *
     * `glass` and `shot` are here because the seed library uses them for drinks,
     * and they are volumes like any other; treating them as counts would have
     * meant storing a weight for a glass of every liquid separately.
     */
    val VOLUMES: Map<String, Double> = linkedMapOf(
        "ml" to 1.0,
        "tsp" to 5.0,
        "tbsp" to 15.0,
        "shot" to 30.0,
        "cup" to 250.0,
        "glass" to 250.0,
    )

    private const val GRAM = "g"

    fun kindOf(unit: String): MeasureKind = when {
        unit.equals(GRAM, ignoreCase = true) -> MeasureKind.WEIGHT
        VOLUMES.keys.any { it.equals(unit, ignoreCase = true) } -> MeasureKind.VOLUME
        else -> MeasureKind.COUNT
    }

    /** Millilitres in one of [unit], or null if it isn't a volume. */
    fun millilitresIn(unit: String): Double? =
        VOLUMES.entries.firstOrNull { it.key.equals(unit, ignoreCase = true) }?.value

    /**
     * Every way this ingredient can sensibly be measured, suggestion first.
     *
     * Volume measures appear **only** for something already measured by volume,
     * because density is the one thing that cannot be guessed from a weight. A
     * gram of flour and a gram of oil occupy quite different spaces, so an
     * ingredient recorded in grams gets grams and nothing else — which is the
     * right answer anyway. Nobody measures tofu in tablespoons.
     */
    fun of(ingredient: Ingredient): List<Measure> {
        val out = LinkedHashMap<String, Measure>()

        fun offer(unit: String, grams: Double) {
            if (grams > 0) out.putIfAbsent(unit.lowercase(), Measure(unit, kindOf(unit), grams))
        }

        // The suggestion leads, whatever kind it is.
        val suggested = ingredient.defaultUnit
        if (suggested != null) {
            gramsIn(suggested, ingredient)?.let { offer(suggested, it) }
        }

        // Grams are always on offer, for anything at all. Putting something on
        // a scale needs no knowledge of what it is, so this is the one measure
        // that never depends on the library knowing anything -- and it is the
        // fallback for an ingredient you added yourself a minute ago.
        offer(GRAM, 1.0)

        // And if a density is known, every volume comes free with it.
        val density = ingredient.densityGPerMl
        if (density != null && density > 0) {
            VOLUMES.forEach { (unit, ml) -> offer(unit, ml * density) }
        }

        return out.values.toList()
    }

    /**
     * What one [unit] of [ingredient] weighs, or null if there is no way to know.
     *
     * Null is a real answer and callers must treat it as one. Inventing a
     * conversion would put a wrong number into the calorie total and the dose
     * buckets at once, and neither would ever look obviously wrong.
     */
    fun gramsIn(unit: String, ingredient: Ingredient): Double? {
        if (unit.equals(GRAM, ignoreCase = true)) return 1.0

        millilitresIn(unit)?.let { ml ->
            val density = ingredient.densityGPerMl
            if (density != null && density > 0) return ml * density
            // A volume unit with no density: only the ingredient's own unit can
            // be answered, from the figure recorded against it.
            if (unit.equals(ingredient.defaultUnit, ignoreCase = true)) return ingredient.gramsPerUnit
            return null
        }

        // A count only converts for the unit it was recorded against. "2 cloves"
        // means something for garlic and nothing for anything else.
        return if (unit.equals(ingredient.defaultUnit, ignoreCase = true)) ingredient.gramsPerUnit else null
    }

    /** [amount] of [unit] as grams, or null when the conversion isn't known. */
    fun toGrams(amount: Double, unit: String?, ingredient: Ingredient): Double? {
        val named = unit ?: ingredient.defaultUnit ?: return null
        val each = gramsIn(named, ingredient) ?: return null
        return amount * each
    }

    /**
     * The three amounts to offer as one-tap chips, in [measure]'s own unit.
     *
     * They sit at half, one and double the typical serving, because those are
     * exactly the boundaries [bucket] sorts by — so a tap lands squarely inside
     * a bucket rather than near its edge.
     *
     * **Except for counted things**, where half is nonsense: nobody measures
     * half a clove of garlic or half an egg. Those get 1, 2, 3 instead. The
     * buckets still classify it correctly, since three cloves against a typical
     * of one is "lots" either way; only the labels change.
     */
    fun quickAmounts(ingredient: Ingredient, measure: Measure): List<Double> {
        val typicalHere = typicalIn(measure, ingredient)

        if (measure.kind == MeasureKind.COUNT) {
            // A count of one or two is the common case, and halving it reads as
            // a joke. Anything counted in larger numbers can still be halved.
            if (typicalHere == null || typicalHere <= 2.0) return listOf(1.0, 2.0, 3.0)
            return listOf(round(typicalHere / 2), round(typicalHere), round(typicalHere * 2))
        }

        val typical = typicalHere ?: return fallbackFor(measure)
        return listOf(round(typical / 2), round(typical), round(typical * 2)).distinct()
    }

    /** The ingredient's typical serving expressed in [measure], if it is known. */
    fun typicalIn(measure: Measure, ingredient: Ingredient): Double? {
        val typical = ingredient.typicalAmount ?: return null
        val grams = toGrams(typical, ingredient.defaultUnit, ingredient) ?: return null
        if (measure.gramsEach <= 0) return null
        return grams / measure.gramsEach
    }

    /** Something plausible when the library has no typical serving to scale. */
    private fun fallbackFor(measure: Measure): List<Double> = when (measure.kind) {
        MeasureKind.WEIGHT -> listOf(25.0, 50.0, 100.0)
        MeasureKind.VOLUME -> listOf(1.0, 2.0, 3.0)
        MeasureKind.COUNT -> listOf(1.0, 2.0, 3.0)
    }

    /**
     * A number a person would actually say.
     *
     * 7.5 g is a spreadsheet talking; 7 g is a person. Bigger amounts round
     * harder, because nobody weighs 152 g of anything on purpose.
     */
    internal fun round(value: Double): Double = when {
        value <= 0 -> 0.0
        value < 1 -> (Math.round(value * 4) / 4.0)      // quarters: 0.25, 0.5, 0.75
        value < 10 -> (Math.round(value * 2) / 2.0)     // halves
        value < 100 -> Math.round(value / 5.0) * 5.0    // fives
        else -> Math.round(value / 10.0) * 10.0         // tens
    }

    /**
     * The density implied by an ingredient recorded in a volume unit.
     *
     * Used once, by the migration, to lift the existing single conversion into
     * something every other volume can be derived from: a figure of 5 g per
     * tablespoon is really 0.33 g/ml, and that also answers teaspoons and cups.
     */
    fun impliedDensity(ingredient: Ingredient): Double? {
        val unit = ingredient.defaultUnit ?: return null
        val grams = ingredient.gramsPerUnit ?: return null
        val ml = millilitresIn(unit) ?: return null
        if (ml <= 0 || grams <= 0) return null
        return grams / ml
    }
}
