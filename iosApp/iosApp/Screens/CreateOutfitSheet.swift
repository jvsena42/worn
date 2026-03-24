import SwiftUI
import Shared

struct CreateOutfitSheet: View {
    let clothingItems: [ClothingItem]
    let selectedItemIds: Set<String>
    let activeCategory: Category?
    let isSaving: Bool
    let onCategorySelected: (Category?) -> Void
    let onToggleItem: (String) -> Void
    let onSave: (String) -> Void
    let onDismiss: () -> Void

    @State private var name = ""

    private var canSave: Bool {
        !name.isEmpty && !selectedItemIds.isEmpty && !isSaving
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    nameField

                    selectItemsHeader

                    CategoryFilterChips(
                        activeCategory: activeCategory,
                        onCategorySelected: onCategorySelected
                    )

                    itemGrid

                    saveButton
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
            .navigationTitle("Create outfit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
            }
        }
    }

    private var nameField: some View {
        TextField("Outfit name", text: $name)
            .font(.system(size: 15))
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
    }

    private var selectItemsHeader: some View {
        HStack {
            Text("Select items")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
            Spacer()
            if !selectedItemIds.isEmpty {
                Text("\(selectedItemIds.count) selected")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(WornColors.accentGreen)
            }
        }
    }

    private var itemGrid: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 100), spacing: 12)],
            spacing: 12
        ) {
            ForEach(clothingItems, id: \.id) { item in
                SelectableItemCell(
                    item: item,
                    isSelected: selectedItemIds.contains(item.id),
                    onTap: { onToggleItem(item.id) }
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            }
        }
        .animation(.easeInOut(duration: 0.3), value: clothingItems.map(\.id))
    }

    private var saveButton: some View {
        Button {
            onSave(name)
        } label: {
            Text(isSaving ? "Saving…" : "Save outfit")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    LinearGradient(
                        colors: canSave
                            ? [WornColors.accentGreen, WornColors.accentGreenDark]
                            : [WornColors.textMuted, WornColors.iconMuted],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: WornColors.saveGradientStart.opacity(0.2), radius: 12, x: 0, y: 8)
        }
        .disabled(!canSave)
    }
}

private struct SelectableItemCell: View {
    let item: ClothingItem
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            ZStack {
                if FileManager.default.fileExists(atPath: item.photoPath) {
                    let url = URL(fileURLWithPath: item.photoPath)
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        placeholderIcon
                    }
                    .frame(maxWidth: .infinity, maxHeight: 100)
                    .clipped()
                } else {
                    placeholderIcon
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 100)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        isSelected ? WornColors.accentGreen : WornColors.borderSubtle,
                        lineWidth: isSelected ? 2 : 1
                    )
            )
            .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)

            selectionCheckbox
                .padding(8)

            VStack {
                Spacer()
                Text(item.name)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(WornColors.textPrimary)
                    .padding(.horizontal, 12)
                    .padding(.bottom, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(height: 100)
        .onTapGesture(perform: onTap)
    }

    private var placeholderIcon: some View {
        Image(systemName: "tshirt")
            .font(.system(size: 28))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var selectionCheckbox: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(isSelected ? WornColors.accentGreen : WornColors.bgCard)
                .frame(width: 20, height: 20)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(
                            isSelected ? Color.clear : WornColors.borderSubtle,
                            lineWidth: 1.5
                        )
                )
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white)
            }
        }
    }
}

private let previewItems: [ClothingItem] = [
    ClothingItem(id: "1", name: "Black T-Shirt", category: .top, colors: ["black"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "2", name: "Navy Jeans", category: .bottom, colors: ["navy"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "3", name: "White Sneakers", category: .shoes, colors: ["white"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "4", name: "Grey Hoodie", category: .top, colors: ["grey"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "5", name: "Olive Jacket", category: .outerwear, colors: ["olive"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "6", name: "Chinos", category: .bottom, colors: ["khaki"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
]

#Preview("iPhone") {
    CreateOutfitSheet(
        clothingItems: previewItems,
        selectedItemIds: Set(["1", "2"]),
        activeCategory: nil,
        isSaving: false,
        onCategorySelected: { _ in },
        onToggleItem: { _ in },
        onSave: { _ in },
        onDismiss: {}
    )
}

#Preview("iPad Portrait") {
    CreateOutfitSheet(
        clothingItems: previewItems,
        selectedItemIds: Set(["1", "2"]),
        activeCategory: nil,
        isSaving: false,
        onCategorySelected: { _ in },
        onToggleItem: { _ in },
        onSave: { _ in },
        onDismiss: {}
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
