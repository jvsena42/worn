import SwiftUI
import Shared

struct ClothingCard: View {
    let item: ClothingItem
    var photoHeight: CGFloat = 171
    var isSelected: Bool = false
    var isSelectionMode: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            photoArea
            itemInfo
        }
        .accessibilityIdentifier("clothing_card")
    }

    private var photoArea: some View {
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
                    .frame(maxWidth: .infinity, maxHeight: photoHeight)
                    .clipped()
                } else {
                    placeholderIcon
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: photoHeight)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 8, x: 0, y: 4)

            if isSelectionMode {
                SelectionIndicator(isSelected: isSelected)
                    .padding(8)
            }
        }
    }


    private var placeholderIcon: some View {
        Image(systemName: "tshirt")
            .font(.system(size: 32))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var itemInfo: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(item.name)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 6) {
                Circle()
                    .fill(dotColor(for: item.category))
                    .frame(width: 8, height: 8)
                Text(displayLabel(for: item.category))
                    .font(.system(size: 12))
                    .foregroundColor(WornColors.textMuted)
            }
        }
    }

    private func dotColor(for category: Shared.Category) -> Color {
        switch category {
        case .top: return WornColors.categoryDotTop
        case .bottom: return WornColors.categoryDotBottom
        case .outerwear: return WornColors.categoryDotOuterwear
        case .shoes: return WornColors.categoryDotShoes
        case .accessory: return WornColors.categoryDotAccessory
        default: return Color.gray
        }
    }

    private func displayLabel(for category: Shared.Category) -> String {
        switch category {
        case .top: return String(localized: "category_tops")
        case .bottom: return String(localized: "category_bottoms")
        case .outerwear: return String(localized: "category_outerwear")
        case .shoes: return String(localized: "category_shoes")
        case .accessory: return String(localized: "category_accessories")
        default: return ""
        }
    }
}

private let previewCardItem = ClothingItem(
    id: "1", name: "Black T-Shirt", category: .top, colors: ["black"], seasons: [],
    tags: [], description: nil, subcategory: .tShirt, fit: .regular, material: .cotton,
    photoPath: "", createdAt: 0
)

#Preview("iPhone") {
    HStack(spacing: 12) {
        ClothingCard(item: previewCardItem)
        ClothingCard(item: previewCardItem, isSelected: true, isSelectionMode: true)
    }
    .padding(24)
    .background(WornColors.bgPage)
}

#Preview("iPad Portrait", traits: .portrait) {
    HStack(spacing: 16) {
        ClothingCard(item: previewCardItem, photoHeight: 200)
        ClothingCard(item: previewCardItem, photoHeight: 200, isSelected: true, isSelectionMode: true)
    }
    .padding(32)
    .background(WornColors.bgPage)
}
