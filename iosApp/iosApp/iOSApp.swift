import SwiftUI
import Shared

@main
struct iOSApp: App {
    @State private var activeTab: WornTab = .wardrobe

    init() {
        KoinHelperKt.initKoin()
    }

    var body: some Scene {
        WindowGroup {
            VStack(spacing: 0) {
                TabView(selection: $activeTab) {
                    WardrobeScreen(onTabSelected: selectTab)
                        .tag(WornTab.wardrobe)
                    OutfitsScreen(onTabSelected: selectTab)
                        .tag(WornTab.outfits)
                    GapsScreen(onTabSelected: selectTab)
                        .tag(WornTab.gaps)
                    TryItScreen(onTabSelected: selectTab)
                        .tag(WornTab.tryIt)
                    SettingsScreen(onTabSelected: selectTab)
                        .tag(WornTab.settings)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                WornBottomBar(activeTab: activeTab, onTabSelected: selectTab)
            }
        }
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
