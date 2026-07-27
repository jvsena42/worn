package com.github.worn.model

import com.github.worn.domain.model.AppShortcut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppShortcutTest {

    @Test
    fun fromId_resolves_the_add_item_shortcut() {
        assertEquals(AppShortcut.ADD_ITEM, AppShortcut.fromId("add_item"))
    }

    @Test
    fun fromId_resolves_the_try_it_shortcut() {
        assertEquals(AppShortcut.TRY_IT, AppShortcut.fromId("try_it"))
    }

    @Test
    fun fromId_returns_null_for_an_unknown_id() {
        assertNull(AppShortcut.fromId("outfits"))
    }

    @Test
    fun fromId_returns_null_for_a_missing_id() {
        assertNull(AppShortcut.fromId(null))
    }

    /** The ids cross a platform boundary as bare strings, so a rename must not go unnoticed. */
    @Test
    fun every_shortcut_round_trips_through_its_id() {
        AppShortcut.entries.forEach { shortcut ->
            assertEquals(shortcut, AppShortcut.fromId(shortcut.id))
        }
    }
}
