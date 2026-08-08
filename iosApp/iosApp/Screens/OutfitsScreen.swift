import SwiftUI
import Shared

struct OutfitsScreen: View {
    @StateObject private var viewModel = OutfitViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass
    var onTabSelected: (WornTab) -> Void = { _ in }
    @State private var showCreateSheet = false
    @State private var detailOutfit: Outfit?
    @State private var editOutfit: Outfit?

    var body: some View {
        OutfitsContent(
            state: viewModel.state,
            isCompact: sizeClass == .compact,
            onCreateClick: { showCreateSheet = true },
            onToggleSelection: { viewModel.toggleSelection($0) },
            onClearSelection: { viewModel.clearSelection() },
            onDeleteSelected: { viewModel.deleteSelected() },
            onOutfitClick: { detailOutfit = $0 },
            onTabSelected: onTabSelected
        )
        .sheet(isPresented: $showCreateSheet) {
            CreateOutfitSheet(
                clothingItems: viewModel.state.clothingItems,
                selectedItemIds: viewModel.state.selectedItemIds,
                activeCategory: viewModel.state.activeItemCategory,
                isSaving: viewModel.state.isSaving,
                existingOutfit: editOutfit,
                onCategorySelected: { viewModel.filterItemsByCategory($0) },
                onToggleItem: { viewModel.toggleItemSelection($0) },
                onSave: { name in
                    if let existing = editOutfit {
                        viewModel.updateOutfit(existing.doCopy(
                            id: existing.id, name: name,
                            itemIds: Array(viewModel.state.selectedItemIds),
                            createdAt: existing.createdAt
                        ))
                        editOutfit = nil
                    } else {
                        viewModel.createOutfit(name: name)
                    }
                },
                onDismiss: { showCreateSheet = false; editOutfit = nil }
            )
        }
        .sheet(item: $detailOutfit) { outfit in
            OutfitDetailSheet(
                outfit: outfit,
                clothingItems: viewModel.state.allClothingItems,
                isCompact: sizeClass == .compact,
                onEdit: { editingOutfit in
                    detailOutfit = nil
                    editOutfit = editingOutfit
                    // Pre-select outfit items
                    for itemId in editingOutfit.itemIds {
                        if !viewModel.state.selectedItemIds.contains(itemId) {
                            viewModel.toggleItemSelection(itemId)
                        }
                    }
                    showCreateSheet = true
                },
                onDelete: { id in
                    detailOutfit = nil
                    viewModel.deleteOutfit(id)
                }
            )
        }
        .onChange(of: viewModel.outfitCreated) { _, created in
            if created {
                showCreateSheet = false
                editOutfit = nil
                viewModel.outfitCreated = false
            }
        }
    }
}

struct OutfitsContent: View {
    let state: OutfitState
    var isCompact: Bool = true
    var onCreateClick: () -> Void = {}
    var onToggleSelection: (String) -> Void = { _ in }
    var onClearSelection: () -> Void = {}
    var onDeleteSelected: () -> Void = {}
    var onOutfitClick: (Outfit) -> Void = { _ in }
    var onTabSelected: (WornTab) -> Void = { _ in }

    private var contentPadding: CGFloat { isCompact ? 24 : 32 }
    private var sectionGap: CGFloat { isCompact ? 24 : 28 }
    private var isSelectionMode: Bool { !state.selectedIds.isEmpty }

    @State private var showDeleteDialog = false

    var body: some View {
        VStack(spacing: 0) {
            scrollContent
        }
        .background(WornColors.bgPage)
        .accessibilityIdentifier("outfits_screen")
        .deleteConfirmationAlert(
            title: String(format: String(localized: "delete_outfits_title"), state.selectedIds.count),
            message: String(localized: "outfits_delete_dialog_message"),
            isPresented: $showDeleteDialog,
            onConfirm: onDeleteSelected
        )
    }

    private var isEmpty: Bool { !state.isLoading && state.outfits.isEmpty }

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
                if isEmpty {
                    emptyState
                        .frame(maxWidth: .infinity)
                } else if state.isLoading && state.outfits.isEmpty {
                    HStack {
                        Spacer()
                        ProgressView().tint(WornColors.accentGreen)
                        Spacer()
                    }
                    .padding(.top, 60)
                } else {
                    outfitsList
                }
            }
            .padding(.horizontal, contentPadding)
            .padding(.top, 8)
            .padding(.bottom, 95)
        }
    }

    private var normalHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(String(localized: "outfits_title"))
                    .font((state.outfits.isEmpty ? Font.title2 : Font.title).weight(.semibold))
                    .tracking(-0.5)
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                if !state.outfits.isEmpty {
                    Button(action: onCreateClick) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                                .font(.system(size: 12, weight: .semibold))
                            Text(String(localized: "outfits_button_create"))
                                .font(.subheadline.weight(.semibold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(WornColors.accentGreen)
                        .clipShape(Capsule())
                    }
                    .accessibilityIdentifier("outfits_create_button")
                }
            }

            if !state.outfits.isEmpty {
                Text(String(format: String(localized: "saved_combinations"), state.outfits.count))
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(WornColors.textSecondary)
            }
        }
    }


    private var outfitsList: some View {
        LazyVStack(spacing: 12) {
            ForEach(state.outfits, id: \.id) { outfit in
                OutfitCardView(
                    outfit: outfit,
                    itemCategories: state.itemCategories,
                    isSelected: state.selectedIds.contains(outfit.id),
                    isSelectionMode: isSelectionMode
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
                .onTapGesture {
                    if isSelectionMode {
                        onToggleSelection(outfit.id)
                    } else {
                        onOutfitClick(outfit)
                    }
                }
                .onLongPressGesture {
                    onToggleSelection(outfit.id)
                }
            }
        }
        .animation(.easeInOut(duration: 0.3), value: state.outfits.map(\.id))
    }

    private var emptyState: some View {
        EmptyStateView(
            icon: {
                Image(systemName: "square.3.layers.3d")
                    .font(.system(size: 42, weight: .regular))
                    .foregroundColor(WornColors.textSecondary)
            },
            title: String(localized: "outfits_empty_title"),
            description: String(localized: "outfits_empty_description"),
            action: {
                WornGradientButton(
                    text: String(localized: "outfits_empty_cta"),
                    action: onCreateClick,
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
                .accessibilityIdentifier("outfits_empty_cta")
            }
        )
    }
}

private let outfitBadgeColors: [Color] = [
    WornColors.accentIndigo,
    WornColors.accentCoral,
    WornColors.accentGreen,
]

private struct OutfitCardView: View {
    let outfit: Outfit
    var itemCategories: [String: Shared.Category] = [:]
    var isSelected: Bool = false
    var isSelectionMode: Bool = false

    private var badgeColor: Color {
        let index = abs(outfit.id.hashValue) % outfitBadgeColors.count
        return outfitBadgeColors[index]
    }

    var body: some View {
        HStack(spacing: 12) {
            if isSelectionMode {
                SelectionIndicator(isSelected: isSelected)
            }
            VStack(spacing: 12) {
                thumbnailRow
                bottomRow
            }
        }
        .padding(20)
        .frame(height: 170)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(
                    isSelected ? WornColors.accentGreen : WornColors.borderSubtle,
                    lineWidth: 1
                )
        )
        .shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)
        .accessibilityIdentifier("outfit_card")
    }

    private var thumbnailRow: some View {
        HStack(spacing: 8) {
            let displayIds = Array(outfit.itemIds.prefix(4))
            ForEach(displayIds, id: \.self) { itemId in
                itemThumbnail(for: itemCategories[itemId])
            }
            Spacer()
            itemCountBadge
        }
    }

    private func itemThumbnail(for category: Shared.Category?) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(WornColors.bgElevated)
                .frame(width: 40, height: 40)
            Image(systemName: iconName(for: category))
                .font(.system(size: 16))
                .foregroundColor(WornColors.iconMuted)
        }
    }

    private func iconName(for category: Shared.Category?) -> String {
        switch category {
        case .top: return "tshirt"
        case .bottom: return "ruler"
        case .outerwear: return "wind"
        case .shoes: return "shoe"
        case .accessory: return "eyeglasses"
        default: return "tshirt"
        }
    }

    private var itemCountBadge: some View {
        Text(String(format: String(localized: "outfit_detail_items_count"), outfit.itemIds.count))
            .font(.caption2.weight(.semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(badgeColor)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var bottomRow: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 2) {
                // Auto-generated names concatenate every item, so they can outgrow the card.
                Text(outfit.name)
                    .font(.callout.weight(.semibold))
                    .foregroundColor(WornColors.textPrimary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Text(formatDate(outfit.createdAt))
                    .font(.caption)
                    .foregroundColor(WornColors.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 14))
                .foregroundColor(WornColors.iconMuted)
        }
    }


    private func formatDate(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: date)
    }
}

private let previewOutfits: [Outfit] = [
    Outfit(id: "1", name: "Weekend Casual", itemIds: ["i1", "i2", "i3", "i4"], createdAt: 1_710_460_800_000),
    Outfit(id: "2", name: "Office Ready", itemIds: ["i1", "i2", "i3"], createdAt: 1_710_201_600_000),
    Outfit(id: "3", name: "Evening Out", itemIds: ["i1", "i2", "i3", "i4", "i5"], createdAt: 1_709_856_000_000),
]

#Preview("iPhone") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone · Dark") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
    .preferredColorScheme(.dark)
}

#Preview("iPhone - Selection") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(["1", "3"]), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Selection · Dark") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(["1", "3"]), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
    .preferredColorScheme(.dark)
}

#Preview("iPhone - Empty") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Empty · Dark") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
    .preferredColorScheme(.dark)
}

#Preview("iPad Portrait", traits: .portrait) {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
}

#Preview("iPad Portrait - Empty", traits: .portrait) {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], allClothingItems: [], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
}
