package com.github.worn.ui.theme

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotations for the two form factors the project targets.
 *
 * Each carries its light and dark variant, so a composable annotated with [PhonePreview] renders
 * both without the file having to repeat the annotation. Compose resolves a "multipreview" by
 * expanding every `@Preview` on the annotation class onto the annotated function.
 *
 * Prefer these over a bare `@Preview`: they are the only thing keeping dark mode covered in the
 * IDE, and dark is exactly where a missed colour role shows up.
 */
@Preview(name = "Phone", showSystemUi = true, device = "id:pixel_8")
@Preview(name = "Phone · Dark", showSystemUi = true, device = "id:pixel_8", uiMode = UI_MODE_NIGHT_YES)
annotation class PhonePreview

@Preview(name = "Tablet", showSystemUi = true, device = "id:pixel_tablet")
@Preview(
    name = "Tablet · Dark",
    showSystemUi = true,
    device = "id:pixel_tablet",
    uiMode = UI_MODE_NIGHT_YES,
)
annotation class TabletPreview
