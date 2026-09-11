package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortionTest {

    private val sourdough = Ingredient(
        id = "sourdough",
        name = "Sourdough",
        defaultUnit = "slice",
        typicalAmount = 2.0,
    )

    private val garlic = Ingredient(id = "garlic", name = "Garlic", defaultUnit = "clove", typicalAmount = 1.0)

    @Test
    fun `no portion stays unknown`() {
        // The whole point: absent must never be silently read as normal, or the
        // analysis ends up reasoning from a number nobody gave it.
        assertNull(null.bucket(sourdough))
        assertNull(Portion().bucket(sourdough))
    }

    @Test
    fun `coarse size is used as given`() {
        assertEquals(PortionSize.LITTLE, Portion.of(PortionSize.LITTLE).bucket(sourdough))
        assertEquals(PortionSize.LOTS, Portion.of(PortionSize.LOTS).bucket(sourdough))
    }

    @Test
    fun `exact amount buckets against the typical serving`() {
        assertEquals(PortionSize.LITTLE, Portion.of(1.0, "slice").bucket(sourdough))
        assertEquals(PortionSize.NORMAL, Portion.of(2.0, "slice").bucket(sourdough))
        assertEquals(PortionSize.NORMAL, Portion.of(3.0, "slice").bucket(sourdough))
        assertEquals(PortionSize.LOTS, Portion.of(4.0, "slice").bucket(sourdough))
    }

    @Test
    fun `boundaries are inclusive at half and double`() {
        assertEquals(PortionSize.LITTLE, Portion.of(0.5, "clove").bucket(garlic))
        assertEquals(PortionSize.LOTS, Portion.of(2.0, "clove").bucket(garlic))
    }

    @Test
    fun `a different unit is not guessed at`() {
        // "2 g" against a typical of 2 slices is not a normal portion, and
        // inventing a conversion would be worse than admitting we don't know.
        assertNull(Portion.of(2.0, "g").bucket(sourdough))
    }

    @Test
    fun `an ingredient with no typical serving cannot be bucketed`() {
        val salt = Ingredient(id = "salt", name = "Salt", defaultUnit = "tsp")
        assertNull(Portion.of(1.0, "tsp").bucket(salt))
    }

    @Test
    fun `coarse size wins over an exact amount when both are set`() {
        val portion = Portion(size = PortionSize.LOTS, amount = 1.0, unit = "slice")
        assertEquals(PortionSize.LOTS, portion.bucket(sourdough))
    }

    @Test
    fun `descriptions read like a diary rather than a spreadsheet`() {
        assertEquals("× 1 slice", Portion.of(1.0, "slice").describe(sourdough))
        assertEquals("× 2 cloves".replace(" cloves", " clove"), Portion.of(2.0, "clove").describe(garlic))
        assertEquals("a little", Portion.of(PortionSize.LITTLE).describe(sourdough))
        assertEquals("", null.describe(sourdough))
    }
}
