# Food diary

A food and symptom diary for working out which foods actually cause trouble,
rather than guessing.

The idea it is built on: **log the properties, not just the foods.** A meal is
recorded as its ingredients, and every ingredient carries attributes — gluten,
fructans, lactose, allium, and so on. That is the difference between "toast
seems bad" and "wheat is fine but fructans are not", which is a conclusion you
cannot reach by writing down meal names.

Nothing you log leaves the phone, and there is no account. The whole diary is
one JSON file in the app's private storage, exportable as the same JSON you
could read yourself.

The app makes one kind of network request, and only when you ask it to: tapping
**Check for updates** in Settings asks GitHub whether a newer release exists. It
sends nothing — no diary, no identifier, not even a record that you asked.

## The part worth knowing about

**Nothing is decided by a model.** Every number in the app is arithmetic over
your own entries — counts, ratios, a fixed decay — so the same diary always
produces the same answer, and any conclusion can be traced back to the meals
that produced it. A food diary that guesses is worse than no food diary.

## What it does

- **Tap a chip to learn what it means.** Half the words here are chemistry —
  mannitol, GOS, casein, sulphites. Tapping any of them explains what it is,
  the non-obvious thing worth knowing, and what trouble with it feels like.
- **Recipes with servings.** A dish is written down the way it is cooked — two
  tins of beans, 250 g of tofu — and logged one portion at a time.
- **Symptoms with duration**, because a bloat from 22:00 to 07:00 and one that
  lasted twenty minutes are not the same event.
- **Sleep, stress and the rest**, logged daily. Without them a bad night's
  sleep gets blamed on dinner.
- **Ten colour themes and seven typefaces**, picked independently, plus a
  custom palette from three swatches with the derived contrast checked and
  reported in numbers.

## Health, honestly

This is a notebook that does arithmetic. It is not a diagnostic tool and it
does not know anything about you.

Two places where that matters enough that the app says so itself: coeliac
disease is an autoimmune condition whose test only works **while you are still
eating gluten**, so if you suspect it, get tested before cutting it out rather
than after — an elimination diary is exactly the thing that destroys that
evidence. And a food *allergy* is minutes and breathing, not weeks and a
pattern; that belongs with a doctor, not with this.

## Building

```
./gradlew installDebug     # onto a connected phone
./gradlew test             # 201 unit tests
```

The domain layer is pure Kotlin with no Android imports, which is why the
portion arithmetic, the FODMAP attribution, the ranking and the ten colour
palettes are all unit-tested on the JVM.

[PLAN.md](PLAN.md) is the long version: what every decision was, and what was
tried and rejected first. It is the actual design record, not a summary.

## Third-party

Eight bundled typefaces — seven under the SIL Open Font License, one under the
Kingthings EULA. See [THIRD-PARTY-LICENCES.txt](THIRD-PARTY-LICENCES.txt).

The FODMAP attributions are a coarse high / not-high judgement rather than
certified figures: a starting point for what to suspect, not a lookup table.
Products differ — soy sauce is usually wheat but tamari is not — so anything
wrong for what you actually buy is meant to be corrected, and your edits
survive app updates.
