import SwiftUI
import Shared

struct WardrobeScreen: View {
    @StateObject private var viewModel = WardrobeViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass

    var body: some View {
        WardrobeContent(
            state: viewModel.state,
            isCompact: sizeClass == .compact,
            onCategorySelected: { viewModel.filterByCategory($0) }
        )
    }
}

struct WardrobeContent: View {
    let state: WardrobeState
    var isCompact: Bool = true
    var onCategorySelected: (Category?) -> Void = { _ in }

    private var contentPadding: CGFloat { isCompact ? 24 : 32 }
    private var gridGap: CGFloat { isCompact ? 12 : 16 }
    private var photoHeight: CGFloat { isCompact ? 171 : 200 }
    private var sectionGap: CGFloat { isCompact ? 24 : 28 }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                scrollContent
                WornBottomBar(
                    activeTab: .wardrobe,
                    onTabSelected: { _ in },
                    isCompact: isCompact
                )
            }

            addItemFab
                .padding(.trailing, contentPadding)
                .padding(.bottom, 110)
        }
        .background(WornColors.bgPage)
    }

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: sectionGap) {
                headerSection
                CategoryFilterChips(
                    activeCategory: state.activeCategory,
                    onCategorySelected: onCategorySelected
                )
                gridSection
            }
            .padding(.horizontal, contentPadding)
            .padding(.top, 8)
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Worn")
                .font(.system(size: 28, weight: .semibold))
                .tracking(-0.8)
                .foregroundColor(WornColors.textPrimary)

            Text("Your capsule wardrobe · \(state.items.count) items")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
        }
    }

    private var gridSection: some View {
        Group {
            if state.isLoading {
                HStack {
                    Spacer()
                    ProgressView()
                        .tint(WornColors.accentGreen)
                    Spacer()
                }
                .padding(.top, 60)
            } else {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 160), spacing: gridGap)],
                    spacing: gridGap
                ) {
                    ForEach(state.items, id: \.id) { item in
                        ClothingCard(item: item, photoHeight: photoHeight)
                    }
                }
            }
        }
    }

    private var addItemFab: some View {
        Button(action: { }) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .semibold))
                Text("Add item")
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(WornColors.textOnColor)
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(WornColors.accentGreen)
            .clipShape(Capsule())
            .shadow(color: Color(hex: "8FA47D").opacity(0.2), radius: 12, x: 0, y: 8)
        }
    }
}

private let previewItems: [ClothingItem] = [
    ClothingItem(id: "1", name: "Black T-Shirt", category: .top, colors: ["black"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "2", name: "Navy Jeans", category: .bottom, colors: ["navy"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "3", name: "White Sneakers", category: .shoes, colors: ["white"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "4", name: "Olive Jacket", category: .outerwear, colors: ["olive"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "5", name: "Grey Hoodie", category: .top, colors: ["grey"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "6", name: "Chinos", category: .bottom, colors: ["khaki"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
]

private let previewState = WardrobeState(
    items: previewItems,
    isLoading: false,
    activeCategory: nil,
    error: nil
)

#Preview("iPhone") {
    WardrobeContent(
        state: previewState,
        isCompact: true
    )
}

#Preview("iPad Portrait") {
    WardrobeContent(
        state: previewState,
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
