import SwiftUI
import Shared

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var quickActions = QuickActionInbox.shared
    @State private var activeTab: WornTab = .wardrobe
    @State private var sharedPhoto: SharedPhoto?
    @State private var openAddSheet: ShortcutCommand?
    @State private var handledShortcutId: UUID?

    init() {
        KoinHelperKt.initKoin()
        // FoundationModels is Swift-only, so the shared engine reaches Apple Intelligence through
        // this bridge. Registered before any screen can resolve the engine from Koin.
        OnDeviceAiBridgeRegistry.shared.bridge = OnDeviceAiService()
        QuickActionInbox.register()
    }

    var body: some Scene {
        WindowGroup {
            VStack(spacing: 0) {
                TabView(selection: $activeTab) {
                    WardrobeScreen(
                        onTabSelected: selectTab,
                        openAddSheet: openAddSheet,
                        onAddSheetOpened: { openAddSheet = nil }
                    )
                        .tag(WornTab.wardrobe)
                    OutfitsScreen(onTabSelected: selectTab)
                        .tag(WornTab.outfits)
                    GapsScreen(onTabSelected: selectTab)
                        .tag(WornTab.gaps)
                    TryItScreen(onTabSelected: selectTab, sharedPhoto: sharedPhoto)
                        .tag(WornTab.tryIt)
                    SettingsScreen(onTabSelected: selectTab)
                        .tag(WornTab.settings)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                WornBottomBar(activeTab: activeTab, onTabSelected: selectTab)
            }
            .onOpenURL { _ in receiveSharedPhoto() }
            .onChange(of: scenePhase) { _, phase in
                // Covers the case where the extension wrote the file but could not launch us.
                if phase == .active { receiveSharedPhoto() }
            }
            // onReceive, not onChange: on a cold launch the scene delegate fills the inbox before
            // this view installs its observers, and @Published replays its current value to a new
            // subscriber whereas onChange would only see later ones.
            //
            // The tap is marked handled here rather than cleared on the inbox, because that replay
            // can land mid-update and SwiftUI forbids publishing changes from there. Every tap
            // carries a fresh id, so a repeat of the same action still gets through.
            .onReceive(quickActions.$pending) { command in
                guard let command, command.id != handledShortcutId else { return }
                handledShortcutId = command.id
                perform(command)
            }
        }
    }

    private func perform(_ command: ShortcutCommand) {
        // A Kotlin enum bridges to Swift as a class, so it is compared rather than switched on.
        if command.shortcut == .addItem {
            selectTab(.wardrobe)
            openAddSheet = command
        } else if command.shortcut == .tryIt {
            selectTab(.tryIt)
        }
    }

    private func receiveSharedPhoto() {
        guard let photo = SharedPhotoInbox.consume() else { return }
        sharedPhoto = photo
        selectTab(.tryIt)
    }

    /// Switches tabs without the paged TabView's slide animation.
    ///
    /// A paged TabView animates a selection change by scrolling through the pages in between, so
    /// jumping from Wardrobe to Settings builds Gaps and Try-It on the way. Snapping builds only
    /// the destination. Swiping still animates, because the gesture drives it directly.
    private func selectTab(_ tab: WornTab) {
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction) { activeTab = tab }
    }
}
