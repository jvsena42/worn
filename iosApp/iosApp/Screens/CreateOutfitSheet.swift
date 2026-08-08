import SwiftUI
import Shared

struct CreateOutfitSheet: View {
    let clothingItems: [ClothingItem]
    let selectedItemIds: Set<String>
    let activeCategory: Shared.Category?
    let isSaving: Bool
    var existingOutfit: Outfit?
    let onCategorySelected: (Shared.Category?) -> Void
    let onToggleItem: (String) -> Void
    let onSave: (String) -> Void
    let onDismiss: () -> Void

    @State private var name = ""
    @State private var didInitFromExisting = false

    // The name is optional — an empty one is filled in with the selected items' names.
    private var canSave: Bool {
        !selectedItemIds.isEmpty && !isSaving
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
            .navigationTitle(existingOutfit != nil ? String(localized: "create_outfit_title_edit") : String(localized: "create_outfit_title"))
            .onAppear {
                if let outfit = existingOutfit, !didInitFromExisting {
                    didInitFromExisting = true
                    name = outfit.name
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common_cancel"), action: onDismiss)
                }
            }
        }
        .accessibilityIdentifier("create_outfit_sheet")
    }

    private var nameField: some View {
        TextField(String(localized: "create_outfit_name_hint"), text: $name)
            .font(.subheadline)
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier("create_outfit_name_field")
    }

    private var selectItemsHeader: some View {
        HStack {
            Text(String(localized: "create_outfit_select_items"))
                .font(.callout.weight(.semibold))
                .foregroundColor(WornColors.textPrimary)
            Spacer()
            if !selectedItemIds.isEmpty {
                Text(String(format: String(localized: "selected_count"), selectedItemIds.count))
                    .font(.footnote.weight(.medium))
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
        WornGradientButton(
            text: isSaving ? String(localized: "common_saving") : (existingOutfit != nil ? String(localized: "common_save_changes") : String(localized: "create_outfit_save")),
            action: { onSave(name) },
            enabled: canSave,
            gradientColors: WornGradients.green,
            shadowRadius: 12,
            shadowColor: WornColors.saveGradientStart.opacity(0.2),
            shadowY: 8
        )
        .accessibilityIdentifier("create_outfit_save_button")
    }
}

private struct SelectableItemCell: View {
    let item: ClothingItem
    let isSelected: Bool
    let onTap: () -> Void

    // The cell is the photo alone: a name label here only ever sat on top of the garment, where it
    // was unreadable. The photo carries the item's name as its accessibility label.
    var body: some View {
        ZStack(alignment: .topLeading) {
            StoredPhotoImage(path: item.photoPath) { placeholderIcon }
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

            SelectionIndicator(isSelected: isSelected, size: 20, iconSize: 10)
                .padding(8)
        }
        .frame(height: 100)
        .accessibilityIdentifier("outfit_item_cell")
        .onTapGesture(perform: onTap)
    }

    private var placeholderIcon: some View {
        Image(systemName: "tshirt")
            .font(.system(size: 28))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

}

private let previewItems: [ClothingItem] = [
    ClothingItem(id: "1", name: "Black T-Shirt", category: .top, colors: ["black"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "2", name: "Navy Jeans", category: .bottom, colors: ["navy"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "3", name: "White Sneakers", category: .shoes, colors: ["white"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "4", name: "Grey Hoodie", category: .top, colors: ["grey"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "5", name: "Olive Jacket", category: .outerwear, colors: ["olive"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "6", name: "Chinos", category: .bottom, colors: ["khaki"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
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

#Preview("iPhone · Dark") {
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
    .preferredColorScheme(.dark)
}

#Preview("iPad Portrait", traits: .portrait) {
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
