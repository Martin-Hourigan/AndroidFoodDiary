# AndroidFoodDiary — Plan

A food and symptom diary built for one job: working out which foods are causing
digestive trouble, and testing that properly rather than guessing.

Same shape as the other two apps — Kotlin, Compose, Material 3, `dev.mahourigan.*`, a
pure-Kotlin domain layer under JVM unit tests, one JSON file on device, no
account and no network.

**Where it's up to:** steps 1 and 2 of the build order are done and installable —
a working diary, and the tagged ingredient library with its editor. See
`TESTING.md`. Trials, insights and reminders (steps 3–5) are still ahead.

## The thesis

Most food diaries are a text box and a date. They produce a pile of notes nobody
can draw a conclusion from, because "pasta" isn't a cause — wheat is, or fructans
are, or it was the garlic in the sauce.

So this app does two things, in order:

1. **Observe.** Log meals down to the ingredient, log symptoms with a time. After
   a few weeks, rank the suspects — by ingredient *and* by attribute (gluten,
   dairy, fructan, allium…), over realistic delay windows.
2. **Test.** Turn a suspect into a trial: cut it for a fixed period, let the app
   police adherence, then reintroduce it deliberately and see what happens.

Observation generates hypotheses. Only the trial answers anything. The app is
built so the second step is one tap from the first.

### Why attributes, not just foods

This matters specifically for the gluten question. Wheat carries **both** gluten
and fructans, and a large share of self-diagnosed gluten sensitivity turns out to
be the fructans — which are also in onion, garlic, and plenty of gluten-free
food. Cutting gluten cuts both at once, so "gluten-free helped" doesn't tell you
which one helped.

Tagging ingredients with attributes lets the app pull them apart: gluten-free
bread is fructan-ish but gluten-free; a wheat cracker with no onion is gluten
without much fructan. And when the two are hopelessly tangled in your data, the
app says so and suggests the meal that would separate them (see *Confounding*).

### Every chip explains itself

Tap any attribute chip on the day screen and a card comes up over it: what the
word means, the thing worth knowing about it, and behind one more tap, what
trouble with it feels like. An X in the corner closes it, and so does tapping
the card, or anywhere outside it.

The X replaced a "Close" button along the bottom, which shared a row with
"Symptoms of intolerance" — four times as wide — and lost, wrapping its own
five-letter label onto two lines. An icon cannot wrap, and the one button left
now has that row to itself.

This is not a nicety. Half these words are chemistry — mannitol, GOS, casein,
sulphite, capsaicin — and an app that labels somebody's breakfast "mannitol"
and then leaves them to go and search for it elsewhere has handed them
homework. The whole value of the attribute model is that it names the thing
underneath the food, and a name nobody understands names nothing.

Three fields per attribute, and the division of labour is the point:

- **What it is.** The dictionary answer, in the plainest language still true.
  Capped at 400 characters by a test, because it pops up over the day you were
  reading and anything longer wants a screen of its own.
- **The note.** The thing you would not have guessed — that wheat carries
  fructans as well as gluten, that lactose falls away as cheese ages, that
  rinsing tinned pulses genuinely lowers the GOS. This is the field that makes
  the difference between a useful trial and a wasted month.
- **Symptoms**, behind the button, because it is the longest and the least
  often wanted. Blank falls back to a group answer, which for the five FODMAPs
  is the honest response: they present alike, and writing five subtly
  different paragraphs would be inventing distinctions.

Three of these carry something the app has an obligation to say, and a test
holds each one in place rather than trusting a future edit:

- **Gluten** says get tested for coeliac disease *before* cutting it out.
  Serology only works while you are still eating gluten, and this app is
  otherwise a standing encouragement to run exactly the elimination that
  destroys the evidence. Coeliac is autoimmune, not an intolerance.
- **Peanut and tree nuts** say an allergy is a doctor's problem. Minutes and
  breathing, not weeks and a pattern — the one place a food diary is the
  wrong tool, and it should say so rather than quietly accept the entry.
- **Seed oils** say there is no established intolerance to them. It is tracked
  because it was asked for, and a correlation from a real log is real
  evidence; dressing it up as settled science would not be.

The `+N` on a crowded row now reveals the rest instead of being a count. You
cannot tap what is not drawn, so a permanently hidden chip would have been a
permanently unexplainable one.

## Domain model

Everything below lives in `domain/`, free of Android imports, so the parts most
likely to be subtly wrong — window arithmetic, adherence, scoring — run in plain
JVM tests.

### Ingredient — the library

```kotlin
@Serializable
data class Ingredient(
    val id: String = "",
    val name: String = "",                        // "sourdough bread"
    val attributes: Set<Attribute> = emptySet(),  // gluten, wheat, fructan, ...
    val aliases: List<String> = emptyList(),      // search: "aubergine" finds "eggplant"

    /** What one of this thing normally is: "slice", "g", "clove", "egg". */
    val defaultUnit: String? = null,

    /** A normal serving in [defaultUnit] — the midpoint the coarse buckets sit around. */
    val typicalAmount: Double? = null,

    val kcalPer100: Double? = null,   // per 100 ml for anything liquid
    val gramsPerUnit: Double? = null, // a slice is 40 g, a clove is 3 g

    val isUserCreated: Boolean = false,
)
```

`Attribute` is a sealed set: a curated list of the things worth eliminating, plus
`Custom(name)` so you can add your own without a code change.

| Group | Attributes |
|---|---|
| Proteins / allergens | gluten, wheat, barley, rye, oats, dairy, lactose, casein, egg, soy, peanut, tree nut, fish, shellfish, sesame |
| FODMAP groups | fructan, GOS, lactose, excess fructose, sorbitol, mannitol |
| Common triggers | allium, nightshade, caffeine, alcohol, capsaicin, high fat, carbonation, histamine, sulphite, artificial sweetener, citrus, seed oils |
| Yours | `Custom("cheap red wine")` |

Attributes are **inherited upward automatically**: a meal has an attribute if any
ingredient in it does. You never tag a meal by hand, which is the whole point —
hand-tagging is where diaries go wrong, because the day you forget the soy sauce
had wheat in it is the day the data starts lying to you.

### Portion — optional, then coarse, then exact

```kotlin
@Serializable
data class MealItem(
    val ingredientId: String = "",
    val portion: Portion? = null,     // null = just "avocado"
)

@Serializable
data class Portion(
    val size: PortionSize? = null,    // Little | Normal | Lots
    val amount: Double? = null,       // finer still
    val unit: String? = null,         // "slice", "g", "ml", "clove"
)
```

Three levels, and **you never have to climb past the first**:

```
avocado                     nothing said
avocado — lots              coarse
sourdough × 1 slice         exact
garlic × 2 cloves
```

Tapping an ingredient puts it in the meal and that is the end of the interaction.
Portion lives behind a tap on the item, so the detail is there when a meal is
worth being careful about and absent the rest of the time. Most days it will be
absent, and that is fine.

Crucially, **unspecified is stored as absent, never as "Normal"**. Defaulting it
would invent a portion you never gave and then let the analysis reason from it —
the app would be making things up.

The analysis handles that cleanly, because the two questions need different
things. *Presence* — "was there gluten in this meal?" — is what the whole trigger
investigation runs on, and it works perfectly with bare `avocado`; a portionless
item counts fully. Only *dose-response* — "is a little sourdough fine but a
sandwich not?" — needs a portion, so it runs over just the exposures that have
one and says how many that was: **"of your 23 wheat meals, 9 have a portion
recorded."** If you want to answer a dose question you start recording portions
for that one ingredient, and it tells you when there's enough.

When `amount` is given, the coarse bucket is derived from it against the
ingredient's `typicalAmount` (≤ ½ → Little, ≥ 2× → Lots), so all three styles mix
freely in one dataset and one axis serves the lot.


## Calories

Deliberately the same shape as portions: three levels, each overriding the one
below, and you never have to climb past the first.

```
Ingredient    kcal per 100 g  +  what one unit weighs   → estimated from the usual serving
An amount     "40 g of cheddar"                         → calculated
A number      typed on the meal, or on a recipe          → used as-is
```

Per-100 is the canonical figure because it's what's printed on the packet and the
only form that works when you give an exact weight. `gramsPerUnit` — a slice ≈
40 g, a clove ≈ 3 g — is what turns "2 slices" into calories. All 333 bundled
ingredients carry both, so **every meal has a total without a single extra tap**.

A recipe can carry its own figure, for something you cooked and worked out once.
When it does, its ingredients are skipped rather than counted again — the point
of putting a number on a recipe is that it stops being re-estimated.

### Serves

A cooked dish is written down the way it's cooked — two tins of beans, 250 g of
tofu — and then eaten a quarter at a time. `Recipe.serves` closes that gap.
Without it the app logs the whole pot: four dinners' worth of GOS every time you
have one, and a calorie total four times too big.

Logging a recipe adds **one serving**, with the amounts divided. Null means one,
so anything written as a single serving keeps working without being told.

Three details that matter more than they look:

**The Basis snapshot holds the serving too**, not the pot. Its job is to record
what this meal took from the recipe — and if it held the pot while the items
held a serving, every logged meal would read as customised the moment it was
added, which is the bug that had the app claiming you'd changed a recipe you
hadn't touched.

**Coarse portions are left alone.** A quarter of "a little" is not something
anyone can picture, and the bucket was never a quantity to begin with.

**Nothing writes a serving back over a recipe.** It would have to scale up
again, and getting that wrong divides the recipe by its own serving count every
time — so the log simply has no path to editing a recipe at all.

### The box arrives filled in

The calorie field is pre-filled with what the ingredients add up to, rather than
sitting empty behind a ghosted placeholder. You can type over it; that's the
third level.

The catch, and the reason this isn't just a default value: if leaving the number
alone stored it as an override, **every meal would become a stated total.** The
honest `~` would disappear from meals that were only ever estimated, and fixing
an ingredient's energy figure later would stop reaching the meals that used it —
they'd each be carrying a frozen copy of the old sum.

So a typed number counts as *yours* only when it differs from the pre-filled
one. Left as it arrived, it means exactly what an empty box used to mean. Same
rule as the ingredient editor, for the same reason.

[Energy.amountIn] exists so the field and the labels can't drift: `describe()`
is built on it, so the box can never read 4900 under a day total reading 4911.

### Why every total is hedged

The bundled figures are estimates from general knowledge, not a database: the app
has no network, so there's nothing to look anything up in. A "slice" of bread is
anywhere from 25 g to 55 g. A meal assembled from typical servings is easily
±20%.

So the display never pretends otherwise. An estimate reads `~280 kcal`, rounded
to ten, because the last digit of a guess is noise dressed as information; a
figure you typed reads `280 kcal`. The day total says which it is, and says how
many things had no data rather than quietly counting them as zero — a total
that's silently short is worse than no total.

**The thing to watch.** Portions are optional by design, because demanding them
on every item is how food diaries get abandoned. Calories reward precise
portioning, so this feature pulls the other way. If it ever makes logging heavy
enough that you skip days, that costs the symptom analysis — which is what the
app is actually for — and the calories should go.

### Calories or kilojoules

A display setting, nothing more. Everything is stored as kcal whichever way it's
set, because one stored unit is the only way the log can't end up half in one and
half in the other — and because per-100-g kcal is what most packets here print.

Switching converts what's shown *including the box you type into*, so a figure
entered in kJ is converted once on the way in rather than sitting in the file in
a second unit. A field you haven't touched is written back untouched: converting
a rounded number in both directions would nudge it a little every time the screen
was opened, and a stored value that drifts because you looked at it is the kind
of bug nobody ever tracks down.

The estimate rounding widens with the unit — nearest 10 kcal, nearest 50 kJ — so
`~2100 kJ` reads as exactly as rough as `~500 kcal` does. Rounding kJ to ten
would imply a precision the estimate has never had.

## The first screen of the picker

No tabs. Every chip is the same kind of thing — a door you tap to narrow down —
and the last one holds everything:

```
Toast >   Porridge >   Coffee >   Cafe >   Toasts >   Recipes >
```

Toast, Porridge and Coffee are categories: open one and it asks you about the
bread and what went on it. Cafe and Toasts are **tags**. **Recipes** is the door
to all of them.

The overlap is deliberate. A tag is a shortcut to a subset and Recipes is the
complete list, so a café order is reachable both ways — the same relationship an
"All" tab has with the filtered ones beside it. Add Soups or Stews and they slot
in as more shortcuts without the full list moving.

### The usual, or build one

Opening Toast used to go straight to the questions, which you answered every
morning even though it's the same toast most mornings. Now a category opens on
**the usual** — the saved things tagged with its name — with a switch to
**build one** instead.

Two named halves rather than one unlabelled button. A toggle can say which side
you're on *and* that there is another one; a lone pill in the breadcrumb can say
neither, and its label has to change meaning under your thumb to get you back.
The cost is a row of height on every category, paid on every open, and it buys
discoverability.

A `SingleChoiceSegmentedButtonRow`, not two chips side by side. The difference
isn't decoration: separate chips read as two independent things you could tick,
where one joined control reads as a single thing with two positions — which is
what it is.

The toggle only appears when something is saved. With none, it would be a switch
over an empty list, so `buildingCategory` is nullable: undecided means "usuals
if there are any, questions if not".

**Where "the usual" comes from: a recipe tagged with the category's name.** No
new field and no inference. Two alternatives were rejected — matching the
category's ingredients is exactly the kind of guessing that put onion in the
café, and recording which category a recipe was built in means new provenance
state that can drift.

That in turn means a tag matching a category's name is **absorbed** by it. Toast
is already a chip on the first screen; a second Toast chip for the tag would be
two doors to one idea, and the recipes behind it are precisely what "the usual"
means inside the category. Committing a category also pre-selects its name as a
tag on save, so a toast you build and keep turns up as a usual next time without
being told twice.

### Membership is declared, never inferred

Both [Recipe.tags] and [Ingredient.tags] draw on one namespace, and a thing is
behind a chip because it carries that tag. Nothing else.

That distinction does real work. A café order is a **whole recipe** — Chilli
scramble goes behind Cafe and the sourdough inside it does not, because nobody
orders a slice of sourdough. A hash brown is a **single ingredient** tagged the
same way, and sits beside the orders. Neither is derived from the other.

This replaced a scoring system that guessed: co-occurrence with the tag's
recipes, plus *their ingredients*, plus bundled hints, against a relevance
floor. Including the ingredients made the inference transitive, and transitive
inference over food is nonsense — "Mushrooms on toast" contains garlic, so one
dinner of garlic and onion put onion, tomato, tofu and miso paste in the café.
One shared ingredient dragged in everything ever eaten beside it.

Guessing membership was the whole mistake, so nothing guesses now. Gone with it:
the co-occurrence scoring for folders, the `suggests` hints, the relevance floor,
the "components before toppings until you've started" ordering, and one level of
folder nesting. Ranking still decides the *order* within a chip — it just no
longer decides who gets in.

### What else went

Whole recipes no longer sit loose beside Toast, which is what made the very
first version of this screen unreadable. And there is no third place to look:
Build, Recent and Saved were three tabs answering "where do I tap", and the
answer is now always "one of these chips".

The Recipes door is a reserved tag rather than a separate mode, so browsing, the
breadcrumb and the back button all work through one mechanism.

`Recent` went with the tabs. "Recently eaten" is a ranking, and these lists are
already ranked by habit and recency — it didn't need a room of its own.

## Keeping a meal you built

Two different questions, so two different controls.

One question, or none:

| Draft | Question |
|---|---|
| Built from ingredients | "Save this as a meal" |
| A saved meal you changed, or two combined | "Save this version as a new meal" |
| A saved meal, untouched | **none** |

**A recipe cannot be edited from here.** There used to be a third answer —
update the recipe you started from — and it's gone deliberately. Editing a saved
meal belongs in Saved meals, not behind a radio button you meet while recording
your dinner, where one wrong tap rewrote the recipe. Removing it also removed a
whole class of bug: because a draft holds one serving and a recipe holds the pot,
that path had to scale back up on every write or it would divide the recipe by
its own serving count each time. No path, no arithmetic to get wrong.

With that gone both remaining cases are a yes/no, so both are a checkbox rather
than a pair of radio buttons pretending to be a choice.

That last row is the one that got broken. Adding the checkbox meant widening the
card's condition from "customised" to "has any items", which made logging an
unchanged saved meal announce *"You changed Creamy tomato and basil beans"* and
offer to overwrite it. Asking what to do about a change nobody made is the app
inventing work, and one of the answers on offer would have damaged the recipe.

The decision now lives in `MealDraft.saveCard` as a three-valued property rather
than as conditions inline in the composable, so it can be tested — and it is,
including the case that regressed.

The folder is free text with the existing folders offered as chips, and the chips
fill in the same box rather than being a separate control. That way making a new
folder is just typing one, and there's no "new folder" mode to enter and leave.
The current meal type is offered alongside them, because "Breakfast" is the
folder people reach for first and having to type it would be silly.

Leave the name blank and it takes one built from the ingredients — "Wheat bread,
wholemeal, Vegemite". Not elegant, but the alternative is ticking a box and
having nothing saved because a field was empty, which is a worse thing to
discover a week later. What it will use is shown under the field rather than as a
placeholder, since Material only reveals a placeholder once the field has focus.

Saving is a snapshot, the same rule as everywhere else here: the recipe gets a
copy of the items, so editing it later can't reach back and change what a logged
meal says you ate.

## Colour and lettering

Two independent choices, plus a custom palette. They began as one — five whole
identities, on the reasoning that what separates them is mostly the typography
and swapping only colours would give five near-identical apps. That reasoning
was right about the *defaults* and wrong as a rule: the only way to read the
diary in Petrock was to accept Oblivion's browns.

So the five survive as colour themes, the faces became a list of seven picked
in two slots, and the old pairing survives as `TypeFace.pairFor` — which is
both what each theme still starts you on and what a diary written before the
split is read back with.

| Colour theme | Ground | Severity | Used to imply |
|---|---|---|---|
| **Herbarium** | sage | specimen plum | Fraunces / Archivo |
| **Vellum & Quill** | parchment | rubric red | IM Fell |
| **Grimoire** | charred, dark | sealing wax | Grenze Gotisch / Cardo |
| **Herbal** | rag paper | earth red | EB Garamond |
| **Cyrodiil** | Oblivion's tan | health-bar crimson | Kingthings Petrock |
| **Custom** | yours | derived | — |

Any of the seven faces can take either slot, including the ones nobody would
pair on purpose. Blackletter body text earns a line of warning under the list
and is then set exactly as asked, which is the rule everywhere on this screen.

Three things make this work rather than being a skin:

**Severity reads off the scheme.** `outline → primary → tertiary → error` is the
mild-to-very-severe ramp, so every palette has to make those four a legible
climb rather than four pleasant colours. It's the one piece of colour with a job
rather than a mood, and designing each style against that constraint is what
stops any of them going decorative.

**The type scale rides on the face.** Petrock and IM Fell both run small and
light for their point size; left on Archivo's numbers the ingredient lines
would quietly become unreadable. The correction used to live on the style,
which is what made the two inseparable — moving it onto `TypeFace.scale` is
what let headings and body text come apart at all. Petrock's 18% now follows
Petrock into any pairing without dragging a grotesque up with it, and headings
and body scale independently off their own faces.

**Paper is drawn, not shipped.** A flat fill would make the parchment styles a
colour swap, and parchment is uneven translucency. A handful of soft radial
washes get drawn behind everything, so it scales to any screen and costs no
bytes.

### The pickers

Every row is the thing it is offering. A colour row is painted in its own
ground, ink and severity ramp; a face row sets both its name *and* its
description in that face, at the size that face will actually be set at. One
word in a typeface tells you almost nothing — what you need to judge is a
sentence of it, which is exactly what the description is. Rows come out at
different heights because the scales genuinely differ.

The colour rows are set in whichever lettering is currently chosen, not in
their own. That is the point of the split: those rows are about colour now, and
five different faces there would say the opposite.

**Text size** — Small, Medium, Large — multiplies both faces' own scales rather
than setting point sizes, so a pairing keeps its internal proportions at every
setting and Petrock keeps the 18% it needs over Fraunces. Medium is exactly 1, which a test pins: anything else quietly
nudges the type for everyone who never opens the setting. Small and Large move
by 12% and 18%, inside a band the same test enforces — under about 10% nobody
can tell it did anything, over about 25% and rows reflow into each other.

It multiplies with the phone's own font scale rather than replacing it, because
everything is in `sp`. That is the right way round: the system setting follows
you between apps, and this is a nudge on top for a diary read at arm's length
on a kitchen bench. The style picker previews honour it too, so the sample is
the size you will actually read.

### Custom: three swatches and about twenty derived

Paper, ink and one accent. Everything else — surfaces, containers, outlines,
the hairline rule — is faded out of those three, because `primaryContainer`
against `onSecondaryContainer` is not a choice anyone can hold in their head,
and nineteen pickers means either a very long screen or a diary you cannot
read.

The split that matters is **what is guaranteed and what is yours**:

- *Guaranteed.* The severity ramp and the live-row edge are relit to fixed
  contrast targets against whatever paper you chose. They carry meaning rather
  than mood — a severity band that has stopped climbing has stopped saying
  anything — so they are not left to chance. Error stays red however green the
  accent is, or a green-accented diary would make every warning look like a
  confirmation. Derived text (`onPrimary` and friends) falls back to plain
  black or white when neither your ink nor your paper is legible on it: your
  colours on your surfaces are your call, but a colour nobody chose on a
  surface nobody chose has no such excuse.
- *Yours.* Text contrast. Checked, reported in numbers rather than adjectives
  — "3.1:1", not "a bit faint" — and then applied exactly as picked. **Use it
  anyway** silences the warning; it never gates the colours.

Choosing contrast first and hue second is what finally settles an argument this
app has had before. Four hand-picked severity colours kept producing ramps that
dipped in the middle, because an amber at "moderate" is brighter than a red at
"severe" — yellow hues carry far more luminance. Fixed ratios cannot dip.

Two things only a test would have caught, both found by one:

- A **mid-grey paper** has almost no contrast headroom — #808080 is 0.216 in
  relative luminance, not 0.5, and tops out near 5:1 in the better direction.
  Asking for 9:1 there returns white four times over, which is a ramp that has
  stopped climbing. The targets are now fitted to the reach the paper actually
  has and spread geometrically beneath it.
- **Near-black paper with dim grey ink** made every derived label illegible at
  2:1 whichever of the two it picked. Hence the black-or-white fallback.

Custom has no light and dark twin. You chose the ground, so the ground decides,
and the mode switch is hidden rather than left present and inert.

Ten built-in palettes: each of the five themes in both modes, chosen in
Settings as **Match phone /
Light / Dark**.

This was originally one mode per style, on the argument that Grimoire isn't
Herbarium after dark. That conflated two things. *Between* styles the difference
is the typography, and no colour change makes Grimoire into Herbarium — but
*within* a style, light and dark genuinely are a colour swap, because the
typefaces don't move. Dark Herbarium is still recognisably Herbarium. So the
mode is a second setting rather than five more styles, and `typeFor(style)` is
deliberately mode-independent to keep that true.

### Severity has its own ramp

Severity used to read off `primary` / `tertiary` / `error`. Convenient and
wrong: those slots are also the accent, so they can't be tuned as a series, and
in Herbarium two of them landed at nearly the same luminance. A plum and a red
at the same brightness are different colours to most eyes and the *same* colour
to a colour-blind one, which makes the band decoration.

So [DiaryPalette] carries an explicit four-step ramp, and every one of the ten
climbs in **brightness** — darker with severity on a light ground, lighter on a
dark one — so the band reads as intensity even in greyscale.

Five properties are asserted across all ten palettes in `PaletteTest`, because
hand-picked colour has no compiler and fifty scheme entries are too many to
eyeball: ink is 7:1 on its ground, each severity step clears 3:1 (mild is held
to 2:1 on purpose — "barely anything" should be quiet), the ramp climbs
monotonically, a dark ground is actually darker than its light twin, and the
live-row edge clears 2:1.

Those tests caught four real ramp defects, three of them the same mistake: **an
amber or gold at "moderate" is brighter than a red at "severe"**, because yellow
hues carry far more luminance than reds at the same apparent saturation. Gold
sitting below red in a ramp has to drop to bronze first.

The fonts are bundled (2.7 MB): the app has no network permission, so
downloadable fonts aren't an option, and that's the right trade. Everything
except IM Fell and Petrock is a variable font, one file covering every weight.
Kingthings Petrock is the typeface Oblivion's menus actually use, free for any
purpose per its author's EULA on condition the .ttf is unmodified — which rules
out subsetting it.

## Roles, and why they aren't attributes

Two vocabularies sit on an ingredient and they must not be confused:

| | Answers | Reaches the analysis |
|---|---|---|
| **Attribute** | could this be the problem — `gluten`, `fructan`, `high-fat` | yes, it *is* the analysis |
| **Role** | would anyone put this on toast — `spread`, `fruit`, `cheese` | never |

Roles exist for one reason: a category's list should fill itself in. Peanut
butter belongs on toast, in porridge, and for some people in a salad. Without
roles that means writing it into three lists by hand and into every list invented
later; with them it's tagged `spread` once and every category that subscribes to
spreads offers it.

A subscription is *eligibility*, not the menu. A facet's own options are the
shortlist — what you actually reach for, in the order you reach for it — and the
role matches sit behind **More**, because "everything tagged vegetable" is eighty
chips and burying butter under them helps nobody.

The shortlist grows by consent. Search for something while a category is open and
the app asks whether to keep it there: that is exactly the moment you find out
the list is missing something, and it's one tap rather than a trip to the editor.

### Chips, with a search box

Options stay chips in a wrapped row — one per line was tried and reads as a
form rather than something you tap through. What a long list needed wasn't a
different layout, it was **a search box per section**, on any facet holding more
than eight options. Toast's Ingredients gets one; Bread, at eight, doesn't,
because a search box over eight breads is furniture.

The lists are **alphabetical**. The seeded order was a guess at what you reach
for most, and a guess is worse than a rule you can predict once a list is long
enough to scan. Searching a section covers the shortlist and the role overflow
together, and hides the "more" chip while it does — offering to show more when
you're already looking at everything would be a lie.

Two things a long list breaks if you let it. A pick made from the overflow is
pinned into the shortlist once the overflow closes, or collapsing it looks like
the tick was thrown away. And [Category.resolve] takes the library, because an
overflow option is built at display time and isn't in `Facet.options` — without
that it ticks on screen and never reaches the meal, which is exactly the bug the
first version shipped with.

### Where a hover state goes on a phone

Oblivion's menus mark the row under the cursor with an amber wash. A phone has
no cursor and no hover, so that language needs somewhere real to live, and there
are three candidates:

1. **The live row** — an open symptom episode. Not selection and not hover:
   "this is the current thing and it's waiting on you". A bloating episode that
   started at 2am and is still running is the one row on Today that is genuinely
   active. This is where it went.
2. **Selected chips.** Persistent selection is the phone's version of leaving
   the cursor somewhere, and Material already spends a container colour on it.
3. **Press feedback.** The literal hover-to-press translation, and the weakest:
   it lasts 200ms on a screen you open four times a day.

Every style defines its own `liveTint` and `liveEdge`, so this isn't a Cyrodiil
feature — Grimoire glows gold, Herbarium tints sage, Vellum uses rubric red. The
edge matters as much as the wash: colour alone can't carry "this is still open",
because it fails in sunlight and for anyone colour-blind, so the bar's
*presence* is the signal and its colour is only the decoration.


**The rule that keeps this from sprawling.**
 A role earns its place only if it
changes what some category offers. A test asserts no role id is also an attribute
id — that collision is how someone later reads one as the other — and another
asserts every subscription actually matches something, so a dead chip can't
accumulate in the editor.
### MealEntry — and the customisation rule

```kotlin
@Serializable
data class MealEntry(
    val id: String = "",
    val at: LocalDateTime = ...,          // wall clock, like Task.remindAt — 08:00 stays 08:00 abroad
    val mealType: MealType? = null,       // defaulted from the clock, always overridable
    val bases: List<Basis> = emptyList(), // the saved things it was built from
    val items: List<MealItem> = emptyList(),  // ingredient + optional portion — FULLY RESOLVED
    val notes: String = "",
)

/** A saved thing as it stood when you logged, kept so the +/− diff stays truthful. */
@Serializable
data class Basis(
    val recipeId: String = "",
    val name: String = "",
    val itemsAtLogTime: List<MealItem> = emptyList(),
)
```

**A list of bases, not one**, because most meals are assembled: toast *and* a
topping, curry *and* rice, dinner *and* a glass of wine. With a single basis
you'd need a saved recipe for every combination you ever eat, which is how a
library becomes unusable. The diff compares against the union of the parts, so a
chilli on toast-and-avocado reads as one `+ chilli` rather than being blamed on
whichever part didn't contain it.

**A logged meal stores the complete ingredient list, never a diff against the
live recipe.** This is the most important decision in the model. If the entry
only said "Greek Salad + avocado", then editing the Greek Salad recipe next March
would silently rewrite what you ate today — and every conclusion drawn from it
would be built on fiction. History has to be immutable.

The *display* is still the diff you'd expect, produced by comparing `items`
against `basis.itemsAtLogTime`:

```
Today 12:40   Greek Salad
              + avocado   + beetroot   − red onion
              contains: dairy (feta)
```

After saving a customised meal the app offers, without a modal in the way:
**Update Greek Salad** / **Save as a new recipe** / **Just this once** (the
default). That's how the library grows — out of what you actually ate, rather
than a data-entry session.

Recipes themselves are the obvious thing: id, name, items, notes, favourite,
plus `timesUsed` and `lastUsedAt` so the picker leads with what you really eat.

### Categories: a couple of questions, not a list

Toast is not a list of saved meals. Nobody wants "Toast, sourdough", "Toast,
wholemeal" and "Toast, gluten-free" as three separate things to scroll past, and
that only gets worse once seeded and multigrain exist. It's **two questions**:

```
Bread         sourdough  wholemeal  multigrain  seeded  rye  gluten-free
Ingredients   butter  Vegemite  avocado  peanut butter  tahini  honey  cheese
```

So a `Category` is a list of `Facet`s, and each facet is a multi-select:

| Kind | What ticking does |
|---|---|
| `STYLE` | Describes **one** thing. Options contribute *modifiers*; an option that names an ingredient decides what the thing is. |
| `ITEMS` | Adds one ingredient per tick. Always called **Ingredients**. |

A category can also carry `always` — what using it means you ate before any
question is answered. A coffee contains coffee, and making that a tick inside the
Coffee category is asking you to state the obvious; an option you always choose
is not a choice. Toast has none, because which bread it was is a real question.
The category editor lists them, so an ingredient landing in every entry is never
invisible.

There is exactly one style question per category and then there are ingredients.
The item lists were briefly split up — "On it", "Made with", "Sweetened" — which
meant deciding which list a topping belonged to before you could tap it. Four
words for one idea.

**The style facet is the important one.** A wholemeal sourdough multigrain loaf
is one bread with three characteristics — ticking three options must not add
three breads, because that would claim you ate three loaves and quietly wreck
every number downstream. So a style facet resolves to a single `MealItem`
carrying all the ticks as `modifiers`:

```kotlin
MealItem(
    ingredientId = "wheat-bread-wholemeal",
    modifiers = setOf(Sourdough, Fermented, Wholegrain),
)
```

**Library → Categories** is where the full lists live: every facet, every option,
what each one actually does, and an × to take it out. Adding from inside the
picker is the fast path, but nothing there removes anything, and a hidden
long-press is a poor place to keep the only way to undo something. Removing an
option only stops it being offered — logged meals hold their own resolved
ingredients, so nothing taken out here can change what the diary says you ate,
and the screen says so.

Every facet ends with **+ Add**, because the seeded options are a starting point
and not a menu — the first unusual bread shouldn't send you back to searching.
Adding follows the same split: picking an *ingredient* adds a thing (a new
topping, or on a style facet a different bread, which then decides what the loaf
is), while typing a *description* adds a way something was made, which describes
whatever you picked rather than replacing it. An option you add is kept on the
category, so it's a chip from then on. If the ingredient doesn't exist at all you
can create it there, and the app says plainly that it has no attributes yet and
so won't count towards anything until you tag it.

Modifiers are `Attribute`s, so they flow into `MealEntry.attributes()` alongside
everything the ingredient itself carries, and rank as suspects like anything
else. That matters more than it sounds: **a long ferment breaks down fructans
but leaves gluten alone**, so "was it sourdough?" is exactly the sort of question
that can separate the two. It's one of the few naturally occurring comparisons
where wheat is held constant and the fructans move.

### Saved meals, and how the picker narrows

A saved thing is either a whole meal (Red lentil dhal) or a **component** you'd
never eat alone (Toast, sourdough). Both are the same kind of object, and a meal
is however many of them you tapped, so **nothing is classified as a base or a
topping**. That distinction isn't a real property of food — is avocado a topping
or a meal, is rice the base or is the curry — and maintaining it would be a
running argument with yourself.

What a recipe *can* carry is an optional `group`: a folder in the picker, one
level nestable on `/`. `Toast` holds the three breads; `Cafe/Kettle Black` holds
a venue's dishes. It's somewhere to put things so a long list stays navigable,
nothing more.

The logging sequence is then:

```
Log a meal
  Breakfast          a category, not a clock setting — it decides what comes next
    Build ▸ Toast    tick the bread, tick what's on it
    Add to the meal
  Save               → when was it, notes, what to do about the library
```

**The top level holds one kind of thing.** Categories to build from, and folders
of saved dishes where a folder makes sense (a café's menu). Whole saved meals and
bare ingredients used to sit underneath in a flat list, which made the first
screen a jumble; they now live behind **Recent** and **Saved**, which is where
you'd look for them on purpose.

**Inside a folder, things live where you'd reach for them.** Butter is in with
the toast, not alongside it. Which folder a loose ingredient belongs to isn't
declared anywhere: it's whatever that folder's contents say they go with (the
bundled hint), plus whatever you've actually eaten them with. The order flips on
what you've done — components first while the meal is empty, toppings once
there's something to put them on.

**The time is asked on the way out, not up front**, because you know what you ate
before you care what time it was, and the answer is usually "just now". The third
option is deliberately not "none": every lag window is measured from the meal
time, so a meal with no time can't be related to a symptom at all. Instead
"sometime that morning" stores the meal type's usual hour, flags the entry
approximate, says so in the dialog, and shows as `~08:00` in the day. The entry
stays analysable and the analysis knows not to trust the minute hand.

**What's offered is learned from your own log**, not declared in advance:

| Signal | What it does |
|---|---|
| Habit at this meal type | Porridge stops being suggested at 19:00. Use at another meal counts for a fraction, so a nightly curry doesn't lead the morning list. |
| Goes-with | Once toast is in the meal, what you normally put on it comes first. This is co-occurrence counting over the log — the same machinery the suspect ranking uses. |
| Recency | A fixed decay, so what you're eating this month beats what you ate in March. |

Folders are ranked by their best member and sorted *among* the items, not above
them — otherwise a Toast folder would sit above the dhal you eat every night.

The one thing that can't be learned is the first morning, since there's no log
yet. So the bundled components carry a `suggests` hint — toast goes with butter —
which exists purely to make day one usable and is overtaken by real data within a
week. You never maintain it; it ships with the seed.

Saving back has one wrinkle: with several parts, "update the recipe" has no
answer, so it's offered only when exactly one thing was used. A combination gets
**Save as a new one** instead, pre-named from the parts — which is the honest
answer, because the combination *is* the new thing.

### Symptoms

A symptom is an **episode with a start and an end**, not a moment:

```kotlin
@Serializable
data class SymptomEntry(
    val id: String = "",
    val type: SymptomType = ...,        // enum + Custom, same pattern as Attribute
    val startedAt: LocalDateTime = ...,
    val endedAt: LocalDateTime? = null, // null = still going

    /** Severity over time. Always at least one point, at [startedAt]. */
    val severityPoints: List<SeverityPoint> = emptyList(),

    /**
     * False once an open episode has gone stale — see "Forgotten timers".
     * Duration is then treated as unknown rather than as however long it's been.
     */
    val durationTrusted: Boolean = true,
    val notes: String = "",
)
```

Duration is derived from the two timestamps, never stored — a stored duration and
an edited end time drift apart, and then the analysis is running on whichever one
happens to be stale.

Four labelled severity levels, not a 0–10 slider. Ten points of self-reported
precision is an illusion — nobody rates the same bloat a 6 twice — and that noise
goes straight into the analysis. Four words you can pick consistently beat ten
numbers you can't.

#### Two ways in, one model

Both paths write the same two timestamps, so there's one thing to analyse and no
second-class entries:

**Start the clock.** *"I'm bloated — start timing."* Two taps: the symptom chip,
then a severity. `startedAt = now`, `endedAt = null`. It pins to the top of Today
as a live episode with the time running, and a **Stopped now** button. Tapping
that sets `endedAt`; a long-press lets you set an earlier end if it stopped a
while ago and you only just noticed.

**Fill it in after.** *"I got bloated around half four and it lasted a couple of
hours."* Same sheet, but you set the start (a picker, with **−1 h / −2 h / −3 h**
shortcuts because that's what actually gets used) and then either an end time or
a duration chip — 30 min / 1 h / 2 h / 4 h / all evening. Duration chips just
compute `endedAt` and are never stored as a duration.

#### Episodes across midnight and across days

**Yes, and nothing special is needed to make it work** — an episode is two
timestamps, so it spans whatever it spans. A bloat from 22:00 to 07:00 and a
three-day flare are the same shape as a twenty-minute one.

What that does need is care in three places:

- **The timeline** draws a spanning episode on every day it overlaps, as a band
  rather than a dot, labelled from the reader's point of view: *"since 16:30
  yesterday"* on day two, *"day 3 of ongoing"* after that. An episode you started
  on Monday must not vanish from Wednesday's screen.
- **Severity is allowed to move.** A three-day flare usually peaks in the middle,
  and calling the whole thing "severe" because the worst hour was overstates two
  of the days. So an open episode takes **It's worse now / It's easing** from its
  card, appending a `SeverityPoint`; the level in force at any moment is the last
  point at or before it. One point is the normal case and costs nothing.
- **The scoring** treats an episode as intensity spread over time rather than an
  event at a point. Its contribution to any lag window is
  `severity × minutes of overlap with that window`, using whichever severity
  point is in force. That falls out correctly for the long ones: a 60-hour flare
  lands in all three windows after the meal that preceded it, in proportion,
  instead of being pinned to one of them and disappearing from the others.

#### Forgotten timers

The obvious failure is starting a timer and never closing it, then having the app
score a forgotten entry as an 80-hour episode. Auto-closing at midnight would
solve it by throwing away exactly the multi-day data that matters, so instead:

Once an episode has been open more than 24 hours without being touched, the app
asks — as a notification and on the card — **Still going / It's stopped (when?)
/ I forgot to close it**. Until you answer, `durationTrusted` goes false and the
scoring uses severity alone for that episode, with duration treated as unknown.
It's still counted as an exposure, still visible; it just isn't allowed to
dominate the numbers on the strength of a forgotten tap. Answer it and the full
duration comes back.

Gut: bloating, gas, distension, cramping, pain, reflux, nausea, urgency,
constipation, incomplete evacuation.

Non-gut: fatigue, brain fog, headache, joint ache, skin flare, low mood,
disturbed sleep. These are often the *clearer* signal — bloating is noisy and
near-constant, a reliable next-day fatigue crash is not.

Bowel movements get their own type, because Bristol 1–7 is not a severity:

```kotlin
@Serializable
data class StoolEntry(
    val id: String = "", val at: LocalDateTime = ...,
    val bristol: Int = 4,               // 1..7, shown as labelled illustrations
    val urgency: Severity? = null,
    val notes: String = "",
)
```

### Confounders

Alcohol and caffeine are *eaten*, so they're ingredients with attributes and
arrive through the meal log for free — no double entry. What's left is genuinely
daily, and lives as one card at the bottom of the day:

```kotlin
@Serializable
data class DayLog(
    val date: LocalDate = ...,
    val sleepHours: Double? = null, val sleepQuality: Severity? = null,
    val stress: Severity? = null,
    val exercise: ExerciseLevel? = null,
    val unwell: Boolean = false,
    val cyclePhase: CyclePhase? = null,
    val note: String = "",
)
```

Medications and supplements are timed entries rather than daily flags — an
antispasmodic taken *after* symptoms start must not end up looking like a cause.

Without any of this the ranking will cheerfully blame last night's dinner for a
bad night's sleep, and you'll cut a food for nothing.

### Trial — the part that actually answers the question

```kotlin
@Serializable
data class Trial(
    val id: String = "", val name: String = "",       // "Gluten-free, 6 weeks"
    val excluded: Set<Attribute> = emptySet(),
    val excludedIngredientIds: Set<String> = emptySet(),
    val startOn: LocalDate = ..., val plannedEndOn: LocalDate? = null,
    val endedOn: LocalDate? = null,
    val phase: TrialPhase = ...,                      // Baseline | Elimination | Reintroduction
)
```

A trial does three things:

- **Warns at log time.** Tapping Greek Salad during a dairy trial shows
  "contains dairy — feta" *before* you save, with one tap to drop it. The warning
  is inline and never blocks; you're allowed to eat the feta and record it.
- **Scores adherence.** Days clean versus days broken, computed from the meals
  rather than self-reported. A 40%-adherent gluten-free month proves nothing, and
  the app should say so plainly instead of letting a conclusion be drawn from it.
- **Compares.** Baseline vs elimination vs reintroduction, on the same symptom
  load metric, with the caveats attached.

It also holds you to a protocol that works: ~2 weeks baseline, 4–6 weeks
elimination, then reintroduction one attribute at a time — a challenge dose over
3 days, then a washout before the next. Reintroducing everything at once on day
43 is the most common way people waste six weeks of effort.

## Analysis

### Symptom load

One number per time window: severity weighted 1 / 2 / 4 / 7 — deliberately
super-linear, because "very severe" is not four milds — multiplied by how much of
the episode actually falls inside the window. Everything downstream reduces to
comparing this number.

Episodes with an untrusted duration (see *Forgotten timers*) contribute their
severity without the time factor, so a forgotten timer can't outweigh a fortnight
of real entries.

### Lag windows

A meal is scored against three windows after it: **0–6 h**, **6–24 h**,
**24–48 h**. Immediate bloating, next-morning stool, and the slower
inflammatory-feeling response are three different mechanisms and shouldn't be
averaged into one.

For each candidate — every ingredient, and every attribute:

```
lift(window) = mean symptom load after meals containing X
             ÷ mean symptom load after meals without X
```

reported with `n` exposed and `n` unexposed. Ranked by lift, but **gated on
evidence**: under 5 exposures each way it isn't ranked at all, it sits under
"not enough yet — you've eaten this 3 times". A suspect list that promotes
whatever you happened to eat on one bad day is worse than no list.

The base rate ships alongside, always: *"you record bloating on 61% of days
regardless of what you eat"*. Without the null, every lift looks meaningful.

### Confounding — the feature that earns its keep

Before ranking, the app checks co-occurrence. If two candidates travel together
in more than ~80% of their appearances it refuses to separate them, and says so:

> **Gluten and fructans can't be told apart yet.** All 23 of your wheat meals
> also contained fructans. To separate them, try a meal with gluten-free bread
> and no onion or garlic — or one wheat meal without either.

Suggesting the discriminating meal is the difference between a diary and an
instrument. The same machinery flags a suspect that's really a proxy for eating
late, drinking, or sleeping badly.

### Purely computed — no model in the loop

The whole analysis is deterministic arithmetic over your own log: sums, means,
ratios, counts, co-occurrence. **No LLM, no network call, no "assessment" by
anything.** That is a hard architectural constraint, not a phase-one shortcut.

Three reasons it's the right call and not just the cheap one. It's
**reproducible** — the same log gives the same ranking, today and in six months,
so a change in the numbers means your body changed rather than a model did. It's
**inspectable** — every figure traces back to entries you can tap through to, and
"9 of your 23 wheat meals" is a claim you can audit. And it **can't invent** — a
model asked to explain a symptom pattern will always produce a plausible story,
including from noise, and a fluent wrong answer about which food to cut is worse
than no answer at all.

So the scoring lives in `domain/analysis/` as pure Kotlin under unit tests, which
is exactly where the risk of being subtly wrong sits.

### Framing

Suspects are "worth testing", never "the cause", and every row carries its `n`.
Each one has a single button: **Run a trial excluding this**, pre-filled. The
observational half exists to choose what to test — it isn't allowed to pretend it
concluded anything.

Deliberately no p-values. One person's noisy self-report over six weeks produces
significance numbers that are confident and wrong, and a number with a star next
to it beats a caveat every time.

## Screens

| Screen | What it is |
|---|---|
| **Today** | The day as one timeline — meals, symptoms, stool, meds in time order. Swipe for other days. Active trial and adherence in the header. A round **+**, centred, logs a meal; three matching circles below it are Library / Symptom / Stool; a cog opens Settings. |
| **Log meal** | Recents and favourites first (that's the fast path), then search. Tap a recipe and it lands as an editable item list; add ingredients from chips, tap an item for portion or to remove it. Trial warnings inline. Save → keep it as a one-off or save a new recipe; never an offer to overwrite the one you started from. |
| **Log symptom** | Chip grid → severity row → done. Two taps, and the clock starts running. Or set the start yourself with **−1 h / −2 h / −3 h** shortcuts and an end time or duration chip. Open episodes pin to the top of Today until closed. |
| **Recipes** | The library. Edit, favourite, duplicate. |
| **Ingredients** | Names, aliases, attributes, default unit. Editable — the seed data will be wrong about something you eat. |
| **Insights** | Suspect ranking, base rates, and per-candidate detail: exposures plotted against symptom load. |
| **Trials** | Set up, run, review. Adherence and phase comparison. |
| **Settings** | Energy unit, text size, style, light/dark. Reached by the cog on the day screen. Reminders, import and custom symptoms still to come. |

Three ranks of control on the day screen, by how often each is used. Eating
happens several times a day, so it gets the round **+**, centred: its own size,
its own row, sharing with nothing. Symptoms and stool are logged most days, so
they are circles in the bar below. The library is a place you go — to add a
recipe or fix an ingredient's tags, usually while logging — so it is a third
circle beside them rather than a menu. Settings you touch once, which is exactly
what a cog in a corner is for.

The three are one size and one shape because they are one rank; the **+** is
bigger and apart because it is not. Icons carry them, with no labels: it costs a
beginner one guess at the book and buys a bar that is mostly empty paper, which
is the look the whole app is going for. Every one carries a
`contentDescription`, so a screen reader still names them.

The stool button shows `Icons.Outlined.Wc`, the restroom door sign. Material
ships neither a toilet nor a stool — checked in both the bundled
`material-icons-extended` and the newer Material Symbols — and the rest of the
near misses mean washing (`Bathroom`, `Sanitizer`, `CleanHands`), the wrong
fixture (`Bathtub`), or a burst pipe (`Plumbing`).

Two hand-drawn icons came before it and both were dropped, which is the part
worth keeping. Neither was bad as a shape; both were bad as a dependency. They
were the only art in the app that was ours to keep working, they never shared
Google's optical sizing with the two stock icons beside them, and each cost
several build-and-look rounds — including a non-zero fill-rule trap where one
sub-path wound the opposite way and subtracted a white seam through the middle,
which no amount of previewing the shapes in isolation would ever have shown.

The rule this leaves: draw an icon only when the stock set has nothing that
points at the right idea, and count the maintenance, not just the drawing.

What this replaced was a three-dot overflow holding the library *and* settings
together: two unlike things, one unfindable menu, and the food you have
described to the app buried two taps deep behind an icon that means "more".

**Reminders** (local, no network): a post-meal nudge at a configurable lag, an
evening catch-up, and a morning prompt for sleep and stool. Missing evening
entries is the single most common reason a food diary stops being usable. Same
approach as AndroidTasks — straight to `AlarmManager` with a boot receiver, no
WorkManager and no extra permissions.

**Export** is two things: the raw `diary.json` for backup and reimport, and a
readable date-range summary — meals, symptom load, trial adherence — to hand to a
GP or dietitian. That's the real destination of this data, and it shouldn't
require a montage of screenshots.

## Storage

One file, `diary.json`, whole-file rewrite on change, temp-file-then-rename — the
`LocalTaskRepository` pattern lifted directly, for the same reasons.

Sizing it honestly: three meals a day for three years is ~3,000 entries, call it
1–2 MB. Rewriting that on every tap is fine, but not free. So `DiarySnapshot`
carries a `schemaVersion` from day one and the repository keeps sharding the log
by year as a known escape hatch — the library (ingredients, recipes, trials) is
small and stays in one file regardless.

## Module layout

```
app/src/main/java/dev/mahourigan/fooddiary/
  domain/         pure Kotlin, no Android imports
    Ingredient.kt      ingredient, Attribute, attribute inheritance
    Portion.kt         coarse buckets, exact amounts, the bucketing rule
    Recipe.kt
    MealEntry.kt       Basis, the resolved-items rule, diff-for-display
    Symptom.kt         SymptomEntry, StoolEntry, Severity
    DayLog.kt          confounders
    Trial.kt           phases, adherence, breach detection
    analysis/
      SymptomLoad.kt   the weighted metric
      LagWindows.kt    window arithmetic
      Suspects.kt      lift, evidence gates, ranking
      Confounding.kt   co-occurrence, the discriminating-meal suggestion
  data/
    DiarySnapshot.kt   the whole store, one serializable root
    LocalDiaryRepository.kt
    Repositories.kt    single shared instance — the reminders write too
    Serializers.kt     ISO-8601 dates, same as tasks
    SeedIngredients.kt the bundled library
  notification/        AlarmManager reminders + boot receiver
  ui/                  Compose screens, one per file, + DiaryViewModel
src/test/java/...      the domain package, mirrored
```

## Seed data

The app is useless on day one with an empty ingredient library, so it ships with
a bundled, fully-tagged set — roughly 250–300 ingredients. That's real work and
gets its own step, not a footnote inside another one.

**The library is vegetarian.** No meat, poultry, fish or shellfish; dairy and
eggs are in. A trigger list you have to scroll past forty cuts of meat to use is
a trigger list you stop using.

| Section | Coverage |
|---|---|
| Vegetables | The full range, not a token list — every allium (onion, red onion, spring onion, shallot, leek, garlic), brassicas, root veg, salad veg, squashes, mushrooms, peas and beans, corn, artichoke, asparagus, avocado. |
| Fruit | Full range, including the ones that matter here: apple, pear, mango, watermelon, stone fruit, dried fruit, plus the low-fructose ones (berries, citrus, kiwi, banana) so the contrast is visible. |
| Cheeses | Named individually, because they are not interchangeable: hard and aged (parmesan, cheddar, pecorino, gruyère) carry almost no lactose; soft and fresh (ricotta, cottage, cream cheese, halloumi, feta, mozzarella) carry a lot. Merging them into "cheese" would hide the single clearest dairy signal you can get. Blue cheeses tagged for histamine too. |
| Eggs | Whole, white, yolk — separately, since egg white is the usual culprit. |
| Alternative milks | Oat, soy, almond, cashew, coconut, rice, macadamia, hemp, plus lactose-free dairy milk. See below. |
| Grains & flours | Wheat, spelt, rye, barley, couscous, semolina, seitan, plus the gluten-free side: rice, corn, buckwheat, quinoa, millet, oats (with and without the gluten-free tag), and the flours. |
| Legumes & pulses | Chickpeas, lentils (red / brown / puy), all the beans, tofu, tempeh, edamame — tinned and dried tagged differently, since draining and rinsing tinned pulses genuinely lowers the GOS load. |
| Nuts, seeds, fats, dairy | Nuts and seeds individually, oils, butter, yoghurts (including the lactose-free and plant ones), cream, milk, kefir. |
| Store-cupboard | Stock cubes, soy sauce and tamari, miso, vinegars, mustard, tahini, nutritional yeast, honey, agave, maple, the artificial sweeteners, chilli, spices, herbs. The hidden-gluten and hidden-onion items live here and are the ones most worth having pre-tagged. |
| Drinks | Tea, coffee, the herbal teas, juices, soft drinks, beer, wine, cider, spirits, kombucha. |

**Alternative milks get their own attention** because they are a common hidden
trigger and swapping between them is invisible in a normal diary. Oat milk is
high in fructans, soy milk made from whole beans carries GOS (soy protein isolate
doesn't), almond and macadamia are clean, and several brands add inulin or
chicory root, which is fructan on purpose. Going dairy-free and feeling worse is
a very ordinary outcome and this is usually why.

One more thing this app should watch for, and the reason it's worth writing down
now: **a vegetarian cutting gluten usually swaps wheat protein for legumes.**
Seitan and bread go out, chickpeas and lentils come in — so fructans drop while
GOS rises, both at once. A gluten trial that changes two variables answers
nothing, so the trial screen flags that shift when the log shows it.

Two caveats belong in the app, not just here: the FODMAP tagging is our own
coarse "high / not high" judgement rather than Monash-certified figures, and
attribute tags describe an ingredient generically, so a specific product can
differ — soy sauce is usually wheat, tamari isn't; one oat milk has inulin and
another doesn't. Anything you correct is yours and sticks.

## Build order

Each step leaves the app usable, so it can be lived with while the next is built.

1. **A working diary.** Model, JSON store, Today timeline, log a meal ad-hoc or
   from a recipe with customisation, log symptoms and stool. No analysis. This
   alone already beats a notes app.
2. **The ingredient library.** Seed data, attributes, inheritance, search and
   aliases, the ingredient editor.
3. **Trials.** Setup, log-time warnings, adherence, phase comparison. At this
   point the gluten question is answerable by hand.
4. **Insights.** Symptom load, lag windows, lift, evidence gates, base rates,
   confounding detection, and the one-tap hop from a suspect into a trial.
5. **Reminders, export, polish.** Notifications, the dietitian summary, backup
   and restore.

## Deliberately out of scope for now

- **Barcode scanning / packaged foods.** Needs a network food database
  (OpenFoodFacts), which breaks the offline-only stance both other apps hold.
  Worth revisiting as an explicit opt-in decision — not something to slide in.
- **Macros.** Protein, carbs and fat are a further step and would need the same
  per-100 data three times over. Calories are in (see below); macros aren't.
- **Meal photos.** Cheap to add later; adds a file store the JSON model doesn't
  currently need.
- **Sync, sharing and second users.** One person, one device, one file. No owner
  field on entries and no profile switcher — a second person installs their own
  copy. The repository still sits behind an interface, so a syncing store stays
  possible without touching anything above it.
- **Anything diagnostic.** No conditions named, no advice. It ranks suspects,
  runs trials, and produces something legible to take to someone qualified.
