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
            onCreateClick: {
                viewModel.loadClothingItems()
                showCreateSheet = true
            },
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
                    viewModel.loadClothingItems()
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
        .onAppear {
            viewModel.loadOutfits()
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
            WornBottomBar(
                activeTab: .outfits, onTabSelected: onTabSelected, isCompact: isCompact
            )
        }
        .background(WornColors.bgPage)
        .alert(
            "Delete \(state.selectedIds.count) outfit\(state.selectedIds.count != 1 ? "s" : "")?",
            isPresented: $showDeleteDialog
        ) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) { onDeleteSelected() }
        } message: {
            Text("This action cannot be undone. The selected outfits will be permanently removed.")
        }
    }

    private var isEmpty: Bool { !state.isLoading && state.outfits.isEmpty }

    private var scrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: sectionGap) {
                if isSelectionMode {
                    selectionHeader
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
        }
    }

    private var normalHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Your outfits")
                    .font(.system(size: state.outfits.isEmpty ? 22 : 28, weight: .semibold))
                    .tracking(-0.5)
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                if !state.outfits.isEmpty {
                    Button(action: onCreateClick) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                                .font(.system(size: 12, weight: .semibold))
                            Text("Create")
                                .font(.system(size: 14, weight: .semibold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(WornColors.accentGreen)
                        .clipShape(Capsule())
                    }
                }
            }

            if !state.outfits.isEmpty {
                Text("\(state.outfits.count) saved combination\(state.outfits.count != 1 ? "s" : "")")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(WornColors.textSecondary)
            }
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
                    .background(WornColors.deleteRed)
                    .clipShape(Capsule())
                }
            }
            Button("Cancel") { onClearSelection() }
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
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
        VStack(spacing: 24) {
            Spacer().frame(height: 60)

            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 130, height: 130)
                    .shadow(color: WornColors.accentIndigo.opacity(0.08), radius: 15, x: 0, y: 0)
                    .overlay(
                        Circle()
                            .stroke(WornColors.borderSubtle, lineWidth: 1)
                    )

                Image(systemName: "square.3.layers.3d")
                    .font(.system(size: 42, weight: .regular))
                    .foregroundColor(WornColors.textSecondary)
            }

            Text("No outfits yet")
                .font(.system(size: 24, weight: .semibold))
                .tracking(-0.5)
                .foregroundColor(WornColors.textPrimary)

            Text("Create your first look by combining\nitems from your wardrobe")
                .font(.system(size: 15))
                .lineSpacing(4)
                .multilineTextAlignment(.center)
                .foregroundColor(WornColors.textSecondary)

            Button(action: onCreateClick) {
                HStack(spacing: 8) {
                    Image(systemName: "plus")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(WornColors.bgPage)
                    Text("Create your first outfit")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(WornColors.textOnColor)
                }
                .padding(.horizontal, 36)
                .padding(.vertical, 16)
                .background(
                    LinearGradient(
                        colors: [WornColors.accentGreen, WornColors.accentGreenEnd],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(Capsule())
                .shadow(color: WornColors.accentIndigo.opacity(0.15), radius: 10, x: 0, y: 6)
            }

            Spacer()
        }
    }
}

private let outfitBadgeColors: [Color] = [
    WornColors.accentIndigo,
    WornColors.accentCoral,
    WornColors.accentGreen,
]

private struct OutfitCardView: View {
    let outfit: Outfit
    var itemCategories: [String: Category] = [:]
    var isSelected: Bool = false
    var isSelectionMode: Bool = false

    private var badgeColor: Color {
        let index = abs(outfit.id.hashValue) % outfitBadgeColors.count
        return outfitBadgeColors[index]
    }

    var body: some View {
        HStack(spacing: 12) {
            if isSelectionMode {
                selectionIndicator
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

    private func itemThumbnail(for category: Category?) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(WornColors.bgElevated)
                .frame(width: 40, height: 40)
            Image(systemName: iconName(for: category))
                .font(.system(size: 16))
                .foregroundColor(WornColors.iconMuted)
        }
    }

    private func iconName(for category: Category?) -> String {
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
        Text("\(outfit.itemIds.count) items")
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(badgeColor)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var bottomRow: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 2) {
                Text(outfit.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(WornColors.textPrimary)
                Text(formatDate(outfit.createdAt))
                    .font(.system(size: 12))
                    .foregroundColor(WornColors.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 14))
                .foregroundColor(WornColors.iconMuted)
        }
    }

    private var selectionIndicator: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14)
                .fill(isSelected ? WornColors.accentGreen : WornColors.bgCard)
                .frame(width: 28, height: 28)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(
                            isSelected ? Color.clear : WornColors.borderSubtle,
                            lineWidth: 1.5
                        )
                )
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
            }
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
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Selection") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(["1", "3"]), error: nil, itemCategories: [:], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Empty") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPad Portrait") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}

#Preview("iPad - Empty") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, itemCategories: [:], clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
