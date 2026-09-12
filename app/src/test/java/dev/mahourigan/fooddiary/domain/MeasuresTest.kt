package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Measures, and the arithmetic between them.
 *
 * The mistake all of this exists to prevent: treating a unit name as if it
 * carried a weight. A tablespoon of chives is 3 g and a tablespoon of maple
 * syrup is 20 g, so "tbsp" means nothing until you say what is in the spoon.
 */
class MeasuresTest {

    // Real figures from the seeded library.
    private val nooch = Ingredient(
        id = "nutritional-yeast", name = "Nutritional yeast",
        defaultUnit = "g", gramsPerUnit = 1.0,
        densityGPerMl = 5.0 / 15.0,     // one tbsp is 5 g
        typicalAmount = 15.0,
    )
    private val oliveOil = Ingredient(
        id = "olive-oil", name = "Olive oil",
        defaultUnit = "tbsp", gramsPerUnit = 14.0,
        densityGPerMl = 14.0 / 15.0,
        typicalAmount = 1.0,
    )
    private val garlic = Ingredient(
        id = "garlic", name = "Garlic",
        defaultUnit = "clove", gramsPerUnit = 3.0, typicalAmount = 1.0,
    )
    private val tofu = Ingredient(
        id = "tofu-firm", name = "Tofu, firm",
        defaultUnit = "g", gramsPerUnit = 1.0, typicalAmount = 150.0,
    )

    // ---- what kind of measure is this -------------------------------------

    @Test
    fun `units sort into the three kinds`() {
        assertEquals(MeasureKind.WEIGHT, Measures.kindOf("g"))
        listOf("ml", "tsp", "tbsp", "cup", "glass", "shot").forEach {
            assertEquals(it, MeasureKind.VOLUME, Measures.kindOf(it))
        }
        listOf("whole", "clove", "slice", "egg", "stalk", "nut").forEach {
            assertEquals(it, MeasureKind.COUNT, Measures.kindOf(it))
        }
    }

    @Test
    fun `an unknown unit is treated as a count, not as a volume`() {
        // Erring the safe way. A count only ever converts for the ingredient it
        // was recorded against, so an unrecognised unit converts for nothing —
        // where guessing it was a volume would invent a weight from a density.
        assertEquals(MeasureKind.COUNT, Measures.kindOf("sprig"))
    }

    // ---- the same spoon, different weights ---------------------------------

    @Test
    fun `a tablespoon weighs what the ingredient says, not what the unit says`() {
        assertEquals(5.0, Measures.gramsIn("tbsp", nooch)!!, 0.01)
        assertEquals(14.0, Measures.gramsIn("tbsp", oliveOil)!!, 0.01)
        // Nearly threefold between two things both measured in tablespoons.
        assertTrue(Measures.gramsIn("tbsp", oliveOil)!! > Measures.gramsIn("tbsp", nooch)!! * 2)
    }

    @Test
    fun `one density answers every volume at once`() {
        // The whole point of storing density rather than grams-per-spoon: these
        // three were never entered, and a unit added later would work too.
        assertEquals(1.67, Measures.gramsIn("tsp", nooch)!!, 0.01)
        assertEquals(5.0, Measures.gramsIn("tbsp", nooch)!!, 0.01)
        assertEquals(83.3, Measures.gramsIn("cup", nooch)!!, 0.1)
    }

    @Test
    fun `grams are always one gram`() {
        listOf(nooch, oliveOil, garlic, tofu).forEach {
            assertEquals(1.0, Measures.gramsIn("g", it)!!, 0.0)
        }
    }

    // ---- what gets offered --------------------------------------------------

    @Test
    fun `something spooned offers grams and every volume`() {
        val units = Measures.of(nooch).map { it.unit }
        assertTrue("g" in units)
        assertTrue("tsp" in units)
        assertTrue("tbsp" in units)
        assertTrue("cup" in units)
    }

    @Test
    fun `the suggestion comes first`() {
        assertEquals("g", Measures.of(nooch).first().unit)
        assertEquals("tbsp", Measures.of(oliveOil).first().unit)
        assertEquals("clove", Measures.of(garlic).first().unit)
    }

    @Test
    fun `something weighed offers grams and nothing else`() {
        // Nobody measures tofu in tablespoons, and more to the point a gram of
        // tofu and a gram of oil take up different space — so a weight can never
        // imply a volume.
        assertEquals(listOf("g"), Measures.of(tofu).map { it.unit })
    }

    @Test
    fun `something counted offers its count and grams, but no spoons`() {
        val units = Measures.of(garlic).map { it.unit }
        assertEquals(listOf("clove", "g"), units)
        assertTrue("tbsp" !in units)
    }

    @Test
    fun `a derived volume is marked as derived`() {
        val tsp = Measures.of(nooch).first { it.unit == "tsp" }
        assertTrue(tsp.isDerived)
        assertTrue(Measures.of(garlic).first { it.unit == "clove" }.isDerived.not())
    }

    // ---- refusing to guess --------------------------------------------------

    @Test
    fun `a count does not convert for an ingredient it does not belong to`() {
        // "2 cloves" of tofu is not a quantity, and answering it with a number
        // would put a wrong figure into both the calories and the dose bucket.
        assertNull(Measures.gramsIn("clove", tofu))
        assertNull(Measures.gramsIn("slice", garlic))
    }

    @Test
    fun `a volume does not convert without a density`() {
        assertNull(Measures.gramsIn("tbsp", garlic))
        assertNull(Measures.gramsIn("cup", tofu))
    }

    @Test
    fun `an ingredient that knows nothing can still be weighed`() {
        // Putting something on a scale needs no knowledge of what it is, so
        // grams work even for an ingredient added a minute ago with nothing
        // filled in. This test originally asserted the opposite and was simply
        // wrong: two grams is two grams.
        val vague = Ingredient(id = "x", name = "Something")
        assertEquals(listOf("g"), Measures.of(vague).map { it.unit })
        assertEquals(2.0, Measures.toGrams(2.0, "g", vague)!!, 0.0)
        // But nothing else is invented for it.
        assertNull(Measures.toGrams(2.0, "tbsp", vague))
    }

    // ---- amounts ------------------------------------------------------------

    @Test
    fun `an amount converts through its unit`() {
        assertEquals(10.0, Measures.toGrams(2.0, "tbsp", nooch)!!, 0.01)
        assertEquals(9.0, Measures.toGrams(3.0, "clove", garlic)!!, 0.01)
        assertEquals(150.0, Measures.toGrams(150.0, "g", tofu)!!, 0.01)
    }

    @Test
    fun `no unit given falls back to the ingredient's own`() {
        assertEquals(28.0, Measures.toGrams(2.0, null, oliveOil)!!, 0.01)
    }

    @Test
    fun `two ways of saying the same amount agree`() {
        // 3 tsp is 1 tbsp, and the app should not think otherwise.
        val viaSpoons = Measures.toGrams(3.0, "tsp", nooch)!!
        val viaTablespoon = Measures.toGrams(1.0, "tbsp", nooch)!!
        assertEquals(viaTablespoon, viaSpoons, 0.01)
    }

    // ---- the one-tap amounts ------------------------------------------------

    @Test
    fun `the three chips sit at half, one and double the typical serving`() {
        val grams = Measures.of(nooch).first { it.unit == "g" }
        // Typical is 15 g, so: 7 g, 15 g, 30 g.
        assertEquals(listOf(7.5, 15.0, 30.0).map { Measures.round(it) },
            Measures.quickAmounts(nooch, grams))
    }

    @Test
    fun `counted things never offer half of one`() {
        // Half a clove of garlic is not a thing anyone measures, and neither is
        // half an egg. The buckets still sort three cloves as "lots".
        val clove = Measures.of(garlic).first { it.unit == "clove" }
        assertEquals(listOf(1.0, 2.0, 3.0), Measures.quickAmounts(garlic, clove))
    }

    @Test
    fun `a count with a larger typical serving can still be halved`() {
        val bread = Ingredient(
            id = "sourdough", name = "Sourdough",
            defaultUnit = "slice", gramsPerUnit = 45.0, typicalAmount = 4.0,
        )
        val slice = Measures.of(bread).first { it.unit == "slice" }
        assertEquals(listOf(2.0, 4.0, 8.0), Measures.quickAmounts(bread, slice))
    }

    @Test
    fun `the chips follow whichever measure is selected`() {
        // Same ingredient, same typical serving, expressed two ways: 15 g of
        // nooch is 3 tbsp, so the spoon chips are a third of the gram ones.
        val tbsp = Measures.of(nooch).first { it.unit == "tbsp" }
        assertEquals(listOf(1.5, 3.0, 6.0), Measures.quickAmounts(nooch, tbsp))
    }

    @Test
    fun `amounts are rounded to numbers a person would say`() {
        assertEquals(0.5, Measures.round(0.53), 0.001)
        assertEquals(2.5, Measures.round(2.6), 0.001)
        assertEquals(15.0, Measures.round(14.2), 0.001)
        assertEquals(150.0, Measures.round(152.0), 0.001)
    }

    @Test
    fun `an ingredient with no typical serving still gets something plausible`() {
        val unknown = Ingredient(id = "x", name = "Mystery", defaultUnit = "g", gramsPerUnit = 1.0)
        val grams = Measures.of(unknown).first { it.unit == "g" }
        assertEquals(listOf(25.0, 50.0, 100.0), Measures.quickAmounts(unknown, grams))
    }

    // ---- lifting the old single conversion ---------------------------------

    @Test
    fun `a density can be recovered from a grams-per-spoon figure`() {
        // What the migration does: 5 g per tablespoon is really 0.33 g/ml, and
        // that also answers teaspoons and cups.
        val old = Ingredient(id = "x", name = "Nooch", defaultUnit = "tbsp", gramsPerUnit = 5.0)
        assertEquals(0.333, Measures.impliedDensity(old)!!, 0.001)
    }

    @Test
    fun `the recovered densities are physically right`() {
        // A sanity check on the seeded figures rather than on the code: if these
        // came out absurd it would mean the library's numbers disagreed with
        // each other, which is worth knowing before deriving anything from them.
        fun density(unit: String, grams: Double) =
            Measures.impliedDensity(Ingredient(id = "x", name = "x", defaultUnit = unit, gramsPerUnit = grams))!!

        assertEquals(0.93, density("tbsp", 14.0), 0.01)   // olive oil
        assertEquals(1.40, density("tsp", 7.0), 0.01)     // honey
        assertEquals(1.00, density("tbsp", 15.0), 0.01)   // water, by definition
        assertEquals(0.20, density("tbsp", 3.0), 0.01)    // chives, mostly air
    }

    @Test
    fun `a count yields no density`() {
        assertNull(Measures.impliedDensity(garlic))
        assertNull(Measures.impliedDensity(tofu))
    }
}
