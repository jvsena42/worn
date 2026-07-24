import SwiftUI

enum WornTab: String, CaseIterable {
    case wardrobe
    case outfits
    case gaps
    case tryIt
    case settings

    var icon: String {
        switch self {
        case .wardrobe: return "tshirt"
        case .outfits: return "square.3.layers.3d"
        case .gaps: return "puzzlepiece.extension"
        case .tryIt: return "viewfinder"
        case .settings: return "gearshape"
        }
    }

    var label: String {
        switch self {
        case .wardrobe: return String(localized: "tab_wardrobe")
        case .outfits: return String(localized: "tab_outfits")
        case .gaps: return String(localized: "tab_gaps")
        case .tryIt: return String(localized: "tab_try_it")
        case .settings: return String(localized: "tab_settings")
        }
    }

    var testTag: String {
        switch self {
        case .wardrobe: return "tab_wardrobe"
        case .outfits: return "tab_outfits"
        case .gaps: return "tab_gaps"
        case .tryIt: return "tab_try_it"
        case .settings: return "tab_settings"
        }
    }
}

struct WornBottomBar: View {
    let activeTab: WornTab
    let onTabSelected: (WornTab) -> Void
    var isCompact: Bool = true

    var body: some View {
        HStack {
            if !isCompact { Spacer() }

            HStack(spacing: 0) {
                ForEach(WornTab.allCases, id: \.self) { tab in
                    TabItem(tab: tab, isActive: tab == activeTab) {
                        onTabSelected(tab)
                    }
                    .accessibilityIdentifier(tab.testTag)
                }
            }
            .frame(maxWidth: isCompact ? .infinity : 480)
            .frame(height: 62)
            .padding(4)
            .background(WornColors.bgElevated)
            .clipShape(RoundedRectangle(cornerRadius: 36))
            .overlay(
                RoundedRectangle(cornerRadius: 36)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier("bottom_bar")

            if !isCompact { Spacer() }
        }
        .padding(.horizontal, isCompact ? 21 : 32)
        .padding(.top, 12)
        .padding(.bottom, 21)
    }
}

private struct TabItem: View {
    let tab: WornTab
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                Image(systemName: tab.icon)
                    .font(.system(size: 18))
                Text(tab.label)
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(0.5)
            }
            .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                isActive
                    ? WornColors.accentGreen
                    : Color.clear
            )
            .clipShape(RoundedRectangle(cornerRadius: 26))
        }
        .buttonStyle(.plain)
    }
}
