package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable

/**
 * A property of an ingredient that you might want to eliminate.
 *
 * A string id rather than an enum, for two reasons. It serialises as plain text,
 * so the stored file and the export stay readable and an old export opened by a
 * newer build keeps attributes this version has never heard of instead of
 * dropping them. And it lets a user-defined attribute be the same kind of thing
 * as a built-in one — [custom] just makes an id with a prefix — so nothing in
 * the app has to special-case "one of ours" against "one of yours".
 */
@Serializable
@JvmInline
value class Attribute(val id: String) {

    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    companion object {
        const val CUSTOM_PREFIX = "custom:"

        fun custom(name: String) = Attribute(CUSTOM_PREFIX + name.trim())
    }
}

enum class AttributeGroup(val label: String) {
    /** Proteins and the common allergens. */
    PROTEIN("Proteins & allergens"),

    /** The fermentable carbohydrate groups. */
    FODMAP("FODMAP groups"),

    /** Everything else that commonly sets people off. */
    TRIGGER("Other triggers"),

    /**
     * How it was made rather than what it is — sourdough, wholegrain.
     *
     * These are chosen per meal rather than baked into an ingredient, because
     * one loaf can be wholemeal *and* sourdough *and* multigrain at once.
     */
    MADE("How it was made"),

    /** Yours. */
    CUSTOM("Your own"),
}

/**
 * What an attribute is called, what the word actually means, why it's worth
 * watching, and what trouble with it feels like.
 *
 * All four are shown in the app rather than kept here as developer commentary.
 * Half these words are chemistry — mannitol, GOS, casein, sulphite — and
 * nobody should have to leave a food diary and go searching to find out what
 * their own breakfast has just been labelled with.
 *
 * The division of labour between the three prose fields:
 *
 * - [what] is the dictionary answer. What the word means and where it turns up,
 *   in the plainest language that is still true.
 * - [note] is the thing worth knowing that you would not have guessed — that
 *   wheat carries fructans as well as gluten, that lactose falls away as cheese
 *   ages. This is the field that makes the difference between a useful trial
 *   and a wasted month.
 * - [symptoms] is what trouble with it tends to feel like. Blank means "the
 *   usual for its group", which is the honest answer for the five FODMAPs:
 *   they present alike, and inventing distinctions between them would be
 *   making things up.
 */
data class AttributeInfo(
    val attribute: Attribute,
    val label: String,
    val group: AttributeGroup,
    /** What the word means, in plain language. */
    val what: String = "",
    /** The non-obvious thing worth knowing. */
    val note: String = "",
    /** What trouble with it feels like. Blank falls back to the group. */
    val symptoms: String = "",
)

/**
 * What trouble with anything in this group tends to feel like.
 *
 * A group-level answer because for the FODMAPs it is genuinely the same answer
 * — they are all fermentable carbohydrates arriving undigested in the same
 * place — and writing five subtly different paragraphs would be inventing
 * distinctions that do not exist.
 */
val AttributeGroup.typicalSymptoms: String
    get() = when (this) {
        AttributeGroup.FODMAP ->
            "Bloating, wind, cramping, and stools that are looser or more urgent " +
                "than usual. Typically one to eight hours after eating, and " +
                "dose-dependent — a small amount is often fine where a large one " +
                "is not, which is why cutting something out entirely and cutting " +
                "it down are different experiments."
        AttributeGroup.PROTEIN ->
            "Depends on the mechanism. A true allergy is fast, involves the immune " +
                "system, and can affect breathing and circulation as well as the " +
                "gut — that is a doctor's problem, not a diary's. An intolerance " +
                "is slower, dose-dependent and uncomfortable rather than dangerous."
        AttributeGroup.TRIGGER ->
            "Usually gut symptoms within a few hours — bloating, cramping, reflux, " +
                "or a change in stools. Several of these irritate a gut that is " +
                "already unhappy rather than causing trouble on their own."
        AttributeGroup.MADE ->
            "Not something you react to in itself — it describes how a food was " +
                "made. It is here because the same grain, made two ways, can " +
                "affect you differently, and that difference is a real clue."
        AttributeGroup.CUSTOM ->
            "Yours to define. The app has nothing to add about it."
    }

object Attributes {

    // Proteins and allergens
    val Gluten = Attribute("gluten")
    val Wheat = Attribute("wheat")
    val Barley = Attribute("barley")
    val Rye = Attribute("rye")
    val Oats = Attribute("oats")
    val Dairy = Attribute("dairy")
    val Lactose = Attribute("lactose")
    val Casein = Attribute("casein")
    val Egg = Attribute("egg")
    val Soy = Attribute("soy")
    val Peanut = Attribute("peanut")
    val TreeNut = Attribute("tree-nut")
    val Sesame = Attribute("sesame")

    // FODMAP groups
    val Fructan = Attribute("fructan")
    val Gos = Attribute("gos")
    val ExcessFructose = Attribute("excess-fructose")
    val Sorbitol = Attribute("sorbitol")
    val Mannitol = Attribute("mannitol")

    // Other triggers
    val Allium = Attribute("allium")
    val Nightshade = Attribute("nightshade")
    val Caffeine = Attribute("caffeine")
    val Alcohol = Attribute("alcohol")
    val Capsaicin = Attribute("capsaicin")
    val HighFat = Attribute("high-fat")
    val Carbonation = Attribute("carbonation")
    val Histamine = Attribute("histamine")
    val Sulphite = Attribute("sulphite")
    val Sweetener = Attribute("artificial-sweetener")
    val Citrus = Attribute("citrus")
    val Fermented = Attribute("fermented")
    val SeedOil = Attribute("seed-oil")

    // Ways a food was made, rather than what it is. These arrive as modifiers
    // chosen at log time — "wholemeal sourdough" describes one loaf — and are
    // testable in their own right.
    val Sourdough = Attribute("sourdough")
    val Wholegrain = Attribute("wholegrain")
    val Refined = Attribute("refined")

    val known: List<AttributeInfo> = listOf(
        AttributeInfo(
            Gluten, "Gluten", AttributeGroup.PROTEIN,
            what = "A protein found in wheat, barley and rye. It is what makes bread " +
                "dough stretchy, and it is in anything made from those grains.",
            note = "Wheat also carries fructans, so cutting gluten cuts both at once. " +
                "That is the single most common reason a gluten-free trial gives an " +
                "answer nobody can interpret — see the fructan note.",
            symptoms = "If gluten is a problem it is worth knowing which kind. Coeliac " +
                "disease is an autoimmune condition, not an intolerance, and it needs " +
                "a blood test and usually a biopsy. The testing only works while you " +
                "are still eating gluten, so if you suspect it, get tested before you " +
                "cut it out rather than after. Non-coeliac sensitivity looks similar " +
                "in the gut — bloating, pain, loose stools, often tiredness and " +
                "foggy-headedness — but has no test, which is what a diary is for.",
        ),
        AttributeInfo(
            Wheat, "Wheat", AttributeGroup.PROTEIN,
            what = "The grain itself, rather than any one component. Bread, pasta, " +
                "couscous, most baking, and a great deal of sauce and stock thickening.",
            note = "Tracked separately from gluten so a gluten-free grain and a wheat " +
                "one can be told apart in the numbers. Wheat brings gluten, fructans " +
                "and its own proteins together, and only separating them says which " +
                "one matters to you.",
        ),
        AttributeInfo(
            Barley, "Barley", AttributeGroup.PROTEIN,
            what = "A grain, most often met as malt rather than as the grain itself — " +
                "beer, malt vinegar, malted drinks, and a lot of breakfast cereal.",
            note = "Including malt and most beer.",
        ),
        AttributeInfo(
            Rye, "Rye", AttributeGroup.PROTEIN,
            what = "A grain, in rye bread, pumpernickel, crispbread and some whiskies.",
            note = "Higher in fructans than wheat, so rye bread can be harder work " +
                "than an equivalent amount of ordinary bread.",
        ),
        AttributeInfo(
            Oats, "Oats", AttributeGroup.PROTEIN,
            what = "A grain that contains no gluten of its own.",
            note = "Oats are usually milled alongside wheat, so ordinary oats carry it " +
                "and certified gluten-free ones do not. If porridge is a problem, that " +
                "is the first thing to change.",
        ),
        AttributeInfo(
            Dairy, "Dairy", AttributeGroup.PROTEIN,
            what = "Anything made from milk: milk, cream, butter, cheese, yoghurt.",
            note = "Anything from milk, lactose or not.",
            symptoms = "Dairy is two separate suspects wearing one coat — lactose, the " +
                "sugar, and casein, the protein. They cause trouble by different routes " +
                "and lactose-free milk still contains casein, so \"dairy disagrees with " +
                "me\" is worth narrowing down.",
        ),
        AttributeInfo(
            Lactose, "Lactose", AttributeGroup.FODMAP,
            what = "The natural sugar in milk. Digesting it needs an enzyme called " +
                "lactase, and most adults worldwide make less of it after childhood. " +
                "That is the ordinary human condition rather than a disease.",
            note = "Falls away as cheese ages: parmesan and mature cheddar have almost " +
                "none, ricotta and cottage cheese have a lot. If dairy troubles you, " +
                "which cheeses do it is the most useful thing you can learn.",
        ),
        AttributeInfo(
            Casein, "Casein", AttributeGroup.PROTEIN,
            what = "The main protein in milk, as opposed to lactose, which is the sugar. " +
                "It is the part that sets into curds and makes cheese.",
            note = "Present whether or not the lactose is — which is how lactose-free " +
                "milk can still cause trouble.",
        ),
        AttributeInfo(
            Egg, "Egg", AttributeGroup.PROTEIN,
            what = "Hen egg, and the many things containing it: mayonnaise, most cake, " +
                "custard, fresh pasta, some breads glazed with it.",
            note = "White and yolk logged separately; the white is the usual culprit.",
        ),
        AttributeInfo(
            Soy, "Soy", AttributeGroup.PROTEIN,
            what = "Soya beans and what is made from them — tofu, tempeh, soy sauce, " +
                "soy milk, edamame, and soy protein in a lot of processed food.",
            note = "Firm tofu is low in FODMAPs because the liquid is pressed out; silken " +
                "tofu and whole soy beans are not. The same bean behaves differently " +
                "depending on what was done to it.",
        ),
        AttributeInfo(
            Peanut, "Peanut", AttributeGroup.PROTEIN,
            what = "A legume despite the name — botanically closer to peas and beans " +
                "than to almonds or walnuts.",
            symptoms = "Peanut is one of the allergies that can be immediate and serious, " +
                "involving the mouth, skin and breathing within minutes rather than the " +
                "gut over hours. That is a matter for a doctor and a test, not for a " +
                "food diary. This app is for the slow, dose-dependent kind of trouble.",
        ),
        AttributeInfo(
            TreeNut, "Tree nuts", AttributeGroup.PROTEIN,
            what = "Almond, cashew, walnut, hazelnut, pistachio, pecan, macadamia, brazil. " +
                "Also the milks, butters and flours made from them.",
            note = "Cashews and pistachios are high in FODMAPs where almonds and walnuts " +
                "are not, so a reaction to some nuts and not others may be about " +
                "fructans rather than the nuts.",
            symptoms = "As with peanut, a true nut allergy can be fast and serious and " +
                "belongs with a doctor. A diary is the right tool for the dose-dependent " +
                "kind of reaction, not the sudden kind.",
        ),
        AttributeInfo(
            Sesame, "Sesame", AttributeGroup.PROTEIN,
            what = "The seeds, and the tahini and oil made from them.",
            note = "Easily missed: hummus, dips, spice mixes, burger buns and a lot of " +
                "bread carry it without saying so prominently.",
        ),

        AttributeInfo(
            Fructan, "Fructans", AttributeGroup.FODMAP,
            what = "Chains of fructose sugars. Nobody has the enzyme to break them down, " +
                "so they travel to the large intestine intact and the bacteria there " +
                "ferment them — which produces gas. This is normal digestion, not a " +
                "fault; the question is only how much of it you are comfortable with.",
            note = "In wheat, rye, barley, onion, garlic and a fair bit of gluten-free " +
                "food too. A lot of what gets called gluten sensitivity is this — " +
                "which is why the two need separating rather than assuming.",
        ),
        AttributeInfo(
            Gos, "GOS", AttributeGroup.FODMAP,
            what = "Short for galacto-oligosaccharides: short sugar chains that behave " +
                "like fructans and are found in legumes. Chickpeas, lentils, beans, " +
                "soy beans. This is the reason beans have the reputation they do.",
            note = "Draining and rinsing tinned pulses genuinely lowers it — a fair " +
                "amount leaches into the liquid. Tinned and rinsed is meaningfully " +
                "gentler than cooked from dry.",
        ),
        AttributeInfo(
            ExcessFructose, "Excess fructose", AttributeGroup.FODMAP,
            what = "Fruit sugar, but only where there is more of it than the glucose " +
                "sitting alongside it. Glucose helps fructose across the gut wall, so " +
                "a fruit with both in balance is usually fine and one with a surplus " +
                "is not. That is why it is \"excess\" fructose rather than fructose.",
            note = "Apple, pear, mango, honey, high-fructose syrups. Ripeness matters, " +
                "and so does quantity — half a portion may pass where a whole one does not.",
        ),
        AttributeInfo(
            Sorbitol, "Sorbitol", AttributeGroup.FODMAP,
            what = "A sugar alcohol — a sweetener that occurs naturally in some fruit " +
                "and is also added to sugar-free products. Absorbed slowly and " +
                "incompletely, which is exactly why it causes trouble.",
            note = "Stone fruit, apple, pear, and sugar-free products. Sugar-free mints " +
                "and gum are a classic hidden source.",
        ),
        AttributeInfo(
            Mannitol, "Mannitol", AttributeGroup.FODMAP,
            what = "Another sugar alcohol, closely related to sorbitol and behaving the " +
                "same way. Found naturally in a handful of vegetables.",
            note = "Mushrooms, cauliflower, celery, snow peas.",
        ),

        AttributeInfo(
            Allium, "Allium", AttributeGroup.TRIGGER,
            what = "The onion and garlic family: onion, garlic, shallot, leek, spring " +
                "onion, chives.",
            note = "Worth its own attribute as well as the fructan tag, because chives " +
                "and the green tops of spring onions are allium without much fructan. " +
                "If the green parts are fine and the bulbs are not, that is fructans.",
        ),
        AttributeInfo(
            Nightshade, "Nightshade", AttributeGroup.TRIGGER,
            what = "A plant family: tomato, potato, aubergine, peppers, chilli, and the " +
                "paprika and cayenne made from them.",
            note = "Sweet potato is not one, despite the name. The evidence for " +
                "nightshade sensitivity is much thinner than for the FODMAP groups — " +
                "it is here because people report it, not because it is established.",
        ),
        AttributeInfo(
            Caffeine, "Caffeine", AttributeGroup.TRIGGER,
            what = "The stimulant in coffee, tea, cola, energy drinks and chocolate.",
            note = "A gut stimulant in its own right, separately from anything it's " +
                "dissolved in. Decaf coffee still has some, and still has the other " +
                "compounds in coffee that speed things up.",
            symptoms = "Speeds up the bowel and can loosen stools within an hour. Also " +
                "reflux, and disturbed sleep from a dose much earlier in the day than " +
                "most people expect — which then shows up as a bad night rather than " +
                "as a gut symptom.",
        ),
        AttributeInfo(
            Alcohol, "Alcohol", AttributeGroup.TRIGGER,
            what = "Beer, wine, spirits and anything cooked with a lot of them.",
            note = "Irritates the gut lining and speeds transit on its own, on top of " +
                "whatever else is in the drink — beer brings barley and fructans, wine " +
                "brings sulphites and histamine.",
        ),
        AttributeInfo(
            Capsaicin, "Chilli heat", AttributeGroup.TRIGGER,
            what = "The compound that makes chilli hot. It works by triggering the same " +
                "nerve receptors that respond to actual heat, which is why it feels " +
                "like burning without anything being burnt.",
            symptoms = "Cramping and urgency, and burning on the way out as well as on " +
                "the way in. Unpleasant rather than harmful for most people, and it " +
                "does not damage anything.",
        ),
        AttributeInfo(
            HighFat, "High fat", AttributeGroup.TRIGGER,
            what = "A large amount of fat in one sitting — fried food, pastry, creamy " +
                "sauces, a lot of cheese.",
            note = "Slows the stomach down and can look exactly like a food intolerance. " +
                "If a rich meal upsets you, the fat is a likelier explanation than " +
                "anything exotic in it.",
        ),
        AttributeInfo(
            Carbonation, "Carbonation", AttributeGroup.TRIGGER,
            what = "The dissolved gas in fizzy drinks, beer and sparkling water. It " +
                "arrives as gas and leaves as gas.",
            note = "A common and very easily missed cause of bloating — sparkling " +
                "water gets treated as if it were water.",
        ),
        AttributeInfo(
            Histamine, "Histamine", AttributeGroup.TRIGGER,
            what = "A compound that builds up in food as it ages, ferments or cures, and " +
                "which the body also releases itself. Some people break down dietary " +
                "histamine slowly and accumulate it.",
            note = "Aged, cured and fermented things, plus wine and some vegetables. " +
                "Leftovers gain histamine in the fridge, so the same meal can be fine " +
                "on the day and not on the third day.",
            symptoms = "Often not just the gut: flushing, headache, a blocked or runny " +
                "nose, itching. Cumulative rather than one-meal — a threshold you " +
                "cross over a day rather than a single dose. Genuinely contested " +
                "territory medically, so treat a pattern here as a lead, not a finding.",
        ),
        AttributeInfo(
            Sulphite, "Sulphites", AttributeGroup.TRIGGER,
            what = "Preservatives, written on labels as sulphur dioxide or E220 to E228. " +
                "They stop things browning and keep wine stable.",
            note = "Wine, dried fruit, some pickles and vinegars.",
            symptoms = "Wheeziness and a tight chest as often as gut trouble, and more " +
                "likely in people who already have asthma.",
        ),
        AttributeInfo(
            Sweetener, "Sweeteners", AttributeGroup.TRIGGER,
            what = "Anything sweetening without sugar. The sugar alcohols — sorbitol, " +
                "mannitol, xylitol, maltitol, isomalt, anything ending in -ol — are " +
                "the ones that reliably cause gut trouble.",
            note = "Sugar-free gum and mints are a classic hidden cause. A few pieces " +
                "a day is a dose most people would never think to write down.",
        ),
        AttributeInfo(
            Citrus, "Citrus", AttributeGroup.TRIGGER,
            what = "Orange, lemon, lime, grapefruit, mandarin, and their juices and zest.",
            note = "Low in FODMAPs, so if citrus bothers you the acid is the likelier " +
                "cause than the sugars — which points at reflux rather than fermentation.",
        ),
        AttributeInfo(
            Fermented, "Fermented", AttributeGroup.TRIGGER,
            what = "Food deliberately cultured with bacteria or yeast: sauerkraut, kimchi, " +
                "kombucha, miso, live yoghurt, sourdough.",
            note = "Cuts both ways. Fermenting breaks down some FODMAPs, which helps, and " +
                "builds histamine, which does not. Which effect wins depends on you.",
        ),
        AttributeInfo(
            SeedOil, "Seed oils", AttributeGroup.TRIGGER,
            what = "Oils pressed from seeds rather than fruit — canola, sunflower, " +
                "soybean, corn, grapeseed, rice bran — as against olive, coconut or butter.",
            note = "Also most mayonnaise and margarine, and anything fried out of the " +
                "house, which is where they're easiest to miss.",
            symptoms = "There is no established intolerance to these the way there is for " +
                "lactose or fructans. It is tracked because it is tracked — if your own " +
                "log shows a pattern, that is your evidence, and it is worth exactly as " +
                "much as any other pattern in here.",
        ),

        AttributeInfo(
            Sourdough, "Sourdough", AttributeGroup.MADE,
            what = "Bread raised with a live starter over a long ferment, rather than " +
                "with quick baker's yeast.",
            note = "A long ferment breaks down some of the fructans in wheat, so sourdough " +
                "and ordinary bread can affect people differently even though both " +
                "are wheat. Worth tracking on its own: if sourdough is fine and " +
                "sandwich bread isn't, that points at fructans rather than gluten.",
        ),
        AttributeInfo(
            Wholegrain, "Wholegrain", AttributeGroup.MADE,
            what = "Made with the whole grain, bran and germ included, rather than just " +
                "the starchy middle.",
            note = "More fibre, and more fructan than the refined version of the same grain.",
        ),
        AttributeInfo(
            Refined, "White / refined", AttributeGroup.MADE,
            what = "Made with the starchy part of the grain only, with the bran and germ " +
                "milled out.",
            note = "Lower in fibre and lower in fructans than the wholegrain version, " +
                "which is why white bread is sometimes the easier one.",
        ),
    )

    private val byId: Map<Attribute, AttributeInfo> = known.associateBy { it.attribute }

    fun info(attribute: Attribute): AttributeInfo? = byId[attribute]

    /**
     * A readable name for any attribute, including ones this build doesn't know.
     *
     * An unknown id from a newer export shows as itself rather than blank — a
     * strange-looking chip is recoverable, an invisible one isn't.
     */
    fun label(attribute: Attribute): String = when {
        attribute.isCustom -> attribute.id.removePrefix(Attribute.CUSTOM_PREFIX)
        else -> byId[attribute]?.label ?: attribute.id
    }

    fun group(attribute: Attribute): AttributeGroup = when {
        attribute.isCustom -> AttributeGroup.CUSTOM
        else -> byId[attribute]?.group ?: AttributeGroup.CUSTOM
    }

    fun note(attribute: Attribute): String = byId[attribute]?.note.orEmpty()

    /** The plain-language explanation, or blank for one this build doesn't know. */
    fun what(attribute: Attribute): String = byId[attribute]?.what.orEmpty()

    /**
     * What trouble with it feels like, falling back to the group.
     *
     * The fallback is the honest answer rather than a gap: the five FODMAPs
     * genuinely present alike, and five subtly different paragraphs would be
     * inventing distinctions.
     */
    fun symptoms(attribute: Attribute): String =
        byId[attribute]?.symptoms?.takeIf { it.isNotBlank() }
            ?: group(attribute).typicalSymptoms

    /** Known attributes in display order, grouped. */
    fun byGroup(): Map<AttributeGroup, List<AttributeInfo>> = known.groupBy { it.group }
}
