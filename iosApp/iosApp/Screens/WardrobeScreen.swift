import SwiftUI
import Shared

struct WardrobeScreen: View {
    @StateObject private var viewModel = WardrobeViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass
    @State private var showAddSheet = false
    @State private var detailItem: ClothingItem?
    @State private var editItem: ClothingItem?
    var onTabSelected: (WornTab) -> Void = { _ in }

    var body: some View {
        WardrobeContent(
            state: viewModel.state,
            isCompact: sizeClass == .compact,
            onCategorySelected: { viewModel.filterByCategory($0) },
            onAddItemClick: { showAddSheet = true },
            onToggleSelection: { viewModel.toggleSelection($0) },
            onClearSelection: { viewModel.clearSelection() },
            onDeleteSelected: { viewModel.deleteSelected() },
            onItemClick: { detailItem = $0 },
            onTabSelected: onTabSelected
        )
        .sheet(isPresented: $showAddSheet) {
            AddItemSheet(
                isSaving: viewModel.state.isSaving,
                hasApiKey: viewModel.state.hasApiKey,
                existingItem: editItem,
                onSave: { data, name, category, colors, seasons, subcategory, fit, material in
                    if let existing = editItem {
                        viewModel.updateItem(existing.doCopy(
                            id: existing.id, name: name, category: category,
                            colors: colors, seasons: seasons, tags: existing.tags,
                            description: existing.description_,
                            subcategory: subcategory, fit: fit, material: material,
                            photoPath: existing.photoPath, createdAt: existing.createdAt
                        ))
                        editItem = nil
                    } else {
                        viewModel.addItem(
                            imageData: data, name: name, category: category,
                            colors: colors, seasons: seasons,
                            subcategory: subcategory, fit: fit, material: material
                        )
                    }
                },
                onDismiss: { showAddSheet = false; editItem = nil }
            )
        }
        .sheet(item: $detailItem) { item in
            ItemDetailSheet(
                item: item,
                isCompact: sizeClass == .compact,
                onEdit: { editingItem in
                    detailItem = nil
                    editItem = editingItem
                    showAddSheet = true
                },
                onDelete: { id in
                    detailItem = nil
                    viewModel.deleteItem(id)
                }
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
    var onItemClick: (ClothingItem) -> Void = { _ in }
    var onTabSelected: (WornTab) -> Void = { _ in }

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
            }

            if !isSelectionMode && !isWardrobeEmpty {
                addItemFab
                    .padding(.trailing, contentPadding)
                    .padding(.bottom, 110)
            }
        }
        .background(WornColors.bgPage)
        .alert(String(format: String(localized: "delete_items_title"), state.selectedIds.count), isPresented: $showDeleteDialog) {
            Button(String(localized: "common_cancel"), role: .cancel) {}
            Button(String(localized: "common_delete"), role: .destructive) { onDeleteSelected() }
        } message: {
            Text(String(localized: "wardrobe_delete_dialog_message"))
        }
    }

    private var isWardrobeEmpty: Bool { !state.isLoading && state.totalItemCount == 0 }
    private var isCategoryEmpty: Bool { !state.isLoading && state.items.isEmpty && state.totalItemCount > 0 }

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: sectionGap) {
                if isSelectionMode {
                    SelectionHeader(
                        count: Int(state.selectedIds.count),
                        onCancel: onClearSelection,
                        onDelete: { showDeleteDialog = true }
                    )
                } else {
                    normalHeader
                }
                if isWardrobeEmpty {
                    emptyState
                        .frame(maxWidth: .infinity)
                } else {
                    CategoryFilterChips(
                        activeCategory: state.activeCategory,
                        onCategorySelected: onCategorySelected
                    )
                    if isCategoryEmpty {
                        categoryEmptyState
                            .frame(maxWidth: .infinity)
                    } else {
                        gridSection
                    }
                }
            }
            .padding(.horizontal, contentPadding)
            .padding(.top, 8)
            .padding(.bottom, 95)
        }
    }

    private var normalHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            if state.totalItemCount == 0 {
                Text(String(localized: "wardrobe_title_empty"))
                    .font(.system(size: 22, weight: .semibold))
                    .tracking(-0.5)
                    .foregroundColor(WornColors.textPrimary)
            } else {
                Text(String(localized: "wardrobe_title"))
                    .font(.system(size: 28, weight: .semibold))
                    .tracking(-0.8)
                    .foregroundColor(WornColors.textPrimary)

                Text(String(format: String(localized: "wardrobe_subtitle"), state.totalItemCount))
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(WornColors.textSecondary)
            }
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
                            if isSelectionMode {
                                onToggleSelection(item.id)
                            } else {
                                onItemClick(item)
                            }
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

    private var categoryEmptyState: some View {
        VStack(spacing: 16) {
            Spacer().frame(height: 60)

            Image(systemName: "tshirt")
                .font(.system(size: 36, weight: .regular))
                .foregroundColor(WornColors.textSecondary.opacity(0.5))

            Text(String(localized: "wardrobe_category_empty"))
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(WornColors.textSecondary)

            Spacer()
        }
    }

    private var emptyState: some View {
        EmptyStateView(
            icon: {
                Image(systemName: "tshirt")
                    .font(.system(size: 42, weight: .regular))
                    .foregroundColor(WornColors.textSecondary)
            },
            title: String(localized: "wardrobe_empty_title"),
            description: String(localized: "wardrobe_empty_description"),
            action: {
                WornGradientButton(
                    text: String(localized: "wardrobe_empty_cta"),
                    action: onAddItemClick,
                    gradientColors: WornGradients.greenCta,
                    cornerRadius: 28,
                    shadowRadius: 10,
                    shadowColor: WornColors.accentIndigo.opacity(0.15),
                    shadowY: 6,
                    icon: AnyView(
                        Image(systemName: "plus")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(WornColors.bgPage)
                    ),
                    fillMaxWidth: false,
                    fixedHeight: nil,
                    contentPadding: EdgeInsets(top: 16, leading: 36, bottom: 16, trailing: 36)
                )
            }
        )
    }

    private var addItemFab: some View {
        Button(action: onAddItemClick) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .semibold))
                Text(String(localized: "wardrobe_fab_add"))
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(WornColors.textOnColor)
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(WornColors.accentGreen)
            .clipShape(Capsule())
            .shadow(color: WornColors.saveGradientStart.opacity(0.2), radius: 12, x: 0, y: 8)
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
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil, totalItemCount: Int32(previewItems.count)),
        isCompact: true
    )
}

#Preview("iPhone - Selection") {
    WardrobeContent(
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(["1", "3"]), activeCategory: nil, error: nil, totalItemCount: Int32(previewItems.count)),
        isCompact: true
    )
}

#Preview("iPhone - Empty") {
    WardrobeContent(
        state: WardrobeState(items: [], isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil, totalItemCount: 0),
        isCompact: true
    )
}

#Preview("iPad Portrait") {
    WardrobeContent(
        state: WardrobeState(items: previewItems, isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil, totalItemCount: Int32(previewItems.count)),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}

#Preview("iPad - Empty") {
    WardrobeContent(
        state: WardrobeState(items: [], isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: nil, error: nil, totalItemCount: 0),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}

#Preview("iPhone - Empty Category") {
    WardrobeContent(
        state: WardrobeState(items: [], isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: .top, error: nil, totalItemCount: Int32(previewItems.count)),
        isCompact: true
    )
}

#Preview("iPad - Empty Category") {
    WardrobeContent(
        state: WardrobeState(items: [], isLoading: false, isSaving: false, isDeleting: false, selectedIds: Set(), activeCategory: .top, error: nil, totalItemCount: Int32(previewItems.count)),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
