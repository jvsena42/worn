import SwiftUI
import Shared

struct GapsScreen: View {
    @StateObject private var viewModel = GapsViewModelWrapper()
    let onTabSelected: (Tab) -> Void

    @State private var selectedGap: GapRecommendation?
    @State private var showAiLockedSheet = false
    @State private var showAddItemSheet = false
    @State private var addItemPreFill: GapRecommendation?

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(String(localized: "gaps_title"))
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .padding(.top, 24)
                    Text(String(localized: "gaps_subtitle"))
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.textSecondary)
                        .padding(.top, 4)
                        .padding(.bottom, 20)

                    if viewModel.state.isLoading {
                        loadingContent
                    } else if let error = viewModel.state.error {
                        ErrorContentView(
                            message: error as String,
                            onRetry: { viewModel.loadGaps() }
                        )
                        .padding(.vertical, 60)
                    } else if viewModel.state.recommendations.isEmpty {
                        completeContent
                    } else {
                        gapsContent
                    }

                    Spacer().frame(height: 95)
                }
                .padding(.horizontal, 24)
            }
            .background(WornColors.bgPage)
        }
        .accessibilityIdentifier("gaps_screen")
        .sheet(item: $selectedGap) { gap in
            GapDetailSheet(
                recommendation: gap,
                isAiMode: viewModel.state.isAiMode,
                onAddToWardrobe: {
                    addItemPreFill = gap
                    selectedGap = nil
                    showAddItemSheet = true
                },
                onDismiss: { selectedGap = nil }
            )
            .presentationDetents([.large])
        }
        .sheet(isPresented: $showAiLockedSheet) {
            AiLockedSheet(
                onDismiss: { showAiLockedSheet = false },
                onGoToSettings: {
                    showAiLockedSheet = false
                    onTabSelected(.settings)
                }
            )
                .presentationDetents([.medium])
        }
        .sheet(isPresented: $showAddItemSheet) {
            if let gap = addItemPreFill {
                AddItemSheet(
                    isSaving: false,
                    hasApiKey: viewModel.state.hasApiKey,
                    existingItem: gap.toPreFilledItem(),
                    onSave: { _, _, _, _, _, _, _, _ in showAddItemSheet = false },
                    onDismiss: { showAddItemSheet = false }
                )
            }
        }
    }

    // MARK: - Loading

    private var loadingContent: some View {
        HStack {
            Spacer()
            ProgressView()
                .padding(.vertical, 80)
            Spacer()
        }
    }

    // MARK: - Complete

    private var completeContent: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 60)
            ZStack {
                Circle()
                    .fill(WornColors.bgElevated)
                    .frame(width: 72, height: 72)
                Image(systemName: "checkmark")
                    .font(.system(size: 32))
                    .foregroundColor(WornColors.accentGreen)
            }
            .frame(maxWidth: .infinity)

            Text(String(localized: "gaps_complete_title"))
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
                .padding(.top, 24)
                .multilineTextAlignment(.center)

            Text(String(localized: "gaps_complete_description"))
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 8)

            Spacer().frame(height: 60)
        }
    }

    // MARK: - Content

    private var gapsContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            gapsBanner
                .padding(.bottom, 20)

            let grouped = Dictionary(grouping: viewModel.state.recommendations as! [GapRecommendation]) { $0.category }
            let orderedKeys = (viewModel.state.recommendations as! [GapRecommendation]).map { $0.category }
                .reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }

            ForEach(orderedKeys, id: \.self) { category in
                if let items = grouped[category] {
                    sectionLabel(category)
                        .padding(.bottom, 10)

                    ForEach(Array(items.enumerated()), id: \.offset) { _, recommendation in
                        gapCard(recommendation: recommendation)
                            .padding(.bottom, 8)
                    }

                    Spacer().frame(height: 12)
                }
            }
        }
    }

    private var gapsBanner: some View {
        Button {
            if !viewModel.state.isAiMode { showAiLockedSheet = true }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(viewModel.state.isAiMode ? String(localized: "gaps_banner_ai_title") : String(localized: "gaps_banner_common_title"))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                    Text(viewModel.state.isAiMode
                         ? String(localized: "gaps_banner_ai_subtitle")
                         : String(localized: "gaps_banner_common_subtitle"))
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer()
                Image(systemName: "sparkles")
                    .font(.system(size: 20))
                    .foregroundColor(.white.opacity(0.7))
            }
            .padding(16)
            .background(viewModel.state.isAiMode ? WornColors.accentGreen : WornColors.accentGreenDark)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("gaps_banner")
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(WornColors.textSecondary)
            .tracking(0.5)
    }

    private func gapCard(recommendation: GapRecommendation) -> some View {
        Button {
            selectedGap = recommendation
        } label: {
            HStack(spacing: 12) {
                categoryIcon(for: recommendation.mappedCategory)
                VStack(alignment: .leading, spacing: 2) {
                    Text(recommendation.itemName)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(WornColors.textPrimary)
                    Text(viewModel.state.isAiMode
                         ? String(format: String(localized: "gaps_pairing_ai"), recommendation.pairingCount)
                         : String(localized: "gaps_pairing_common"))
                        .font(.system(size: 12))
                        .foregroundColor(WornColors.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.iconMuted)
            }
            .padding(12)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func categoryIcon(for category: Category) -> some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(dotColor(for: category))
            .frame(width: 36, height: 36)
            .overlay(
                Image(systemName: iconName(for: category))
                    .font(.system(size: 16))
                    .foregroundColor(.white)
            )
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

    private func iconName(for category: Category) -> String {
        switch category {
        case .top: return "tshirt"
        case .bottom: return "ruler"
        case .outerwear: return "wind"
        case .shoes: return "shoe"
        case .accessory: return "eyeglasses"
        default: return "tshirt"
        }
    }
}

// MARK: - Detail Sheet

private struct GapDetailSheet: View {
    let recommendation: GapRecommendation
    let isAiMode: Bool
    let onAddToWardrobe: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                detailHeader
                pairingInfo
                    .padding(.top, 16)
                detailRows
                    .padding(.top, 16)
                detailActions
                    .padding(.top, 24)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .background(WornColors.bgElevated)
    }

    private var detailHeader: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(WornColors.bgCard)
                    .frame(height: 140)
                Image(systemName: iconName(for: recommendation.mappedCategory))
                    .font(.system(size: 40))
                    .foregroundColor(WornColors.iconMuted)
            }
            .frame(maxWidth: .infinity)

            Text(recommendation.itemName)
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
                .padding(.top, 16)

            HStack(spacing: 6) {
                Circle()
                    .fill(dotColor(for: recommendation.mappedCategory))
                    .frame(width: 8, height: 8)
                Text(displayLabel(for: recommendation.mappedCategory))
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.textSecondary)
            }
            .padding(.top, 4)
        }
    }

    private var pairingInfo: some View {
        HStack(spacing: 8) {
            Image(systemName: "sparkles")
                .font(.system(size: 14))
                .foregroundColor(WornColors.accentGreen)
            Text(isAiMode
                 ? String(format: String(localized: "gaps_pairing_ai"), recommendation.pairingCount)
                 : String(localized: "gaps_pairing_common"))
                .font(.system(size: 13))
                .foregroundColor(WornColors.textSecondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var detailRows: some View {
        VStack(spacing: 0) {
            if let sub = recommendation.subcategory {
                detailRow(String(localized: "label_subcategory"), sub.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized)
            }
            if !recommendation.colors.isEmpty {
                detailRow(String(localized: "label_color"), recommendation.colors.joined(separator: ", "))
            }
            if !recommendation.seasons.isEmpty {
                let text = recommendation.seasons.count == 4
                    ? String(localized: "common_all_seasons")
                    : recommendation.seasons.map { $0.name.lowercased().capitalized }.joined(separator: ", ")
                detailRow(String(localized: "label_season"), text)
            }
            if let fit = recommendation.fit {
                detailRow(String(localized: "label_fit"), fit.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized)
            }
            if let material = recommendation.material {
                detailRow(String(localized: "label_material"), material.name.lowercased().capitalized)
            }
        }
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
        }
        .padding(.vertical, 8)
    }

    private var detailActions: some View {
        VStack(spacing: 8) {
            Button(action: onAddToWardrobe) {
                Text(String(localized: "gaps_add_to_wardrobe"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        LinearGradient(
                            colors: [WornColors.saveGradientStart, WornColors.saveGradientEnd],
                            startPoint: .top, endPoint: .bottom
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .accessibilityIdentifier("gap_add_to_wardrobe")

            Button(action: onDismiss) {
                Text(String(localized: "gaps_dismiss"))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(WornColors.textSecondary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(WornColors.bgCard)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .accessibilityIdentifier("gap_dismiss")
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

    private func iconName(for category: Category) -> String {
        switch category {
        case .top: return "tshirt"
        case .bottom: return "ruler"
        case .outerwear: return "wind"
        case .shoes: return "shoe"
        case .accessory: return "eyeglasses"
        default: return "tshirt"
        }
    }

    private func displayLabel(for category: Category) -> String {
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

// MARK: - Helpers

extension GapRecommendation: @retroactive Identifiable {
    public var id: String { itemName + category }
}

extension GapRecommendation {
    func toPreFilledItem() -> ClothingItem {
        return ClothingItem(
            id: "",
            name: itemName,
            category: mappedCategory,
            colors: colors,
            seasons: seasons,
            tags: [],
            description: nil,
            subcategory: subcategory,
            fit: fit,
            material: material,
            photoPath: "",
            createdAt: 0
        )
    }
}

#Preview("iPhone") {
    GapsScreen(onTabSelected: { _ in })
}

#Preview("iPad") {
    GapsScreen(onTabSelected: { _ in })
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
