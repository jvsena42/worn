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
    private var nameSize: CGFloat { isCompact ? 22 : 26 }
    private var cardSize: CGFloat { isCompact ? 200 : 300 }
    private var cardRadius: CGFloat { isCompact ? 18 : 20 }
    private var cardGap: CGFloat { isCompact ? 12 : 16 }
    private var propFontSize: CGFloat { isCompact ? 14 : 15 }
    private var propGap: CGFloat { isCompact ? 14 : 16 }
    private var buttonHeight: CGFloat { isCompact ? 48 : 52 }
    private var buttonFontSize: CGFloat { isCompact ? 15 : 16 }

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
                    .font(.system(size: nameSize, weight: .semibold))
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
                    propertyRow(label: "Items", value: "\(outfit.itemIds.count) items")
                    propertyRow(label: "Season", value: deriveSeasonText())
                }
                .padding(.horizontal, contentPadding)

                // Buttons
                VStack(spacing: 12) {
                    Button { onEdit(outfit) } label: {
                        Text("Edit Outfit")
                            .font(.system(size: buttonFontSize, weight: .semibold))
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

                    Button { showDeleteAlert = true } label: {
                        Text("Delete Outfit")
                            .font(.system(size: buttonFontSize, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: buttonHeight)
                            .background(WornColors.deleteRed)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                    }
                }
                .padding(.horizontal, contentPadding)
            }
            .padding(.bottom, 36)
        }
        .background(WornColors.bgElevated)
        .alert("Delete outfit?", isPresented: $showDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) { onDelete(outfit.id) }
        } message: {
            Text("This action cannot be undone. \"\(outfit.name)\" will be permanently removed.")
        }
    }

    private func outfitItemCard(item: ClothingItem) -> some View {
        VStack(spacing: 6) {
            ZStack {
                if FileManager.default.fileExists(atPath: item.photoPath) {
                    let url = URL(fileURLWithPath: item.photoPath)
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        cardPlaceholder
                    }
                    .frame(width: cardSize, height: cardSize)
                    .clipped()
                } else {
                    cardPlaceholder
                }
            }
            .frame(width: cardSize, height: cardSize)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: cardRadius)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 8, x: 0, y: 4)

            Text(item.name)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
        }
    }

    private var cardPlaceholder: some View {
        Image(systemName: "tshirt")
            .font(.system(size: 32))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func propertyRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: propFontSize, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: propFontSize, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
        }
    }

    private func deriveSeasonText() -> String {
        let allSeasons = Set(outfitItems.flatMap { $0.seasons })
        if allSeasons.isEmpty { return "Not specified" }
        if allSeasons.count == Season.entries.count { return "All seasons" }
        return allSeasons.map { seasonName($0) }.joined(separator: "/")
    }

    private func seasonName(_ season: Season) -> String {
        switch season {
        case .spring: return "Spring"
        case .summer: return "Summer"
        case .fall: return "Fall"
        case .winter: return "Winter"
        default: return ""
        }
    }
}

private let previewItems: [ClothingItem] = [
    ClothingItem(id: "i1", name: "Black T-Shirt", category: .top, colors: ["Black"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i2", name: "Navy Jeans", category: .bottom, colors: ["Navy"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i3", name: "White Sneakers", category: .shoes, colors: ["White"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
    ClothingItem(id: "i4", name: "Olive Jacket", category: .outerwear, colors: ["Olive"], seasons: [], tags: [], description: nil, photoPath: "", createdAt: 0),
]

private let previewOutfit = Outfit(id: "1", name: "Weekend Casual", itemIds: ["i1", "i2", "i3", "i4"], createdAt: 1_710_460_800_000)

#Preview("iPhone") {
    OutfitDetailSheet(
        outfit: previewOutfit, clothingItems: previewItems,
        isCompact: true, onEdit: { _ in }, onDelete: { _ in }
    )
}

#Preview("iPad") {
    OutfitDetailSheet(
        outfit: previewOutfit, clothingItems: previewItems,
        isCompact: false, onEdit: { _ in }, onDelete: { _ in }
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
