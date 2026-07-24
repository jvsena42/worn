import Foundation

/// Canonical registry of UI test identifiers for the iOS app.
///
/// These values mirror `composeApp/.../ui/WornTestTags.kt` exactly. On iOS they are
/// applied to interactive/assertable SwiftUI views via `.accessibilityIdentifier(...)`,
/// which surfaces them in the accessibility tree so UI journey tests can locate elements
/// deterministically — the same role that `Modifier.testTag(...)` serves on Android.
///
/// Keep this file in sync with the Kotlin registry so a single journey vocabulary
/// describes both platforms.
enum WornTestTags {
    // Bottom navigation
    static let bottomBar = "bottom_bar"
    static let tabWardrobe = "tab_wardrobe"
    static let tabOutfits = "tab_outfits"
    static let tabGaps = "tab_gaps"
    static let tabTryIt = "tab_try_it"
    static let tabSettings = "tab_settings"

    // Wardrobe
    static let wardrobeScreen = "wardrobe_screen"
    static let wardrobeAddFab = "wardrobe_add_fab"
    static let wardrobeEmptyAddCta = "wardrobe_empty_add_cta"
    static let clothingCard = "clothing_card"

    // Add / Edit item sheet
    static let addItemSheet = "add_item_sheet"
    static let addItemPhotoZone = "add_item_photo_zone"
    static let addItemAiBadge = "add_item_ai_badge"
    static let addItemNameField = "add_item_name_field"
    static let addItemCategoryDropdown = "add_item_category_dropdown"
    static let addItemSaveButton = "add_item_save_button"

    // Photo source dialog (shared by Add item and Try It)
    static let photoSourceDialog = "photo_source_dialog"
    static let photoSourceCamera = "photo_source_camera"
    static let photoSourceGallery = "photo_source_gallery"

    // Outfits
    static let outfitsScreen = "outfits_screen"
    static let outfitsCreateButton = "outfits_create_button"
    static let outfitsEmptyCta = "outfits_empty_cta"
    static let outfitCard = "outfit_card"
    static let createOutfitSheet = "create_outfit_sheet"
    static let createOutfitNameField = "create_outfit_name_field"
    static let createOutfitSaveButton = "create_outfit_save_button"

    // Gaps
    static let gapsScreen = "gaps_screen"
    static let gapsBanner = "gaps_banner"
    static let gapAddToWardrobe = "gap_add_to_wardrobe"
    static let gapDismiss = "gap_dismiss"

    // Try It
    static let tryItScreen = "try_it_screen"
    static let tryItConnectCta = "try_it_connect_cta"
    static let tryItUploadZone = "try_it_upload_zone"
    static let tryItAnalyzeButton = "try_it_analyze_button"

    // Settings
    static let settingsScreen = "settings_screen"
    static let settingsProfileCard = "settings_profile_card"
    static let settingsApiKeyCard = "settings_api_key_card"
    static let apiKeySheet = "api_key_sheet"
    static let apiKeyField = "api_key_field"
    static let apiKeySaveButton = "api_key_save_button"
    static let apiKeyRemoveButton = "api_key_remove_button"
    static let profileSheet = "profile_sheet"
    static let profileSaveButton = "profile_save_button"

    // AI locked sheet (shared)
    static let aiLockedSheet = "ai_locked_sheet"
    static let aiLockedCta = "ai_locked_cta"

    // Detail sheets
    static let itemDetailSheet = "item_detail_sheet"
    static let itemDetailEdit = "item_detail_edit"
    static let itemDetailDelete = "item_detail_delete"
    static let outfitDetailSheet = "outfit_detail_sheet"
    static let outfitDetailEdit = "outfit_detail_edit"
    static let outfitDetailDelete = "outfit_detail_delete"

    // Delete confirmation dialog (shared)
    static let deleteDialogConfirm = "delete_dialog_confirm"
    static let deleteDialogCancel = "delete_dialog_cancel"
}
