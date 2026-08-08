package com.github.worn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Worn's palette in two layers.
 *
 * The M3 [androidx.compose.material3.ColorScheme] below is the real source of truth: every
 * Material component (sheets, dialogs, ripples, snackbars) reads it, so filling in *all* the roles
 * — not just the handful the app names — is what keeps stray baseline-purple out of the UI.
 *
 * [WornExtras] carries the few brand tokens M3 has no role for: gradient stops, the muted
 * text/icon greys and the category dots. It hangs off [MaterialTheme] as [wornExtras], so call
 * sites read `MaterialTheme.wornExtras.iconMuted` right next to
 * `MaterialTheme.colorScheme.primary` and every colour comes from one place.
 */

// ---------------------------------------------------------------------------------------------
// Light — the established warm beige + sage brand, unchanged in hue.
// ---------------------------------------------------------------------------------------------

internal val WornLightColorScheme = lightColorScheme(
    primary = Color(0xFF7A9468),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6D2),
    onPrimaryContainer = Color(0xFF2A3A20),
    inversePrimary = Color(0xFFA8C295),

    secondary = Color(0xFF6B7B8E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE3EA),
    onSecondaryContainer = Color(0xFF22303E),

    tertiary = Color(0xFFA87560),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DED4),
    onTertiaryContainer = Color(0xFF3D2318),

    error = Color(0xFFC45B4A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DAD4),
    onErrorContainer = Color(0xFF43110A),

    background = Color(0xFFF5F0EB),
    onBackground = Color(0xFF2C2924),
    surface = Color(0xFFF5F0EB),
    onSurface = Color(0xFF2C2924),
    surfaceVariant = Color(0xFFEDE8E1),
    onSurfaceVariant = Color(0xFF7D776F),
    surfaceTint = Color(0xFF7A9468),

    surfaceDim = Color(0xFFE4DDD3),
    surfaceBright = Color(0xFFFFFBF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    // Cards and tiles. White here, one step *above* the beige page — see the dark scheme for why
    // this role rather than surfaceContainerLowest.
    surfaceContainer = Color(0xFFFFFFFF),
    // Chrome that sits behind content: the bottom bar and the filter chips.
    surfaceContainerHigh = Color(0xFFEDE8E1),
    surfaceContainerHighest = Color(0xFFE7E0D7),

    // M3 defines outline as the stronger of the pair; the previous scheme had these two swapped.
    outline = Color(0xFFC8C0B5),
    outlineVariant = Color(0xFFE0D9D0),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF322F2A),
    inverseOnSurface = Color(0xFFF5F0EB),
)

internal val WornLightExtras = WornExtras(
    textMuted = Color(0xFFB5AFA8),
    iconMuted = Color(0xFFA09A92),
    accentGreenDark = Color(0xFF5C6E50),
    saveGradientStart = Color(0xFF8FA47D),
    saveGradientEnd = Color(0xFF6B7F5E),
    greenCtaStart = Color(0xFF7A9468),
    greenCtaEnd = Color(0xFF6B8A58),
    indigoGradientStart = Color(0xFF6B7B8E),
    indigoGradientEnd = Color(0xFF556070),
    categoryDotTop = Color(0xFF444444),
    categoryDotBottom = Color(0xFF2B4570),
    categoryDotDress = Color(0xFFA87560),
    categoryDotOuterwear = Color(0xFF7A9468),
    categoryDotShoes = Color(0xFF8B6914),
    categoryDotAccessory = Color(0xFFB59D6E),
)

// ---------------------------------------------------------------------------------------------
// Dark — the same hues on a warm dark axis. Two things this deliberately avoids:
//
//  * Near-black. A #121212-style base reads cold next to the sage and drops the brand's warmth
//    entirely, so the page sits at #211D18 — a brown-grey that still reads as "Worn".
//  * A compressed ramp. The steps are spaced widely enough to be visible: page #211D18 against
//    cards #1A1713 is a step you can actually see, where a 2% difference just looks like one
//    flat black sheet no matter how correct the token names are.
// ---------------------------------------------------------------------------------------------

internal val WornDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C295),
    onPrimary = Color(0xFF1B2913),
    primaryContainer = Color(0xFF3D5230),
    onPrimaryContainer = Color(0xFFC4DEB0),
    inversePrimary = Color(0xFF7A9468),

    secondary = Color(0xFFA9BACE),
    onSecondary = Color(0xFF1B2733),
    secondaryContainer = Color(0xFF3A4756),
    onSecondaryContainer = Color(0xFFC7D6E6),

    tertiary = Color(0xFFD8A88F),
    onTertiary = Color(0xFF2C1509),
    tertiaryContainer = Color(0xFF59392A),
    onTertiaryContainer = Color(0xFFF2DED4),

    error = Color(0xFFF2B8AC),
    onError = Color(0xFF4E1509),
    errorContainer = Color(0xFF7A2A1E),
    onErrorContainer = Color(0xFFFFDAD3),

    background = Color(0xFF211D18),
    onBackground = Color(0xFFEFE8DD),
    surface = Color(0xFF211D18),
    onSurface = Color(0xFFEFE8DD),
    surfaceVariant = Color(0xFF363029),
    onSurfaceVariant = Color(0xFFD2C9BC),
    surfaceTint = Color(0xFFA8C295),

    surfaceDim = Color(0xFF211D18),
    surfaceBright = Color(0xFF4A443C),
    surfaceContainerLowest = Color(0xFF17140F),
    surfaceContainerLow = Color(0xFF262119),
    // Cards and tiles — one step *above* the page, so they still read as standing forward the way
    // the white-on-beige light scheme does. Mapping them to surfaceContainerLowest instead (the
    // darkest step, which is what "card = white = brightest" naively translates to) sinks them
    // into the background and reads as black holes.
    surfaceContainer = Color(0xFF2B261F),
    surfaceContainerHigh = Color(0xFF363029),
    surfaceContainerHighest = Color(0xFF423B32),

    outline = Color(0xFFA0988B),
    outlineVariant = Color(0xFF554E45),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDE6DC),
    inverseOnSurface = Color(0xFF322F2A),
)

internal val WornDarkExtras = WornExtras(
    textMuted = Color(0xFF8A8378),
    iconMuted = Color(0xFF9B9488),
    accentGreenDark = Color(0xFF4E6641),
    saveGradientStart = Color(0xFF6E8A5C),
    saveGradientEnd = Color(0xFF546B45),
    greenCtaStart = Color(0xFF6E8A5C),
    greenCtaEnd = Color(0xFF5C7A4B),
    indigoGradientStart = Color(0xFF5E6E80),
    indigoGradientEnd = Color(0xFF4A5462),
    // These do double duty: 8dp dots on the page surface, and 36dp tile fills behind *white*
    // icons in Gaps. So they can only be lifted far enough to be visible on #14120F, not so far
    // that white stops reading on top — which rules out simply using the light-scheme tints.
    // Only the two darkest needed moving; the rest already clear both bars unchanged.
    categoryDotTop = Color(0xFF6E6862),
    categoryDotBottom = Color(0xFF42618F),
    categoryDotDress = Color(0xFFA87560),
    categoryDotOuterwear = Color(0xFF7A9468),
    categoryDotShoes = Color(0xFF8B6914),
    categoryDotAccessory = Color(0xFFB59D6E),
)

/**
 * Brand tokens with no equivalent M3 role.
 *
 * The gradient stops are deliberately *not* derived from `primary` / `secondary`. Those roles
 * invert between light and dark (sage goes from #7A9468 to a much lighter #A8C295), and the
 * gradient buttons always draw white label text — deriving them would silently drop the label to
 * roughly 1.8:1 in dark mode. These stay saturated enough for white in both themes.
 */
@Immutable
data class WornExtras(
    val textMuted: Color,
    val iconMuted: Color,
    /** Banner and gradient-end fills that always carry white text — dark in *both* themes. */
    val accentGreenDark: Color,
    val saveGradientStart: Color,
    val saveGradientEnd: Color,
    val greenCtaStart: Color,
    val greenCtaEnd: Color,
    val indigoGradientStart: Color,
    val indigoGradientEnd: Color,
    val categoryDotTop: Color,
    val categoryDotBottom: Color,
    val categoryDotDress: Color,
    val categoryDotOuterwear: Color,
    val categoryDotShoes: Color,
    val categoryDotAccessory: Color,
)

internal val LocalWornExtras = staticCompositionLocalOf { WornLightExtras }

/**
 * Extension point for the brand tokens that have no M3 role.
 *
 * This is the standard Compose way to widen a design system: hang the extra tokens off
 * [MaterialTheme] so a call site reads `MaterialTheme.wornExtras.iconMuted` right beside
 * `MaterialTheme.colorScheme.primary`. Everything colour-related then comes from one object, and
 * the M3 role in play is visible at the point of use instead of hidden behind an alias.
 *
 * Reading it outside a composition is a compile error by design — a top-level `val` capturing a
 * colour would freeze it to whichever theme was active at class-init time.
 */
val MaterialTheme.wornExtras: WornExtras
    @Composable @ReadOnlyComposable get() = LocalWornExtras.current
