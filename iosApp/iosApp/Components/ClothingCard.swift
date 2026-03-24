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
                selectionIndicator
                    .padding(8)
            }
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

    private func dotColor(for category: Category) -> Color {
        switch category {
        case .top: return WornColors.categoryDotTop
        case .bottom: return WornColors.categoryDotBottom
        case .dress: return WornColors.categoryDotDress
        case .outerwear: return WornColors.categoryDotOuterwear
        case .shoes: return WornColors.categoryDotShoes
        case .accessory: return WornColors.categoryDotAccessory
        default: return Color.gray
        }
    }

    private func displayLabel(for category: Category) -> String {
        switch category {
        case .top: return "Tops"
        case .bottom: return "Bottoms"
        case .dress: return "Dresses"
        case .outerwear: return "Outerwear"
        case .shoes: return "Shoes"
        case .accessory: return "Accessories"
        default: return ""
        }
    }
}
