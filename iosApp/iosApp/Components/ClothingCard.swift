import SwiftUI
import Shared

struct ClothingCard: View {
    let item: ClothingItem
    var photoHeight: CGFloat = 171

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            photoArea
            itemInfo
        }
    }

    private var photoArea: some View {
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
        case .top: return Color(hex: "444444")
        case .bottom: return Color(hex: "2B4570")
        case .dress: return Color(hex: "A87560")
        case .outerwear: return Color(hex: "7A9468")
        case .shoes: return Color(hex: "8B6914")
        case .accessory: return Color(hex: "B59D6E")
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
