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
                    WardrobeScreen(onTabSelected: { activeTab = $0 })
                        .tag(WornTab.wardrobe)
                    OutfitsScreen(onTabSelected: { activeTab = $0 })
                        .tag(WornTab.outfits)
                    GapsScreen(onTabSelected: { activeTab = $0 })
                        .tag(WornTab.gaps)
                    TryItScreen(onTabSelected: { activeTab = $0 })
                        .tag(WornTab.tryIt)
                    SettingsScreen(onTabSelected: { activeTab = $0 })
                        .tag(WornTab.settings)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                WornBottomBar(activeTab: activeTab, onTabSelected: { activeTab = $0 })
            }
        }
    }
}
