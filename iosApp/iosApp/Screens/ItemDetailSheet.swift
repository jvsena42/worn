import SwiftUI
import Shared

struct ItemDetailSheet: View {
    let item: ClothingItem
    var isCompact: Bool = true
    var showActions: Bool = true
    let onEdit: (ClothingItem) -> Void
    let onDelete: (String) -> Void

    @State private var showDeleteAlert = false

    private var photoHeight: CGFloat { isCompact ? 280 : 360 }
    private var photoRadius: CGFloat { isCompact ? 20 : 24 }
    private var nameSize: CGFloat { isCompact ? 22 : 26 }
    private var propFontSize: CGFloat { isCompact ? 14 : 15 }
    private var propGap: CGFloat { isCompact ? 14 : 16 }
    private var buttonHeight: CGFloat { isCompact ? 48 : 52 }
    private var buttonFontSize: CGFloat { isCompact ? 15 : 16 }
    private var contentPadding: CGFloat { isCompact ? 24 : 32 }
    private var sectionGap: CGFloat { isCompact ? 20 : 24 }
    private var placeholderIconSize: CGFloat { isCompact ? 64 : 80 }

    var body: some View {
        ScrollView {
            VStack(spacing: sectionGap) {
                photoArea
                nameGroup
                divider
                properties
                if showActions {
                    buttons
                }
            }
            .padding(.horizontal, contentPadding)
            .padding(.bottom, 36)
        }
        .background(WornColors.bgElevated)
        .alert("Delete item?", isPresented: $showDeleteAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) { onDelete(item.id) }
        } message: {
            Text("This action cannot be undone. \"\(item.name)\" will be permanently removed from your wardrobe.")
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
        .clipShape(RoundedRectangle(cornerRadius: photoRadius))
        .overlay(
            RoundedRectangle(cornerRadius: photoRadius)
                .stroke(WornColors.borderSubtle, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.25), radius: 8, x: 0, y: 4)
    }

    private var placeholderIcon: some View {
        Image(systemName: "tshirt")
            .font(.system(size: placeholderIconSize))
            .foregroundColor(WornColors.iconMuted)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var nameGroup: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(item.name)
                .font(.system(size: nameSize, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                Circle()
                    .fill(dotColor(for: item.category))
                    .frame(width: 10, height: 10)
                Text(displayLabel(for: item.category))
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var divider: some View {
        Rectangle()
            .fill(WornColors.borderSubtle)
            .frame(height: 1)
    }

    private var properties: some View {
        VStack(spacing: propGap) {
            if !item.colors.isEmpty {
                HStack {
                    Text("Color")
                        .font(.system(size: propFontSize, weight: .medium))
                        .foregroundColor(WornColors.textSecondary)
                    Spacer()
                    HStack(spacing: 8) {
                        Circle()
                            .fill(colorForName(item.colors.first ?? ""))
                            .frame(width: 14, height: 14)
                            .overlay(
                                Circle().stroke(WornColors.borderSubtle, lineWidth: 1)
                            )
                        Text(item.colors.map { $0.capitalized }.joined(separator: ", "))
                            .font(.system(size: propFontSize, weight: .medium))
                            .foregroundColor(WornColors.textPrimary)
                    }
                }
            }

            if !item.seasons.isEmpty {
                let seasonText = item.seasons.count == Season.entries.count
                    ? "All seasons"
                    : item.seasons.map { seasonDisplayName($0) }.joined(separator: ", ")
                propertyRow(label: "Season", value: seasonText)
            }

            if let fit = item.fit {
                propertyRow(label: "Fit", value: fitDisplayName(fit))
            }

            if let subcategory = item.subcategory {
                propertyRow(label: "Subcategory", value: subcategoryDisplayName(subcategory))
            }

            if let material = item.material {
                propertyRow(label: "Material", value: materialDisplayName(material))
            }
        }
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

    private var buttons: some View {
        VStack(spacing: 12) {
            Button { onEdit(item) } label: {
                Text("Edit Item")
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
                Text("Delete Item")
                    .font(.system(size: buttonFontSize, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: buttonHeight)
                    .background(WornColors.deleteRed)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
            }
        }
    }

    private func dotColor(for category: Category) -> Color {
        switch category {
        case .top: return WornColors.categoryDotTop
        case .bottom: return WornColors.categoryDotBottom
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
        case .outerwear: return "Outerwear"
        case .shoes: return "Shoes"
        case .accessory: return "Accessories"
        default: return ""
        }
    }

    private func seasonDisplayName(_ season: Season) -> String {
        switch season {
        case .spring: return "Spring"
        case .summer: return "Summer"
        case .fall: return "Fall"
        case .winter: return "Winter"
        default: return ""
        }
    }

    private func fitDisplayName(_ fit: Fit) -> String {
        switch fit {
        case .slimFit: return "Slim Fit"
        case .regular: return "Regular"
        case .relaxed: return "Relaxed"
        case .oversized: return "Oversized"
        default: return ""
        }
    }

    private func subcategoryDisplayName(_ sub: Subcategory) -> String {
        sub.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func materialDisplayName(_ material: Material) -> String {
        material.name.lowercased().capitalized
    }

    private let colorPalette: [(name: String, color: Color)] = [
        ("White", Color(hex: "FFFFFF")), ("Cream", Color(hex: "EDE8E1")),
        ("Black", Color(hex: "2C2924")), ("Navy", Color(hex: "2B4570")),
        ("Grey", Color(hex: "808080")), ("Charcoal", Color(hex: "36454F")),
        ("Olive", Color(hex: "6B7B3F")), ("Beige", Color(hex: "C4A882")),
        ("Khaki", Color(hex: "C3B091")), ("Tan", Color(hex: "D2B48C")),
        ("Brown", Color(hex: "8B4513")), ("Burgundy", Color(hex: "800020")),
        ("Coral", Color(hex: "A87560")), ("Light Blue", Color(hex: "ADD8E6")),
    ]

    private func colorForName(_ name: String) -> Color {
        colorPalette.first { $0.name.caseInsensitiveCompare(name) == .orderedSame }?.color ?? Color(hex: "444444")
    }
}

private let previewItem = ClothingItem(
    id: "1", name: "Black T-Shirt", category: .top,
    colors: ["Black"], seasons: [.spring, .summer, .fall, .winter],
    tags: [], description: nil,
    subcategory: .tShirt, fit: .regular, material: .cotton,
    photoPath: "", createdAt: 0
)

#Preview("iPhone") {
    ItemDetailSheet(
        item: previewItem, isCompact: true,
        onEdit: { _ in }, onDelete: { _ in }
    )
}

#Preview("iPad") {
    ItemDetailSheet(
        item: previewItem, isCompact: false,
        onEdit: { _ in }, onDelete: { _ in }
    )
    .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
