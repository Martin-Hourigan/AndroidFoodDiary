package dev.mahourigan.fooddiary.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * The ground a style is written on.
 *
 * A flat fill would make the parchment styles a colour swap; parchment is
 * uneven translucency and that unevenness is most of the effect. Drawn rather
 * than shipped as an image so it scales to any screen and costs no bytes — a
 * handful of soft radial washes over the solid background.
 */
fun Modifier.diaryPaper(background: Color, palette: DiaryPalette): Modifier =
    this.background(background).drawBehind {
        palette.washes.brushes(size).forEach { brush -> drawRect(brush = brush) }
    }
