package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable

/**
 * What an ingredient is *for*, as opposed to what it contains.
 *
 * Attributes answer "could this be the problem". Roles answer a much duller
 * question — "would anyone put this on toast" — and exist only so a category's
 * list can fill itself in. Peanut butter belongs on toast and in porridge and,
 * for some people, in a salad; without roles that means adding it by hand to
 * three lists and to every list invented later.
 *
 * Deliberately coarse and deliberately short. This is a second vocabulary
 * sitting next to [Attribute], and the only thing stopping it sprawling is
 * refusing to add a role that doesn't change what some category offers.
 */
@Serializable
@JvmInline
value class Role(val id: String) {

    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    companion object {
        const val CUSTOM_PREFIX = "custom:"

        fun custom(name: String) = Role(CUSTOM_PREFIX + name.trim())
    }
}

data class RoleInfo(val role: Role, val label: String)

object Roles {

    val Spread = Role("spread")
    val Sweetener = Role("sweetener")
    val Fruit = Role("fruit")
    val Vegetable = Role("vegetable")
    val SaladLeaf = Role("salad-leaf")
    val Cheese = Role("cheese")
    // "eggs", not "egg": the Egg *attribute* already owns that id, and two
    // vocabularies sharing a string in the same file is how someone later
    // reads one as the other.
    val Egg = Role("eggs")
    val NutSeed = Role("nut-seed")
    val Grain = Role("grain")
    val Legume = Role("legume")
    val Milk = Role("milk")
    val Yoghurt = Role("yoghurt")
    val Oil = Role("oil")
    val Herb = Role("herb-spice")
    val Sauce = Role("sauce")
    val Drink = Role("drink")

    val known: List<RoleInfo> = listOf(
        RoleInfo(Spread, "Spread"),
        RoleInfo(Sweetener, "Sweetener"),
        RoleInfo(Fruit, "Fruit"),
        RoleInfo(Vegetable, "Vegetable"),
        RoleInfo(SaladLeaf, "Salad leaf"),
        RoleInfo(Cheese, "Cheese"),
        RoleInfo(Egg, "Egg"),
        RoleInfo(NutSeed, "Nut or seed"),
        RoleInfo(Grain, "Grain"),
        RoleInfo(Legume, "Legume"),
        RoleInfo(Milk, "Milk"),
        RoleInfo(Yoghurt, "Yoghurt or cream"),
        RoleInfo(Oil, "Oil or fat"),
        RoleInfo(Herb, "Herb or spice"),
        RoleInfo(Sauce, "Sauce or condiment"),
        RoleInfo(Drink, "Drink"),
    )

    private val byId: Map<Role, RoleInfo> = known.associateBy { it.role }

    /** Readable name for any role, including one this build doesn't know. */
    fun label(role: Role): String = when {
        role.isCustom -> role.id.removePrefix(Role.CUSTOM_PREFIX)
        else -> byId[role]?.label ?: role.id
    }
}
