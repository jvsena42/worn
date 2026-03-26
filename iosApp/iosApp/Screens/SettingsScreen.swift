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
                    Text("Settings")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .padding(.top, 24)
                        .padding(.bottom, 28)

                    sectionLabel("YOUR PROFILE")
                    settingsCard(
                        iconColor: WornColors.accentGreen,
                        iconName: "person.fill",
                        title: "Your Profile",
                        subtitle: profileSummary,
                        action: { showProfileSheet = true }
                    )
                    .padding(.top, 10)

                    sectionLabel("AI FEATURES")
                        .padding(.top, 24)
                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "sparkles",
                        title: "Claude API Key",
                        subtitle: viewModel.state.hasApiKey ? "Connected" : "Required for AI features",
                        action: { showApiKeySheet = true }
                    )
                    .padding(.top, 10)

                    sectionLabel("ABOUT")
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
        return parts.isEmpty ? "Tap to set up" : parts.joined(separator: " · ")
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

    private var aboutCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Version")
                    .font(.system(size: 15))
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                Text("1.0.0")
                    .font(.system(size: 15))
                    .foregroundColor(WornColors.textSecondary)
            }
            .padding(16)

            Divider().overlay(WornColors.borderSubtle.opacity(0.5))

            Button {} label: {
                HStack {
                    Text("Licenses")
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

// MARK: - Profile Sheet

private struct ProfileSheet: View {
    @ObservedObject var viewModel: SettingsViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Your Profile")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                    Text("Help AI give better suggestions")
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.textSecondary)

                    chipGroup(title: "Body Type", options: bodyTypeOptions,
                              selected: viewModel.state.userProfile.bodyType as? BodyType) {
                        viewModel.selectBodyType($0)
                    }
                    chipGroup(title: "Style Profile", options: styleOptions,
                              selected: viewModel.state.userProfile.styleProfile as? StyleProfile) {
                        viewModel.selectStyleProfile($0)
                    }
                    chipGroup(title: "Age Range", options: ageOptions,
                              selected: viewModel.state.userProfile.ageRange as? AgeRange) {
                        viewModel.selectAgeRange($0)
                    }
                    chipGroup(title: "Climate / Region", options: climateOptions,
                              selected: viewModel.state.userProfile.climate as? Climate) {
                        viewModel.selectClimate($0)
                    }
                    multiChipGroup(title: "Lifestyle / Occasions", options: lifestyleOptions,
                                   selected: Set((viewModel.state.userProfile.lifestyles as? Set<Lifestyle>) ?? [])) {
                        viewModel.toggleLifestyle($0)
                    }

                    saveGradientButton(text: "Save") { dismiss() }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
        }
    }

    private var bodyTypeOptions: [(BodyType, String)] {
        [(.slim, "Slim"), (.athletic, "Athletic"), (.average, "Average"),
         (.stocky, "Stocky"), (.short_, "Short"), (.tallAndSlim, "Tall & Slim"),
         (.bigAndTall, "Big & Tall")]
    }
    private var styleOptions: [(StyleProfile, String)] {
        [(.classic, "Classic"), (.casual, "Casual"), (.streetwear, "Streetwear"),
         (.smartCasual, "Smart Casual"), (.minimalist, "Minimalist")]
    }
    private var ageOptions: [(AgeRange, String)] {
        [(.age1825, "18-25"), (.age2635, "26-35"), (.age3645, "36-45"), (.age46Plus, "46+")]
    }
    private var climateOptions: [(Climate, String)] {
        [(.tropical, "Tropical"), (.temperate, "Temperate"), (.cold, "Cold"), (.mixed, "Mixed")]
    }
    private var lifestyleOptions: [(Lifestyle, String)] {
        [(.workOffice, "Work (Office)"), (.workManual, "Work (Manual)"),
         (.social, "Social"), (.sports, "Sports"), (.formalEvents, "Formal Events")]
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
            Text("Connect Claude AI")
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
            Text("Paste your Anthropic API key to unlock AI-powered features like auto-tagging clothes and outfit analysis.")
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
            Text("Get a free key at console.anthropic.com →")
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

            saveGradientButton(text: "Save & Connect", enabled: !hasApiKey && !keyInput.isEmpty) {
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
                        Text("Remove key")
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
            Text("(multi-select)")
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
        case .slim: return "Slim"
        case .athletic: return "Athletic"
        case .average: return "Average"
        case .stocky: return "Stocky"
        case .short_: return "Short"
        case .tallAndSlim: return "Tall & Slim"
        case .bigAndTall: return "Big & Tall"
        default: return ""
        }
    }
}

private extension StyleProfile {
    var displayName: String {
        switch self {
        case .classic: return "Classic"
        case .casual: return "Casual"
        case .streetwear: return "Streetwear"
        case .smartCasual: return "Smart Casual"
        case .minimalist: return "Minimalist"
        default: return ""
        }
    }
}

private extension AgeRange {
    var displayName: String {
        switch self {
        case .age1825: return "18-25"
        case .age2635: return "26-35"
        case .age3645: return "36-45"
        case .age46Plus: return "46+"
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
