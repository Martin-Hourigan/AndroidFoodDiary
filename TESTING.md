# What to try

Steps 1 and 2 of the plan: a working diary, and the tagged ingredient library it
runs on. Log meals in a few taps, customise them, log symptoms both ways round,
log movements, fill in the day's confounders. No analysis and no trials yet —
those are steps 3 and 4.

Data lives in one JSON file on the device. No account, no backend, no internet —
the app has no network permission at all.

## Getting it on your phone

```
cd Projects/AndroidFoodDiary
./gradlew installDebug
```

Or `./gradlew assembleDebug` and copy `app/build/outputs/apk/debug/app-debug.apk`
across. It ships with 328 tagged ingredients, three build-it categories and six
saved meals, so it is usable the moment it opens.

## Or on the emulator

```
.\run.ps1            build, install and launch
.\run.ps1 -Fresh     clear the app's data first, to see first-run state
.\run.ps1 -BootOnly  just bring the emulator up
```

Each of the three apps has its own AVD — `food_test` here, `tasks_test` and
`moon_test` for the others — and each `run.ps1` shuts any other one down before
starting its own. One emulator at a time is the point: the other two apps hold
alarms, a widget and a periodic calendar sync, and those keep waking a device
you're trying to watch a symptom timer on.

## The interesting bits

**Logging your usual breakfast, in taps.**

```
Meal  →  Breakfast  →  Toast  →  tick Sourdough, Wholemeal, Butter, Vegemite
      →  Add to the meal  →  Save
```

No typing. Note what happens on the way:

- **Breakfast is a category, not a clock setting.** It decides what you're shown
  next. The clock's guess is marked *likely* but you still tap it, and that tap
  is the one that opens the right list.
- **The top level holds one kind of thing** — categories to build from, and the
  café folder. Whole saved meals live under **Recent** and **Saved**.
- **Toast is two questions**, not a list of every bread-and-topping combination.

**Ticking three bread styles gives you one loaf, not three.** Try Sourdough +
Wholemeal + Multigrain. The summary at the bottom reads:

```
Wheat bread, wholemeal (fermented, sourdough, wholegrain), Butter, Vegemite
```

One bread with three characteristics — which is what a wholemeal sourdough
multigrain loaf actually is. Ticking them as three separate breads would claim
you'd eaten three loaves, and every number downstream would be wrong. The
Ingredients list works the opposite way and adds one per tick.

That distinction has a real payoff: a long ferment breaks down fructans but
leaves gluten alone, so **sourdough is a factor you can test on its own**. If
sourdough sits fine and sandwich bread doesn't, that points at fructans rather
than gluten — one of the few comparisons where wheat is held constant and the
fructans move.

**Adding your own options.** Every facet ends with **+ Add**. Try it on
Ingredients: search "hummus", tap it, and it's ticked *and* kept — a chip at the
end of the list from then on. On a style facet you get a second box, "how it was
made", for things like *long-fermented* or *from the bakery*: those describe
whatever bread you picked rather than replacing it, exactly as sourdough does.

If what you want isn't in the library at all, the dialog offers to create it —
and says outright that it'll have no attributes yet, so it won't count towards
anything until you tag it under Library → Ingredients. Better than refusing to
let you log what you actually ate; the entry is true either way.

**Taking options out.** Library → Categories → Toast shows every facet in full,
what each option does (`Sourdough — sourdough, fermented`, `Gluten-free —
Gluten-free bread`), and an × on each. Remove one, save, and it's gone from the
picker. Removing only stops it being offered: meals you've already logged hold
their own ingredients, so nothing you take out here rewrites the diary. The same
screen renames a category, adds options, and deletes the whole thing.


**Calories, three ways.** Build toast with butter and Vegemite and the summary
reads `~280 kcal` before you've said how much of anything. Every bundled
ingredient carries kcal per 100 g plus what one unit weighs, so a total exists
without extra taps.

- Tap an item and give a real amount — `2 slices`, `40 g` — and that part stops
  being a guess.
- Set a figure on a saved recipe (Library → Saved meals) for something you cooked
  and worked out once. Its ingredients are then skipped rather than counted again.
- Type a number on the save sheet for anything you actually know.

**The hedging is deliberate.** Estimates read `~280` and round to ten; a number
you typed reads `280`. The day total says whether it was estimated and how many
things had no data — counting those as zero would make a day look lighter than it
was, and you'd never know which days.

The bundled figures are my estimates, not a database — the app has no network to
look anything up in, and a "slice" of bread is anywhere from 25 g to 55 g. Treat
them as ±20%. Library → Ingredients has both numbers on every ingredient, so
anything you weigh once can be corrected for good.
**A meal is however many things you added.** A category, then another category,
then a saved meal if you like — Toast *and* a coffee, curry *and* rice, dinner
*and* a glass of wine. There is no base-versus-topping setting anywhere, because
that isn't a real property of food; you'd spend your life deciding whether rice
or the curry is the base.

**The time comes last.** Hitting Save asks *when was it*: **Just now** (the
usual answer), **At…** for the clock, or **that morning** when you genuinely
don't know. That third one isn't "none" on purpose — every symptom is lined up
against meal times, so a meal with no time can't be related to anything. It
records the meal type's usual hour, tells you it has, and shows in the day as
`~08:00` so you never mistake it for a time you actually looked at.

**Folders.** Tap **Cafe** to narrow to café dishes. Groups nest one level: open
Library → Saved meals → any recipe and set its folder to `Cafe/Kettle Black`, and
Cafe then asks which venue first. It's only somewhere to put things; existing
folders are offered as chips so you don't retype them.

**Watch it learn.** Log toast with butter a few mornings, then open the picker
again — Toast has climbed to the front and butter is the first thing offered
after it. Log dhal a few evenings and dhal leads at dinner while toast still
leads at breakfast. All of it is counting over your own entries; the same diary
always produces the same order.

**Customising a saved meal.** Tap **Meal** → **Lunch** → **Greek salad**. Remove
the red onion, search "avoc" and add avocado, add beetroot, save. The day shows:

```
17:06   Greek salad
        + avocado   + beetroot   − red onion
        Casein  Dairy  Fructans  Lactose  Sorbitol  High fat  +3
```

Watch the attribute chips while you edit. Dropping the onion removes Allium —
but **Fructans stays**, because beetroot has them too. Nothing there was tagged
by hand; it's all inherited from the ingredients.

**Then edit the recipe and look again.** Library → Saved meals → Greek salad,
delete something, save. Go back to the day: the logged meal is untouched, and it
still says `− red onion`. Entries store their own resolved ingredient list plus
a snapshot of the recipe as it stood, precisely so tidying a recipe can't rewrite
what you ate. This is the guarantee the whole analysis rests on.

**Saving changes back.** The same save sheet offers **Just this once** (the
default) / **Update Greek salad** / **Save as a new one**. The meal is recorded
identically either way — that choice is only about the library. With several
parts in the meal, "update the recipe" has no answer, so it isn't offered; you
get "save as a new one", pre-named from the parts, instead.

**Portions are optional, at three depths.** Tap any ingredient in a meal:

```
avocado                  say nothing — the normal case
avocado — lots           coarse
sourdough × 1 slice      exact, in the ingredient's own unit
```

"Nothing" is a real button and it clears the field. Unspecified is stored as
absent, never quietly recorded as normal — presence is what the trigger hunt
runs on and works fine without a portion; only dose-response needs one.

**Symptoms, both ways round.** Tap **Symptom** → Bloating → Moderate:

- **Happening now** starts a timer. It shows on the day as *"1m and going"* with
  **Stopped now / Worse / Easing**. Worse and Easing append a severity change, so
  a flare that peaked in the middle isn't scored as severe throughout.
- **Fill in afterwards** gives you *"started about half four, lasted a couple of
  hours"*: set the start with **−1h / −2h / −3h** or the clock, then a duration
  chip or an explicit end time. Duration is never stored, only derived from the
  two timestamps.

**Episodes across days.** Start a timer and set its start back a couple of days
(or leave one running overnight). It appears on **every** day it touches, as
*"since 16:30 yesterday"* on the following day. Nothing gets truncated at
midnight — a three-day flare is the data, not a bug.

**Forgotten timers.** Leave one open past 24 hours and a card pins to the top of
the day: **Still going / Stopped now / I forgot**. Until you answer, that
episode's duration stops being trusted and scores as one nominal hour, so a
forgotten tap can't outweigh a fortnight of real entries. (Easiest way to see it:
log one retrospectively with a start two days ago and "Still going".)

**Cheeses are individual, and that's the point.** Library → Ingredients → search
"cheese". Parmesan and mature cheddar carry **no lactose**; ricotta, feta and
cottage cheese carry plenty. If dairy is a suspect, which cheeses do it is the
most useful thing in the library. Eggs are split into whole/white/yolk for the
same reason.

**The gluten/fructan confound, visible in the data.** Search "bread". Wheat bread
has both **Gluten** and **Fructans**; gluten-free bread has neither. That pairing
is why the analysis works on attributes rather than foods — cutting gluten cuts
fructans too, so "gluten-free helped" can't tell you which one helped. Tap the
Gluten or Fructans chip on any ingredient's editor to see the note explaining it.

**Everything is editable.** Ingredients → tap anything → change its attributes,
its unit, its normal serving. Your edits survive app updates, and an ingredient
you delete doesn't come back when the bundled library grows.

## Not built yet

- **Trials.** Setting up "gluten-free for six weeks", warnings at log time when a
  meal breaks one, adherence scoring, and the reintroduction protocol. Step 3 —
  and the step that actually answers the question.
- **Insights.** Symptom load, the three lag windows, suspect ranking with
  evidence gates, base rates, and the co-occurrence check that says when two
  suspects can't be separated yet. Step 4.
- **Reminders.** Post-meal nudges and the evening catch-up. Step 5.
- **The dietitian export.** Right now export is the raw JSON only.
- **Medication entries.** The model is there; there's no screen for it yet.

## If something looks wrong

`./gradlew test` runs 169 unit tests over the parts most likely to be subtly
wrong — portion bucketing, the recipe diff and its immutability, episode
duration and overlap arithmetic across midnight and across days, the untrusted
duration fallback, timeline assembly, and the seed library's own consistency
(unique ids, no meat, every recipe and category option referring to an
ingredient that exists), the picker ranking — folders, nesting, day-one hints,
and what a log of real meals does to the order — and the rule that a style facet
resolves to one item however many descriptors you tick.

The app's raw state is readable with:

```
adb shell run-as dev.mahourigan.fooddiary cat files/diary.json
```
