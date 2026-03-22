import SwiftUI
import Shared

struct WardrobeScreen: View {
    @StateObject private var viewModel = WardrobeViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass

    private var isCompact: Bool { sizeClass == .compact }
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
                    activeCategory: viewModel.state.activeCategory,
                    onCategorySelected: { category in
                        viewModel.filterByCategory(category)
                    }
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

            Text("Your capsule wardrobe · \(viewModel.state.items.count) items")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
        }
    }

    private var gridSection: some View {
        Group {
            if viewModel.state.isLoading {
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
                    ForEach(viewModel.state.items, id: \.id) { item in
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
