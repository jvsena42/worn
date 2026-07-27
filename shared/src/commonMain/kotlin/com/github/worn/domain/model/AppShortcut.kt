package com.github.worn.domain.model

/**
 * Actions the OS can launch straight from the app icon: Android App Shortcuts and iOS Home Screen
 * Quick Actions.
 *
 * [id] is the iOS `UIApplicationShortcutItem.type` and the suffix of the Android intent action, so
 * the two platforms cannot drift apart.
 */
enum class AppShortcut(val id: String) {
    /** Opens the Wardrobe tab with the Add Item sheet already up. */
    ADD_ITEM("add_item"),

    /** Opens the Try It tab in its idle state. */
    TRY_IT("try_it"),

    ;

    companion object {
        fun fromId(id: String?): AppShortcut? = entries.firstOrNull { it.id == id }
    }
}
