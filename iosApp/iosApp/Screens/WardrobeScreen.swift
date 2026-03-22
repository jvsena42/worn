import SwiftUI
import Shared

struct WardrobeScreen: View {
    @StateObject private var viewModel = WardrobeViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass
    @State private var showAddSheet = false

    var body: some View {
        WardrobeContent(
            state: viewModel.state,
            isCompact: sizeClass == .compact,
            onCategorySelected: { viewModel.filterByCategory($0) },
            onAddItemClick: { showAddSheet = true },
            onToggleSelection: { viewModel.toggleSelection($0) },
            onClearSelection: { viewModel.clearSelection() },
            onDeleteSelected: { viewModel.deleteSelected() }
        )
        .sheet(isPresented: $showAddSheet) {
            AddItemSheet(
                isSaving: viewModel.state.isSaving,
                hasApiKey: viewModel.state.hasApiKey,
                onSave: { data, name, category, colors, seasons in
                    viewModel.addItem(
                        imageData: data, name: name, category: category,
                        colors: colors, seasons: seasons
                    )
                },
                onDismiss: { showAddSheet = false }
            )
        }
    }
}

struct WardrobeContent: View {
    let state: WardrobeState
    var isCompact: Bool = true
    var onCategorySelected: (Category?) -> Void = { _ in }
    var onAddItemClick: () -> Void = {}
    var onToggleSelection: (String) -> Void = { _ in }
    var onClearSelection: () -> Void = {}
    var onDeleteSelected: () -> Void = {}

    private var contentPadding: CGFloat { isCompact ? 24 : 32 }
    private var gridGap: CGFloat { isCompact ? 12 : 16 }
    private var photoHeight: CGFloat { isCompact ? 171 : 200 }
    private var sectionGap: CGFloat { isCompact ? 24 : 28 }
    private var isSelectionMode: Bool { !state.selectedIds.isEmpty }

    @State private var showDeleteDialog = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                scrollContent
                WornBottomBar(
                    activeTab: .wardrobe, onTabSelected: { _ in }, isCompact: isCompact
                )
            }

            if !isSelectionMode {
                addItemFab
                    .padding(.trailing, contentPadding)
                    .padding(.bottom, 110)
            }
        }
        .background(WornColors.bgPage)
        .alert("Delete \(state.selectedIds.count) item\(state.selectedIds.count != 1 ? "s" : "")?", isPresented: $showDeleteDialog) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) { onDeleteSelected() }
        } message: {
            Text("This action cannot be undone. The selected items will be permanently removed from your wardrobe.")
        }
    }

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: sectionGap) {
                if isSelectionMode {
                    selectionHeader
                } else {
                    normalHeader
                }
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

    private var normalHeader: some View {
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

    private var selectionHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(state.selectedIds.count) selected")
                    .font(.system(size: 28, weight: .medium))
                    .tracking(-0.8)
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                Button {
                    showDeleteDialog = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "trash")
                            .font(.system(size: 15))
                        Text("Delete")
                            .font(.system(size: 15, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Color(hex: "C45B4A"))
                    .clipShape(Capsule())
                }
            }
            Button("Cancel") { onClearSelection() }
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
        }
    }

    private var gridSection: some View {
        Group {
            if state.isLoading && state.items.isEmpty {
                HStack {
                    Spacer()
                    ProgressView().tint(WornColors.accentGreen)
                    Spacer()
                }
                .padding(.top, 60)
            } else {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 160), spacing: gridGap)],
                    spacing: gridGap
                ) {
                    ForEach(state.items, id: \.id) { item in
                        ClothingCard(
                            item: item,
                            photoHeight: photoHeight,
                            isSelected: state.selectedIds.contains(item.id),
                            isSelectionMode: isSelectionMode
                        )
                        .transition(.opacity.combined(with: .scale(scale: 0.95)))
                        .onTapGesture {
                            if isSelectionMode { onToggleSelection(item.id) }
                        }
                        .onLongPressGesture {
                            onToggleSelection(item.id)
                        }
                    }
                }
                .animation(.easeInOut(duration: 0.3), value: state.items.map(\.id))
            }
        }
    }

    private var addItemFab: some View {
        Button(action: onAddItemClick) {
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

#Preview("iPhone") {
    WardrobeContent(
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil),
        isCompact: true
    )
}

#Preview("iPhone - Selection") {
    WardrobeContent(
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(["1", "3"]), activeCategory: nil, error: nil),
        isCompact: true
    )
}

#Preview("iPad Portrait") {
    WardrobeContent(
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
