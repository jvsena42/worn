import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModelWrapper()
    let onTabSelected: (Tab) -> Void

    @State private var showProfileSheet = false
    @State private var showApiKeySheet = false

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(String(localized: "settings_title"))
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .padding(.top, 24)
                        .padding(.bottom, 28)

                    sectionLabel(String(localized: "settings_section_profile"))
                    settingsCard(
                        iconColor: WornColors.accentGreen,
                        iconName: "person.fill",
                        title: String(localized: "settings_your_profile"),
                        subtitle: profileSummary,
                        action: { showProfileSheet = true }
                    )
                    .padding(.top, 10)

                    sectionLabel(String(localized: "settings_section_ai"))
                        .padding(.top, 24)
                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "sparkles",
                        title: String(localized: "settings_api_key_title"),
                        subtitle: viewModel.state.hasApiKey ? String(localized: "settings_api_key_connected") : String(localized: "settings_api_key_required"),
                        action: { showApiKeySheet = true }
                    )
                    .padding(.top, 10)

                    sectionLabel(String(localized: "settings_section_about"))
                        .padding(.top, 24)
                    aboutCard
                        .padding(.top, 10)

                    Spacer().frame(height: 32)
                }
                .padding(.horizontal, 24)
            }
            .background(WornColors.bgPage)

            WornBottomBar(activeTab: .settings, onTabSelected: onTabSelected)
        }
        .sheet(isPresented: $showProfileSheet) {
            ProfileSheet(viewModel: viewModel)
                .presentationDetents([.large])
        }
        .sheet(isPresented: $showApiKeySheet) {
            ApiKeySheet(
                hasApiKey: viewModel.state.hasApiKey,
                onSave: { viewModel.saveApiKey($0) },
                onClear: { viewModel.clearApiKey() }
            )
            .presentationDetents([.medium])
        }
    }

    private var profileSummary: String {
        let profile = viewModel.state.userProfile
        let parts: [String] = [
            (profile.bodyType as? BodyType)?.displayName,
            (profile.styleProfile as? StyleProfile)?.displayName,
            (profile.ageRange as? AgeRange)?.displayName,
        ].compactMap { $0 }
        return parts.isEmpty ? String(localized: "settings_profile_subtitle_empty") : parts.joined(separator: " · ")
    }

    // MARK: - Components

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(WornColors.textSecondary)
            .tracking(0.5)
    }

    private func settingsCard(iconColor: Color, iconName: String, title: String, subtitle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(iconColor)
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: iconName)
                            .font(.system(size: 18))
                            .foregroundColor(.white)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(WornColors.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(WornColors.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.iconMuted)
            }
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var aboutCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text(String(localized: "settings_version"))
                    .font(.system(size: 15))
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                Text(appVersion)
                    .font(.system(size: 15))
                    .foregroundColor(WornColors.textSecondary)
            }
            .padding(16)

            Divider().overlay(WornColors.borderSubtle.opacity(0.5))

            Button {
                if let url = URL(string: licenseURL) {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack {
                    Text(String(localized: "settings_licenses"))
                        .font(.system(size: 15))
                        .foregroundColor(WornColors.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.iconMuted)
                }
                .padding(16)
            }
            .buttonStyle(.plain)
        }
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private let licenseURL = "https://github.com/jvsena42/worn/blob/main/LICENSE"

// MARK: - Profile Sheet

private struct ProfileSheet: View {
    @ObservedObject var viewModel: SettingsViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text(String(localized: "settings_your_profile"))
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                    Text(String(localized: "settings_profile_help"))
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.textSecondary)

                    chipGroup(title: String(localized: "label_body_type"), options: bodyTypeOptions,
                              selected: viewModel.state.userProfile.bodyType as? BodyType) {
                        viewModel.selectBodyType($0)
                    }
                    chipGroup(title: String(localized: "label_style_profile"), options: styleOptions,
                              selected: viewModel.state.userProfile.styleProfile as? StyleProfile) {
                        viewModel.selectStyleProfile($0)
                    }
                    chipGroup(title: String(localized: "label_age_range"), options: ageOptions,
                              selected: viewModel.state.userProfile.ageRange as? AgeRange) {
                        viewModel.selectAgeRange($0)
                    }
                    chipGroup(title: String(localized: "label_climate"), options: climateOptions,
                              selected: viewModel.state.userProfile.climate as? Climate) {
                        viewModel.selectClimate($0)
                    }
                    multiChipGroup(title: String(localized: "label_lifestyle"), options: lifestyleOptions,
                                   selected: Set((viewModel.state.userProfile.lifestyles as? Set<Lifestyle>) ?? [])) {
                        viewModel.toggleLifestyle($0)
                    }

                    saveGradientButton(text: String(localized: "common_save")) { dismiss() }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
        }
    }

    private var bodyTypeOptions: [(BodyType, String)] {
        [(.slim, String(localized: "body_type_slim")), (.athletic, String(localized: "body_type_athletic")), (.average, String(localized: "body_type_average")),
         (.stocky, String(localized: "body_type_stocky")), (.short_, String(localized: "body_type_short")), (.tallAndSlim, String(localized: "body_type_tall_slim")),
         (.bigAndTall, String(localized: "body_type_big_tall"))]
    }
    private var styleOptions: [(StyleProfile, String)] {
        [(.classic, String(localized: "style_classic")), (.casual, String(localized: "style_casual")), (.streetwear, String(localized: "style_streetwear")),
         (.smartCasual, String(localized: "style_smart_casual")), (.minimalist, String(localized: "style_minimalist"))]
    }
    private var ageOptions: [(AgeRange, String)] {
        [(.age1825, String(localized: "age_18_25")), (.age2635, String(localized: "age_26_35")), (.age3645, String(localized: "age_36_45")), (.age46Plus, String(localized: "age_46_plus"))]
    }
    private var climateOptions: [(Climate, String)] {
        [(.tropical, String(localized: "climate_tropical")), (.temperate, String(localized: "climate_temperate")), (.cold, String(localized: "climate_cold")), (.mixed, String(localized: "climate_mixed"))]
    }
    private var lifestyleOptions: [(Lifestyle, String)] {
        [(.workOffice, String(localized: "lifestyle_work_office")), (.workManual, String(localized: "lifestyle_work_manual")),
         (.social, String(localized: "lifestyle_social")), (.sports, String(localized: "lifestyle_sports")), (.formalEvents, String(localized: "lifestyle_formal_events"))]
    }
}

// MARK: - API Key Sheet

private struct ApiKeySheet: View {
    let hasApiKey: Bool
    let onSave: (String) -> Void
    let onClear: () -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var keyInput = ""
    @State private var passwordVisible = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String(localized: "settings_connect_claude"))
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
            Text(String(localized: "settings_api_description"))
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
            Text(String(localized: "settings_api_get_key"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(WornColors.accentGreen)

            HStack {
                Group {
                    if passwordVisible {
                        TextField("", text: hasApiKey ? .constant("sk-ant-••••••••") : $keyInput)
                    } else {
                        SecureField("", text: hasApiKey ? .constant("sk-ant-••••••••") : $keyInput)
                    }
                }
                .disabled(hasApiKey)
                .font(.system(size: 15))

                Button { passwordVisible.toggle() } label: {
                    Image(systemName: passwordVisible ? "eye" : "eye.slash")
                        .foregroundColor(WornColors.iconMuted)
                }
            }
            .padding(14)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )

            saveGradientButton(text: String(localized: "settings_save_connect"), enabled: !hasApiKey && !keyInput.isEmpty) {
                onSave(keyInput)
                keyInput = ""
                dismiss()
            }

            if hasApiKey {
                HStack {
                    Spacer()
                    Button {
                        onClear()
                        dismiss()
                    } label: {
                        Text(String(localized: "settings_remove_key"))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(WornColors.textSecondary)
                    }
                    Spacer()
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 24)
    }
}

// MARK: - Shared chip components

private func chipGroup<T: Equatable>(
    title: String, options: [(T, String)], selected: T?, onSelected: @escaping (T?) -> Void
) -> some View {
    VStack(alignment: .leading, spacing: 10) {
        Text(title)
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(WornColors.textPrimary)
        FlowLayout(spacing: 8) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, item in
                let (value, label) = item
                let isActive = value == selected
                Button { onSelected(isActive ? nil : value) } label: {
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private func multiChipGroup<T: Hashable>(
    title: String, options: [(T, String)], selected: Set<T>, onToggle: @escaping (T) -> Void
) -> some View {
    VStack(alignment: .leading, spacing: 10) {
        HStack(spacing: 6) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
            Text(String(localized: "settings_multi_select"))
                .font(.system(size: 12))
                .foregroundColor(WornColors.textMuted)
        }
        FlowLayout(spacing: 8) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, item in
                let (value, label) = item
                let isActive = selected.contains(value)
                Button { onToggle(value) } label: {
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private func saveGradientButton(text: String, enabled: Bool = true, action: @escaping () -> Void) -> some View {
    Button(action: action) {
        Text(text)
            .font(.system(size: 16, weight: .semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                LinearGradient(
                    colors: enabled
                        ? [WornColors.saveGradientStart, WornColors.saveGradientEnd]
                        : [WornColors.textMuted, WornColors.iconMuted],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
    .disabled(!enabled)
}

// MARK: - Display name helpers

private extension BodyType {
    var displayName: String {
        switch self {
        case .slim: return String(localized: "body_type_slim")
        case .athletic: return String(localized: "body_type_athletic")
        case .average: return String(localized: "body_type_average")
        case .stocky: return String(localized: "body_type_stocky")
        case .short_: return String(localized: "body_type_short")
        case .tallAndSlim: return String(localized: "body_type_tall_slim")
        case .bigAndTall: return String(localized: "body_type_big_tall")
        default: return ""
        }
    }
}

private extension StyleProfile {
    var displayName: String {
        switch self {
        case .classic: return String(localized: "style_classic")
        case .casual: return String(localized: "style_casual")
        case .streetwear: return String(localized: "style_streetwear")
        case .smartCasual: return String(localized: "style_smart_casual")
        case .minimalist: return String(localized: "style_minimalist")
        default: return ""
        }
    }
}

private extension AgeRange {
    var displayName: String {
        switch self {
        case .age1825: return String(localized: "age_18_25")
        case .age2635: return String(localized: "age_26_35")
        case .age3645: return String(localized: "age_36_45")
        case .age46Plus: return String(localized: "age_46_plus")
        default: return ""
        }
    }
}

// MARK: - FlowLayout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(proposal: proposal, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x,
                                      y: bounds.minY + result.positions[index].y),
                          proposal: .unspecified)
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0; var y: CGFloat = 0; var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 { x = 0; y += rowHeight + spacing; rowHeight = 0 }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        return (CGSize(width: maxWidth, height: y + rowHeight), positions)
    }
}

#Preview("iPhone") {
    SettingsScreen(onTabSelected: { _ in })
}

#Preview("iPad") {
    SettingsScreen(onTabSelected: { _ in })
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
