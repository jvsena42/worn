package com.github.worn.ui

/**
 * Canonical registry of UI test-tag identifiers.
 *
 * These strings are applied to interactive/assertable components via `Modifier.testTag(...)`.
 * With `testTagsAsResourceId = true` set on the root semantics (see `App.kt`), each tag surfaces
 * as the `resource-id` of the node in the accessibility/layout tree, which is what the
 * `android layout` CLI and journey evaluation read to locate elements deterministically.
 *
 * The same identifiers are mirrored on iOS as `accessibilityIdentifier` values (see
 * `iosApp/iosApp/WornTestTags.swift`) so a single journey vocabulary describes both platforms.
 * Keep the three in sync: this file, the Swift mirror, and `journeys/`.
 */
object WornTestTags {
    // Bottom navigation
    const val BOTTOM_BAR = "bottom_bar"
    const val TAB_WARDROBE = "tab_wardrobe"
    const val TAB_OUTFITS = "tab_outfits"
    const val TAB_GAPS = "tab_gaps"
    const val TAB_TRY_IT = "tab_try_it"
    const val TAB_SETTINGS = "tab_settings"

    // Wardrobe
    const val WARDROBE_SCREEN = "wardrobe_screen"
    const val WARDROBE_ADD_FAB = "wardrobe_add_fab"
    const val WARDROBE_EMPTY_ADD_CTA = "wardrobe_empty_add_cta"
    const val CLOTHING_CARD = "clothing_card"

    // Add / Edit item sheet
    const val ADD_ITEM_SHEET = "add_item_sheet"
    const val ADD_ITEM_PHOTO_ZONE = "add_item_photo_zone"
    const val ADD_ITEM_AI_BADGE = "add_item_ai_badge"
    const val ADD_ITEM_NAME_FIELD = "add_item_name_field"
    const val ADD_ITEM_CATEGORY_DROPDOWN = "add_item_category_dropdown"
    const val ADD_ITEM_SAVE_BUTTON = "add_item_save_button"

    // Photo source dialog (shared by Add item and Try It)
    const val PHOTO_DIALOG = "photo_source_dialog"
    const val PHOTO_DIALOG_CAMERA = "photo_source_camera"
    const val PHOTO_DIALOG_GALLERY = "photo_source_gallery"

    // Outfits
    const val OUTFITS_SCREEN = "outfits_screen"
    const val OUTFITS_CREATE_BUTTON = "outfits_create_button"
    const val OUTFITS_EMPTY_CTA = "outfits_empty_cta"
    const val OUTFIT_CARD = "outfit_card"
    const val CREATE_OUTFIT_SHEET = "create_outfit_sheet"
    const val CREATE_OUTFIT_NAME_FIELD = "create_outfit_name_field"
    const val CREATE_OUTFIT_SAVE_BUTTON = "create_outfit_save_button"

    // Gaps
    const val GAPS_SCREEN = "gaps_screen"
    const val GAPS_BANNER = "gaps_banner"
    const val GAP_ADD_TO_WARDROBE = "gap_add_to_wardrobe"
    const val GAP_DISMISS = "gap_dismiss"

    // Try It
    const val TRY_IT_SCREEN = "try_it_screen"
    const val TRY_IT_CONNECT_CTA = "try_it_connect_cta"
    const val TRY_IT_UPLOAD_ZONE = "try_it_upload_zone"
    const val TRY_IT_ANALYZE_BUTTON = "try_it_analyze_button"

    // Settings
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_PROFILE_CARD = "settings_profile_card"
    const val SETTINGS_API_KEY_CARD = "settings_api_key_card"
    const val API_KEY_SHEET = "api_key_sheet"
    const val API_KEY_FIELD = "api_key_field"
    const val API_KEY_SAVE_BUTTON = "api_key_save_button"
    const val API_KEY_REMOVE_BUTTON = "api_key_remove_button"
    const val PROFILE_SHEET = "profile_sheet"
    const val PROFILE_SAVE_BUTTON = "profile_save_button"

    // AI locked sheet (shared)
    const val AI_LOCKED_SHEET = "ai_locked_sheet"
    const val AI_LOCKED_CTA = "ai_locked_cta"

    // Detail sheets
    const val ITEM_DETAIL_SHEET = "item_detail_sheet"
    const val ITEM_DETAIL_EDIT = "item_detail_edit"
    const val ITEM_DETAIL_DELETE = "item_detail_delete"
    const val OUTFIT_DETAIL_SHEET = "outfit_detail_sheet"
    const val OUTFIT_DETAIL_EDIT = "outfit_detail_edit"
    const val OUTFIT_DETAIL_DELETE = "outfit_detail_delete"

    // Delete confirmation dialog (shared)
    const val DELETE_DIALOG_CONFIRM = "delete_dialog_confirm"
    const val DELETE_DIALOG_CANCEL = "delete_dialog_cancel"
}
