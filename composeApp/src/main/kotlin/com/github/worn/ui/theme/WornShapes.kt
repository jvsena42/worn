@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/**
 * Worn's corner-radius scale.
 *
 * The radii are the ones already scattered across the UI as `RoundedCornerShape(n.dp)`, collapsed
 * onto the eight M3 slots. Naming them does two things: Material components pick the right corner
 * on their own, and the handful of near-duplicate one-offs (10/22/26/30dp) fold into the nearest
 * step so the app stops shipping four radii that differ by 2dp and read as the same curve.
 */
internal val WornShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    // Chips, small tiles, input fields.
    medium = RoundedCornerShape(12.dp),
    // Cards and photo frames.
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    // Sheets and dialogs.
    extraLarge = RoundedCornerShape(24.dp),
    // Pill buttons and the FAB.
    extraLargeIncreased = RoundedCornerShape(28.dp),
    // The bottom bar.
    extraExtraLarge = RoundedCornerShape(36.dp),
)

/**
 * Top-rounded shape for bottom sheets, matching [Shapes.extraLarge] on the corners that show.
 *
 * M3 has no slot for a partly-rounded shape, so this hangs off [MaterialTheme] the same way
 * [wornExtras] does, keeping every sheet on one radius instead of ten copies of the literal.
 */
val MaterialTheme.sheetShape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = SheetShape

private val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
