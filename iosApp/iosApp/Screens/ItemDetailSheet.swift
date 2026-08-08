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
    private var nameFont: Font { isCompact ? .title2 : .title }
    private var propFont: Font { isCompact ? .subheadline : .callout }
    private var propGap: CGFloat { isCompact ? 14 : 16 }
    private var buttonHeight: CGFloat { isCompact ? 48 : 52 }
    private var buttonFont: Font { isCompact ? .subheadline : .callout }
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
        .accessibilityIdentifier("item_detail_sheet")
        .deleteConfirmationAlert(
            title: String(localized: "item_detail_delete_dialog_title"),
            message: String(format: String(localized: "item_detail_delete_dialog_message"), item.name),
            isPresented: $showDeleteAlert,
            onConfirm: { onDelete(item.id) }
        )
    }

    private var photoArea: some View {
        StoredPhotoImage(path: item.photoPath) { placeholderIcon }
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
                .font(nameFont.weight(.semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                Circle()
                    .fill(dotColor(for: item.category))
                    .frame(width: 10, height: 10)
                Text(displayLabel(for: item.category))
                    .font(.subheadline)
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
                    Text(String(localized: "label_color"))
                        .font(propFont.weight(.medium))
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
                            .font(propFont.weight(.medium))
                            .foregroundColor(WornColors.textPrimary)
                    }
                }
            }

            if !item.seasons.isEmpty {
                let seasonText = item.seasons.count == Season.entries.count
                    ? String(localized: "common_all_seasons")
                    : item.seasons.map { seasonDisplayName($0) }.joined(separator: ", ")
                PropertyRow(label: String(localized: "label_season"), value: seasonText, textFont: propFont)
            }

            if let fit = item.fit {
                PropertyRow(label: String(localized: "label_fit"), value: fitDisplayName(fit), textFont: propFont)
            }

            if let subcategory = item.subcategory {
                PropertyRow(label: String(localized: "label_subcategory"), value: subcategoryDisplayName(subcategory), textFont: propFont)
            }

            if let material = item.material {
                PropertyRow(label: String(localized: "label_material"), value: materialDisplayName(material), textFont: propFont)
            }
        }
    }


    private var buttons: some View {
        VStack(spacing: 12) {
            // Edit is the primary action, so it takes the filled button. Delete was previously the
            // filled one — a full-width solid red block ranked *below* a plain white Edit — which
            // gave the destructive action more visual weight than the thing people came to do.
            Button { onEdit(item) } label: {
                Text(String(localized: "item_detail_edit"))
                    .font(buttonFont.weight(.semibold))
                    .foregroundColor(WornColors.textOnColor)
                    .frame(maxWidth: .infinity)
                    .frame(height: buttonHeight)
                    .background(WornColors.accentGreen)
                    .clipShape(RoundedRectangle(cornerRadius: WornShape.extraLarge))
            }
            .accessibilityIdentifier("item_detail_edit")

            // `role: .destructive` is what makes VoiceOver announce this as destructive and lets
            // the system style it; the outline keeps it unmistakable without shouting. Tinting the
            // label rather than filling also survives dark, where deleteRed is a light #F2B8AC and
            // white on it is unreadable.
            Button(role: .destructive) { showDeleteAlert = true } label: {
                Text(String(localized: "item_detail_delete"))
                    .font(buttonFont.weight(.semibold))
                    .foregroundColor(WornColors.deleteRed)
                    .frame(maxWidth: .infinity)
                    .frame(height: buttonHeight)
                    .overlay(
                        RoundedRectangle(cornerRadius: WornShape.extraLarge)
                            .stroke(WornColors.deleteRed, lineWidth: 1)
                    )
            }
            .accessibilityIdentifier("item_detail_delete")
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

    private func seasonDisplayName(_ season: Season) -> String {
        switch season {
        case .spring: return String(localized: "season_spring")
        case .summer: return String(localized: "season_summer")
        case .fall: return String(localized: "season_fall")
        case .winter: return String(localized: "season_winter")
        default: return ""
        }
    }

    private func fitDisplayName(_ fit: Fit) -> String {
        switch fit {
        case .slimFit: return String(localized: "fit_slim")
        case .regular: return String(localized: "fit_regular")
        case .relaxed: return String(localized: "fit_relaxed")
        case .oversized: return String(localized: "fit_oversized")
        default: return ""
        }
    }

    private func subcategoryDisplayName(_ sub: Subcategory) -> String {
        let key = "subcategory_\(sub.name.lowercased())"
        return String(localized: String.LocalizationValue(key))
    }

    private func materialDisplayName(_ material: Shared.Material) -> String {
        let key = "material_\(material.name.lowercased())"
        return String(localized: String.LocalizationValue(key))
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

#Preview("iPhone · Dark") {
    ItemDetailSheet(
        item: previewItem, isCompact: true,
        onEdit: { _ in }, onDelete: { _ in }
    )
    .preferredColorScheme(.dark)
}

#Preview("iPad Portrait", traits: .portrait) {
    ItemDetailSheet(
        item: previewItem, isCompact: false,
        onEdit: { _ in }, onDelete: { _ in }
    )
}
