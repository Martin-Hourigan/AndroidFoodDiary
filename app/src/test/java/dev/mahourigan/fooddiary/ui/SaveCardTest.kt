package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Basis
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Portion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the save sheet asks about the library.
 *
 * The bug this exists to stop: logging a saved meal you hadn't touched came up
 * saying "You changed Creamy tomato and basil beans" and offering to update it.
 * Asking what to do about a change nobody made is the app inventing work — and
 * the offer to update has since gone entirely, because a recipe should not be
 * rewritable from the screen where you record your dinner.
 */
class SaveCardTest {

    private val beans = listOf(
        MealItem("butter-beans-tinned-and-rinsed", Portion.of(2.0, "tin")),
        MealItem("tofu-firm", Portion.of(250.0, "g")),
        MealItem("basil", Portion.of(2.0, "tbsp")),
    )

    private fun draftFrom(vararg items: MealItem) = MealDraft(
        bases = listOf(Basis("creamy-beans", "Creamy tomato and basil beans", beans)),
        items = items.toList(),
    )

    @Test
    fun `a saved meal logged as it stands asks nothing`() {
        assertEquals(SaveCard.NONE, draftFrom(*beans.toTypedArray()).saveCard)
    }

    @Test
    fun `dropping something from it offers the three answers`() {
        assertEquals(SaveCard.SAVE_BACK, draftFrom(beans[0], beans[1]).saveCard)
    }

    @Test
    fun `changing an amount counts as changing it`() {
        val lessTofu = beans.toMutableList().also {
            it[1] = MealItem("tofu-firm", Portion.of(125.0, "g"))
        }
        assertEquals(SaveCard.SAVE_BACK, draftFrom(*lessTofu.toTypedArray()).saveCard)
    }

    @Test
    fun `adding something to it offers the three answers`() {
        assertEquals(
            SaveCard.SAVE_BACK,
            draftFrom(*beans.toTypedArray(), MealItem("avocado")).saveCard,
        )
    }

    @Test
    fun `combining two saved meals asks, even with neither changed`() {
        // Neither part changed, but the combination is a new thing worth
        // offering to keep.
        val draft = MealDraft(
            bases = listOf(
                Basis("toast", "Toast", listOf(MealItem("wheat-bread-wholemeal"))),
                Basis("creamy-beans", "Creamy tomato and basil beans", beans),
            ),
            items = listOf(MealItem("wheat-bread-wholemeal")) + beans,
        )
        assertEquals(SaveCard.SAVE_BACK, draft.saveCard)
    }

    @Test
    fun `a meal built from ingredients offers to keep it`() {
        assertEquals(
            SaveCard.KEEP_AS_MEAL,
            MealDraft(items = listOf(MealItem("avocado"), MealItem("egg-whole"))).saveCard,
        )
    }

    @Test
    fun `an empty draft asks nothing`() {
        assertEquals(SaveCard.NONE, MealDraft().saveCard)
    }
}
