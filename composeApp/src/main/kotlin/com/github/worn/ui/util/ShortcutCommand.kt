package com.github.worn.ui.util

import com.github.worn.domain.model.AppShortcut

private const val ACTION_PREFIX = "com.github.worn.action."

/**
 * One launcher shortcut tap, waiting to be routed.
 *
 * Deliberately not a data class: identity equality makes every tap a distinct value, so
 * `LaunchedEffect` re-fires even when the same shortcut is tapped twice in a row.
 */
class ShortcutCommand(val shortcut: AppShortcut)

/** The intent action declared for this shortcut in `res/xml/shortcuts.xml`. */
fun AppShortcut.action(): String = ACTION_PREFIX + id

fun shortcutCommandFor(action: String?): ShortcutCommand? =
    AppShortcut.entries.firstOrNull { it.action() == action }?.let(::ShortcutCommand)
