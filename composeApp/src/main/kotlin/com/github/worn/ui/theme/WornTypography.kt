package com.github.worn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Worn's type scale.
 *
 * The sizes are the ones the app was already using at ~145 scattered `fontSize =` call sites, so
 * adopting this changes no pixels — it just gives each one a name, a matching line height and a
 * single place to change. The M3 role names are kept (rather than invented ones) so Material
 * components that reach for `typography` on their own land on the right style too.
 *
 * `letterSpacing` on [Typography.headlineMedium] is the -0.5sp the screen titles already carried;
 * [Typography.labelSmall] keeps the +0.5sp of the uppercase tab labels.
 */
internal val WornTypography = Typography(
    // Large stat numbers.
    displaySmall = TextStyle(
        fontSize = 42.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Normal,
    ),

    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // Screen titles: "Worn", "Your outfits", "What's missing", "Settings".
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),

    // Empty-state and sheet titles.
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // Section headers.
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // Row titles and button labels.
    titleSmall = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),

    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    // Card names and descriptions — the most common style in the app.
    bodySmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),

    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    // Category labels and captions.
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
    // Bottom-bar tab labels.
    labelSmall = TextStyle(
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    ),
)
