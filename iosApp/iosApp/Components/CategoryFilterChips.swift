import SwiftUI
import Shared

struct CategoryFilterChips: View {
    let activeCategory: Category?
    let onCategorySelected: (Category?) -> Void

    private let allChips: [(category: Category?, label: String)] = [
        (nil, "All"),
        (.top, "Tops"),
        (.bottom, "Bottoms"),
        (.dress, "Dresses"),
        (.outerwear, "Outerwear"),
        (.shoes, "Shoes"),
        (.accessory, "Accessories"),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(allChips, id: \.label) { chip in
                    CategoryChip(
                        label: chip.label,
                        isActive: chip.category == activeCategory,
                        onTap: { onCategorySelected(chip.category) }
                    )
                }
            }
        }
    }
}

private struct CategoryChip: View {
    let label: String
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
