import SwiftUI
import Shared

struct OutfitDetailSheet: View {
    let outfit: Outfit
    let clothingItems: [ClothingItem]
    var isCompact: Bool = true
    let onEdit: (Outfit) -> Void
    let onDelete: (String) -> Void

    @State private var showDeleteAlert = false

    private var contentPadding: CGFloat { isCompact ? 24 : 32 }
    private var sectionGap: CGFloat { isCompact ? 20 : 24 }
    private var nameFont: Font { isCompact ? .title2 : .title }
    private var cardSize: CGFloat { isCompact ? 200 : 300 }
    private var cardRadius: CGFloat { isCompact ? 18 : 20 }
    private var cardGap: CGFloat { isCompact ? 12 : 16 }
    private var propFont: Font { isCompact ? .subheadline : .callout }
    private var propGap: CGFloat { isCompact ? 14 : 16 }
    private var buttonHeight: CGFloat { isCompact ? 48 : 52 }
    private var buttonFont: Font { isCompact ? .subheadline : .callout }

    private var outfitItems: [ClothingItem] {
        outfit.itemIds.compactMap { id in
            clothingItems.first { $0.id == id }
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: sectionGap) {
                // Title
                Text(outfit.name)
                    .font(nameFont.weight(.semibold))
                    .foregroundColor(WornColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, contentPadding)

                // Items Preview
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: cardGap) {
                        ForEach(outfitItems, id: \.id) { item in
                            outfitItemCard(item: item)
                        }
                    }
                    .padding(.horizontal, contentPadding)
                }

                // Divider (tablet)
                if !isCompact {
                    Rectangle()
                        .fill(WornColors.borderSubtle)
                        .frame(height: 1)
                        .padding(.horizontal, contentPadding)
                }

                // Properties
                VStack(spacing: propGap) {
                    PropertyRow(label: String(localized: "label_items"), value: String(format: String(localized: "outfit_detail_items_count"), outfit.itemIds.count), textFont: propFont)
                    PropertyRow(label: String(localized: "label_season"), value: deriveSeasonText(), textFont: propFont)
                }
                .padding(.horizontal, contentPadding)

                // Buttons
                VStack(spacing: 12) {
                    Button { onEdit(outfit) } label: {
                        Text(String(localized: "outfit_detail_edit"))
                            .font(buttonFont.weight(.semibold))
                            .foregroundColor(WornColors.textPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: buttonHeight)
                            .background(WornColors.bgCard)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(WornColors.borderSubtle, lineWidth: 1)
                            )
                    }
                    .accessibilityIdentifier("outfit_detail_edit")

                    Button { showDeleteAlert = true } label: {
                        Text(String(localized: "outfit_detail_delete"))
                            .font(buttonFont.weight(.semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: buttonHeight)
                            .background(WornColors.deleteRed)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                    }
                    .accessibilityIdentifier("outfit_detail_delete")
                }
                .padding(.horizontal, contentPadding)
            }
            .padding(.bottom, 36)
        }
        .background(WornColors.bgElevated)
        .accessibilityIdentifier("outfit_detail_sheet")
        .deleteConfirmationAlert(
            title: String(localized: "outfit_detail_delete_dialog_title"),
            message: String(format: String(localized: "outfit_detail_delete_dialog_message"), outfit.name),
            isPresented: $showDeleteAlert,
            onConfirm: { onDelete(outfit.id) }
        )
    }

    private func outfitItemCard(item: ClothingItem) -> some View {
        VStack(spacing: 6) {
            StoredPhotoImage(path: item.photoPath) { cardPlaceholder }
            .frame(width: cardSize, height: cardSize)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: cardRadius)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 8, x: 0, y: 4)

            Text(item.name)
                .font(.footnote.weight(.medium))
                .foregroundColor(WornColors.textPrimary)
        }
    }

    private var cardPlaceholder: some View {
        Image(systemName: "tshirt")
            .font(.system(size: 32))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }


    private func deriveSeasonText() -> String {
        let allSeasons = Set(outfitItems.flatMap { $0.seasons })
        if allSeasons.isEmpty { return String(localized: "common_not_specified") }
        if allSeasons.count == Season.entries.count { return String(localized: "common_all_seasons") }
        return allSeasons.map { seasonName($0) }.joined(separator: "/")
    }

    private func seasonName(_ season: Season) -> String {
        switch season {
        case .spring: return String(localized: "season_spring")
        case .summer: return String(localized: "season_summer")
        case .fall: return String(localized: "season_fall")
        case .winter: return String(localized: "season_winter")
        default: return ""
        }
    }
}

private let previewItems: [ClothingItem] = [
    ClothingItem(id: "i1", name: "Black T-Shirt", category: .top, colors: ["Black"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i2", name: "Navy Jeans", category: .bottom, colors: ["Navy"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i3", name: "White Sneakers", category: .shoes, colors: ["White"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i4", name: "Olive Jacket", category: .outerwear, colors: ["Olive"], seasons: [], tags: [], description: nil, subcategory: nil, fit: nil, material: nil, photoPath: "", createdAt: 0),
]

private let previewOutfit = Outfit(id: "1", name: "Weekend Casual", itemIds: ["i1", "i2", "i3", "i4"], createdAt: 1_710_460_800_000)

#Preview("iPhone") {
    OutfitDetailSheet(
        outfit: previewOutfit, clothingItems: previewItems,
        isCompact: true, onEdit: { _ in }, onDelete: { _ in }
    )
}

#Preview("iPhone · Dark") {
    OutfitDetailSheet(
        outfit: previewOutfit, clothingItems: previewItems,
        isCompact: true, onEdit: { _ in }, onDelete: { _ in }
    )
    .preferredColorScheme(.dark)
}

#Preview("iPad Portrait", traits: .portrait) {
    OutfitDetailSheet(
        outfit: previewOutfit, clothingItems: previewItems,
        isCompact: false, onEdit: { _ in }, onDelete: { _ in }
    )
}
