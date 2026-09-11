package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The explanations behind the chips.
 *
 * A chip you can tap and learn nothing from is worse than one you cannot tap,
 * because the first promises something. So the coverage is asserted rather
 * than trusted: every attribute the app can put on a meal has to explain
 * itself, and adding a new one without writing that explanation fails here
 * rather than shipping a blank card.
 */
class AttributeInfoTest {

    @Test
    fun `every attribute says what it is`() {
        Attributes.known.forEach { info ->
            assertTrue(
                "${info.label} has no plain-language explanation",
                info.what.isNotBlank(),
            )
        }
    }

    @Test
    fun `every attribute can answer what trouble with it feels like`() {
        // Either its own text or its group's. Never nothing, because the
        // button that reveals it is always offered.
        Attributes.known.forEach { info ->
            assertTrue(
                "${info.label} resolves to no symptoms text",
                Attributes.symptoms(info.attribute).isNotBlank(),
            )
        }
    }

    @Test
    fun `the explanation is a paragraph, not an essay`() {
        // It pops up over the day you were reading. Anything longer than this
        // wants a screen of its own, and the whole point was not needing one.
        Attributes.known.forEach { info ->
            assertTrue(
                "${info.label} is ${info.what.length} characters of 'what'",
                info.what.length <= 400,
            )
        }
    }

    @Test
    fun `nothing explains itself with its own name and nothing else`() {
        // "Sulphites: sulphites in food." A real risk when filling in thirty of
        // these, and worthless to the person reading it.
        Attributes.known.forEach { info ->
            val firstWords = info.what.take(info.label.length + 4).lowercase()
            assertFalse(
                "${info.label} explains itself with its own name",
                info.what.length < 40 && firstWords.contains(info.label.lowercase()),
            )
        }
    }

    @Test
    fun `the allergens say that an allergy is a doctor's problem`() {
        // The one place this app must not be the tool someone reaches for. A
        // peanut allergy is minutes and breathing, not weeks and a pattern.
        listOf(Attributes.Peanut, Attributes.TreeNut).forEach { allergen ->
            val text = Attributes.symptoms(allergen).lowercase()
            assertTrue(
                "${Attributes.label(allergen)} does not point at a doctor",
                "doctor" in text,
            )
        }
    }

    @Test
    fun `gluten says to get tested before cutting it out`() {
        // Coeliac serology only works while you are still eating gluten. Going
        // gluten-free first is the single most common way people lose the
        // chance of a diagnosis, and this app is otherwise an encouragement to
        // do exactly that.
        val text = Attributes.symptoms(Attributes.Gluten).lowercase()
        assertTrue("gluten does not mention coeliac", "coeliac" in text)
        assertTrue("gluten does not mention testing", "test" in text)
    }

    @Test
    fun `seed oils are honest about having no established mechanism`() {
        // It is tracked because it was asked for, and correlation from a real
        // log is real evidence. Dressing it up as settled science would not be.
        val text = Attributes.symptoms(Attributes.SeedOil).lowercase()
        assertTrue("seed oils overclaim", "no established" in text)
    }

    @Test
    fun `the five FODMAPs share one description of what it feels like`() {
        // Deliberate. They present alike, and five subtly different paragraphs
        // would be distinctions this app invented.
        val fodmaps = listOf(
            Attributes.Fructan, Attributes.Gos, Attributes.ExcessFructose,
            Attributes.Sorbitol, Attributes.Mannitol,
        )
        val texts = fodmaps.map { Attributes.symptoms(it) }.distinct()
        assertEquals(1, texts.size)
        assertTrue(texts.single().contains("dose-dependent"))
    }

    @Test
    fun `an attribute this build has never heard of still explains its silence`() {
        val unknown = Attribute.custom("Marmite")
        assertEquals("", Attributes.what(unknown))
        // The group fallback still answers, so the card is never blank.
        assertTrue(Attributes.symptoms(unknown).isNotBlank())
    }

    @Test
    fun `what and note say different things`() {
        // The split is the design: what it is, then the thing you would not
        // have guessed. If one is a copy of the other, one of them is wasted.
        Attributes.known.filter { it.note.isNotBlank() }.forEach { info ->
            assertFalse("${info.label} repeats itself", info.what == info.note)
        }
    }
}
