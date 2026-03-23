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
            switch activeTab {
            case .wardrobe:
                WardrobeScreen(onTabSelected: { activeTab = $0 })
            case .outfits:
                OutfitsScreen(onTabSelected: { activeTab = $0 })
            default:
                WardrobeScreen(onTabSelected: { activeTab = $0 })
            }
        }
    }
}
