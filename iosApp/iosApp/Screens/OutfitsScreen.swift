import SwiftUI
import Shared

struct OutfitsScreen: View {
    @StateObject private var viewModel = OutfitViewModelWrapper()
    @Environment(\.horizontalSizeClass) var sizeClass
    var onTabSelected: (WornTab) -> Void = { _ in }
    @State private var showCreateSheet = false

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
            onTabSelected: onTabSelected
        )
        .sheet(isPresented: $showCreateSheet) {
            CreateOutfitSheet(
                clothingItems: viewModel.state.clothingItems,
                selectedItemIds: viewModel.state.selectedItemIds,
                activeCategory: viewModel.state.activeItemCategory,
                isSaving: viewModel.state.isSaving,
                onCategorySelected: { viewModel.filterItemsByCategory($0) },
                onToggleItem: { viewModel.toggleItemSelection($0) },
                onSave: { name in viewModel.createOutfit(name: name) },
                onDismiss: { showCreateSheet = false }
            )
        }
        .onChange(of: viewModel.outfitCreated) { _, created in
            if created {
                showCreateSheet = false
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
                    .background(Color(hex: "C45B4A"))
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
                    isSelected: state.selectedIds.contains(outfit.id),
                    isSelectionMode: isSelectionMode
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
                .onTapGesture {
                    if isSelectionMode { onToggleSelection(outfit.id) }
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
                    .shadow(color: Color(hex: "6B7B8E").opacity(0.08), radius: 15, x: 0, y: 0)
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
                        colors: [WornColors.accentGreen, Color(hex: "6B8A58")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(Capsule())
                .shadow(color: Color(hex: "6B7B8E").opacity(0.15), radius: 10, x: 0, y: 6)
            }

            Spacer()
        }
    }
}

private struct OutfitCardView: View {
    let outfit: Outfit
    var isSelected: Bool = false
    var isSelectionMode: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            if isSelectionMode {
                selectionIndicator
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(outfit.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(WornColors.textPrimary)
                HStack(spacing: 8) {
                    Text("\(outfit.itemIds.count) item\(outfit.itemIds.count != 1 ? "s" : "")")
                        .font(.system(size: 13))
                        .foregroundColor(WornColors.textSecondary)
                    Text("·")
                        .font(.system(size: 13))
                        .foregroundColor(WornColors.textMuted)
                    Text(formatDate(outfit.createdAt))
                        .font(.system(size: 13))
                        .foregroundColor(WornColors.textMuted)
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 14))
                .foregroundColor(WornColors.textMuted)
        }
        .padding(16)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    isSelected ? WornColors.accentGreen : WornColors.borderSubtle,
                    lineWidth: 1
                )
        )
        .shadow(color: .black.opacity(0.04), radius: 4, x: 0, y: 2)
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
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Selection") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(["1", "3"]), error: nil, clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPhone - Empty") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: true
    )
}

#Preview("iPad Portrait") {
    OutfitsContent(
        state: OutfitState(outfits: previewOutfits, isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}

#Preview("iPad - Empty") {
    OutfitsContent(
        state: OutfitState(outfits: [], isLoading: false, isDeleting: false, selectedIds: Set(), error: nil, clothingItems: [], selectedItemIds: Set(), activeItemCategory: nil, isSaving: false, isLoadingItems: false),
        isCompact: false
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
