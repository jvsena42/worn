package com.github.worn.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Exposes descendant `Modifier.testTag(...)` values as `resource-id`s in the Android
 * accessibility/layout tree so the `android` CLI and journey evaluation can locate them.
 *
 * The root content sets this once in `App.kt`, but Compose renders `ModalBottomSheet`,
 * `AlertDialog` and other popups in their own semantics owners, which do NOT inherit that
 * root flag. Apply this at the root of each such sheet/dialog so its tags surface too.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTagsAsResourceId(): Modifier =
    this.semantics { testTagsAsResourceId = true }
