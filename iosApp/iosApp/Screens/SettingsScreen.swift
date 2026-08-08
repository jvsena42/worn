import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModelWrapper()
    @Environment(\.horizontalSizeClass) private var sizeClass
    let onTabSelected: (WornTab) -> Void

    @State private var showProfileSheet = false
    @State private var showApiKeySheet = false
    @State private var showYouCamSheet = false

    var body: some View {
        SettingsContent(
            state: viewModel.state,
            isCompact: sizeClass == .compact,
            onProfileClick: { showProfileSheet = true },
            onApiKeyClick: { showApiKeySheet = true },
            onYouCamClick: { showYouCamSheet = true },
            onSetOnDeviceAi: { viewModel.setOnDeviceAi($0) }
        )
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
        .sheet(isPresented: $showYouCamSheet) {
            YouCamCredentialsSheet(
                hasCredentials: viewModel.state.hasYouCamKey,
                verifying: viewModel.state.verifyingYouCam,
                errorMessage: viewModel.state.youCamError,
                onSave: { viewModel.saveYouCamCredentials(clientId: $0, clientSecret: $1) },
                onClear: { viewModel.clearYouCamCredentials() }
            )
            .presentationDetents([.medium, .large])
        }
        .onChange(of: viewModel.state.hasYouCamKey) { _, has in
            if has { showYouCamSheet = false }
        }
    }
}

/// Pure, state-driven half of the Settings screen.
///
/// Split out for the same reason as `WardrobeContent` and `OutfitsContent`: the stateful wrapper
/// resolves its ViewModel from Koin, so a preview of it only renders once `initKoin()` has run.
/// This view takes the state directly and previews on its own.
struct SettingsContent: View {
    let state: SettingsState
    var isCompact: Bool = true
    var onProfileClick: () -> Void = {}
    var onApiKeyClick: () -> Void = {}
    var onYouCamClick: () -> Void = {}
    var onSetOnDeviceAi: (Bool) -> Void = { _ in }

    private var contentPadding: CGFloat { isCompact ? 24 : 32 }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(String(localized: "settings_title"))
                        .font(.title.weight(.semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .padding(.top, 24)
                        .padding(.bottom, 28)

                    sectionLabel(String(localized: "settings_section_profile"))
                    settingsCard(
                        iconColor: WornColors.accentGreen,
                        iconName: "person.fill",
                        title: String(localized: "settings_your_profile"),
                        subtitle: profileSummary,
                        action: onProfileClick
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_profile_card")

                    sectionLabel(String(localized: "settings_section_ai"))
                        .padding(.top, 24)
                    // Listed before the key card: it is free and private, so it should be the
                    // first option a new user sees.
                    settingsToggleCard(
                        iconColor: WornColors.accentGreen,
                        iconName: "iphone",
                        title: String(localized: "settings_on_device_ai_title"),
                        subtitle: onDeviceAiSubtitle,
                        isOn: Binding(
                            get: { state.onDeviceAiEnabled },
                            set: onSetOnDeviceAi
                        ),
                        enabled: OnDeviceAiAvailabilityKt.isUsable(state.onDeviceAiAvailability)
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_on_device_ai_toggle")

                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "sparkles",
                        title: String(localized: "settings_api_key_title"),
                        subtitle: state.hasApiKey ? String(localized: "settings_api_key_connected") : String(localized: "settings_api_key_required"),
                        action: onApiKeyClick
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_api_key_card")

                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "tshirt",
                        title: String(localized: "settings_youcam_title"),
                        subtitle: state.hasYouCamKey ? String(localized: "settings_youcam_connected") : String(localized: "settings_youcam_required"),
                        action: onYouCamClick
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_youcam_card")

                    sectionLabel(String(localized: "settings_section_about"))
                        .padding(.top, 24)
                    aboutCard
                        .padding(.top, 10)

                    donationCard
                        .padding(.top, 16)

                    Spacer().frame(height: 95)
                }
                .padding(.horizontal, contentPadding)
            }
            .background(WornColors.bgPage)
        }
        .accessibilityIdentifier("settings_screen")
    }

    private var onDeviceAiSubtitle: String {
        switch state.onDeviceAiAvailability {
        case is OnDeviceAiAvailabilityAvailable:
            return String(localized: "settings_on_device_ai_available")
        case is OnDeviceAiAvailabilityDownloadable:
            return String(localized: "settings_on_device_ai_downloading")
        case let unavailable as OnDeviceAiAvailabilityUnavailable:
            switch unavailable.reason {
            case .unsupportedDevice: return String(localized: "settings_on_device_ai_unsupported_device")
            case .unsupportedOs: return String(localized: "settings_on_device_ai_unsupported_os")
            case .disabledByUser: return String(localized: "settings_on_device_ai_disabled_by_user")
            default: return String(localized: "settings_on_device_ai_unavailable")
            }
        default:
            return String(localized: "settings_on_device_ai_unavailable")
        }
    }

    private var profileSummary: String {
        let profile = state.userProfile
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
            .font(.caption.weight(.medium))
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
                        .font(.callout.weight(.medium))
                        .foregroundColor(WornColors.textPrimary)
                    Text(subtitle)
                        .font(.footnote)
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

    /// A `settingsCard` whose trailing affordance is a switch instead of a chevron. The switch is
    /// disabled — rather than hidden — when the capability is missing, so the subtitle can say why.
    private func settingsToggleCard(
        iconColor: Color,
        iconName: String,
        title: String,
        subtitle: String,
        isOn: Binding<Bool>,
        enabled: Bool
    ) -> some View {
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
                    .font(.callout.weight(.medium))
                    .foregroundColor(WornColors.textPrimary)
                Text(subtitle)
                    .font(.footnote)
                    .foregroundColor(WornColors.textSecondary)
            }
            Spacer()
            Toggle("", isOn: isOn)
                .labelsHidden()
                .tint(WornColors.accentGreen)
                .disabled(!enabled)
        }
        .padding(16)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var aboutCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text(String(localized: "settings_version"))
                    .font(.subheadline)
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                Text(appVersion)
                    .font(.subheadline)
                    .foregroundColor(WornColors.textSecondary)
            }
            .padding(16)

            Divider().overlay(WornColors.borderSubtle.opacity(0.5))

            Button {
                if let url = URL(string: feedbackURL) {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack {
                    Text(String(localized: "settings_suggestions_bugs"))
                        .font(.subheadline)
                        .foregroundColor(WornColors.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.iconMuted)
                }
                .padding(16)
            }
            .buttonStyle(.plain)

            Divider().overlay(WornColors.borderSubtle.opacity(0.5))

            Button {
                if let url = URL(string: licenseURL) {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack {
                    Text(String(localized: "settings_licenses"))
                        .font(.subheadline)
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

    @State private var showCopied = false

    private var donationCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(String(localized: "settings_donate_title"))
                .font(.subheadline.weight(.medium))
                .foregroundColor(WornColors.textPrimary)

            Text(String(localized: "settings_donate_subtitle"))
                .font(.footnote)
                .foregroundColor(WornColors.textSecondary)

            Button {
                UIPasteboard.general.string = donationLNAddress
                showCopied = true
            } label: {
                HStack {
                    Text(donationLNAddress)
                        .font(.footnote.weight(.medium))
                        .foregroundColor(WornColors.accentGreen)
                    Spacer()
                    Text(showCopied ? String(localized: "settings_donate_copied") : String(localized: "settings_donate_copy"))
                        .font(.caption.weight(.medium))
                        .foregroundColor(WornColors.textSecondary)
                }
                .padding(12)
                .background(WornColors.bgElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(WornColors.borderSubtle, lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
        }
        .padding(16)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private let donationLNAddress = "jvsena42@blink.sv"
private let feedbackURL = "https://github.com/jvsena42/worn/issues/new"
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
                        .font(.title2.weight(.semibold))
                        .foregroundColor(WornColors.textPrimary)
                    Text(String(localized: "settings_profile_help"))
                        .font(.subheadline)
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
                        .accessibilityIdentifier("profile_save_button")
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
        }
        .accessibilityIdentifier("profile_sheet")
    }

    private var bodyTypeOptions: [(BodyType, String)] {
        [(.slim, String(localized: "body_type_slim")), (.athletic, String(localized: "body_type_athletic")), (.average, String(localized: "body_type_average")),
         (.stocky, String(localized: "body_type_stocky")), (.short_, String(localized: "body_type_short")), (.tallAndSlim, String(localized: "body_type_tall_and_slim")),
         (.tallAndFit, String(localized: "body_type_tall_and_fit")), (.bigAndTall, String(localized: "body_type_big_and_tall"))]
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
                .font(.title2.weight(.semibold))
                .foregroundColor(WornColors.textPrimary)
            Text(String(localized: "settings_api_description"))
                .font(.subheadline)
                .foregroundColor(WornColors.textSecondary)
            Text(String(localized: "settings_api_get_key"))
                .font(.footnote.weight(.medium))
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
                .font(.subheadline)

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
            .accessibilityIdentifier("api_key_field")

            saveGradientButton(text: String(localized: "settings_save_connect"), enabled: !hasApiKey && !keyInput.isEmpty) {
                onSave(keyInput)
                keyInput = ""
                dismiss()
            }
            .accessibilityIdentifier("api_key_save_button")

            if hasApiKey {
                HStack {
                    Spacer()
                    Button {
                        onClear()
                        dismiss()
                    } label: {
                        Text(String(localized: "settings_remove_key"))
                            .font(.subheadline.weight(.medium))
                            .foregroundColor(WornColors.textSecondary)
                    }
                    .accessibilityIdentifier("api_key_remove_button")
                    Spacer()
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 24)
        .accessibilityIdentifier("api_key_sheet")
    }
}

// MARK: - YouCam Credentials Sheet

private struct YouCamCredentialsSheet: View {
    let hasCredentials: Bool
    let verifying: Bool
    let errorMessage: String?
    let onSave: (String, String) -> Void
    let onClear: () -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var clientId = ""
    @State private var clientSecret = ""
    @State private var idVisible = false
    @State private var secretVisible = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(String(localized: "settings_youcam_title"))
                    .font(.title2.weight(.semibold))
                    .foregroundColor(WornColors.textPrimary)
                Text(String(localized: "settings_youcam_description"))
                    .font(.subheadline)
                    .foregroundColor(WornColors.textSecondary)
                Text(String(localized: "settings_youcam_get_key"))
                    .font(.footnote.weight(.medium))
                    .foregroundColor(WornColors.accentGreen)

                fieldLabel(String(localized: "settings_youcam_client_id_hint"))
                credentialField(text: hasCredentials ? .constant("••••••••••••") : $clientId, visible: $idVisible, enabled: !hasCredentials)
                    .accessibilityIdentifier("youcam_client_id_field")
                fieldLabel(String(localized: "settings_youcam_client_secret_hint"))
                credentialField(text: hasCredentials ? .constant("••••••••••••") : $clientSecret, visible: $secretVisible, enabled: !hasCredentials)
                    .accessibilityIdentifier("youcam_client_secret_field")

                saveGradientButton(
                    text: String(localized: verifying ? "settings_youcam_verifying" : "settings_youcam_save"),
                    enabled: !hasCredentials && !verifying && !clientId.isEmpty && !clientSecret.isEmpty
                ) {
                    onSave(clientId, clientSecret)
                }
                .accessibilityIdentifier("youcam_save_button")

                if let errorMessage, !verifying {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundColor(WornColors.deleteRed)
                        .accessibilityIdentifier("youcam_error")
                }

                if hasCredentials {
                    HStack {
                        Spacer()
                        Button {
                            onClear()
                            dismiss()
                        } label: {
                            Text(String(localized: "settings_youcam_remove"))
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(WornColors.textSecondary)
                        }
                        .accessibilityIdentifier("youcam_remove_button")
                        Spacer()
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 24)
        }
        .background(WornColors.bgElevated)
        .accessibilityIdentifier("youcam_sheet")
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.footnote.weight(.semibold))
            .foregroundColor(WornColors.textPrimary)
    }

    private func credentialField(text: Binding<String>, visible: Binding<Bool>, enabled: Bool) -> some View {
        HStack {
            Group {
                if visible.wrappedValue {
                    TextField("", text: text)
                } else {
                    SecureField("", text: text)
                }
            }
            .disabled(!enabled)
            .font(.subheadline)

            Button { visible.wrappedValue.toggle() } label: {
                Image(systemName: visible.wrappedValue ? "eye" : "eye.slash")
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
    }
}

// MARK: - Shared chip components

private func chipGroup<T: Equatable>(
    title: String, options: [(T, String)], selected: T?, onSelected: @escaping (T?) -> Void
) -> some View {
    VStack(alignment: .leading, spacing: 10) {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundColor(WornColors.textPrimary)
        FlowLayout(spacing: 8) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, item in
                let (value, label) = item
                let isActive = value == selected
                WornChip(label: label, isActive: isActive) {
                    onSelected(isActive ? nil : value)
                }
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
                .font(.subheadline.weight(.semibold))
                .foregroundColor(WornColors.textPrimary)
            Text(String(localized: "settings_multi_select"))
                .font(.caption)
                .foregroundColor(WornColors.textMuted)
        }
        FlowLayout(spacing: 8) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, item in
                let (value, label) = item
                let isActive = selected.contains(value)
                WornChip(label: label, isActive: isActive) {
                    onToggle(value)
                }
            }
        }
    }
}

private func saveGradientButton(text: String, enabled: Bool = true, action: @escaping () -> Void) -> some View {
    WornGradientButton(
        text: text,
        action: action,
        enabled: enabled
    )
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
        case .tallAndSlim: return String(localized: "body_type_tall_and_slim")
        case .tallAndFit: return String(localized: "body_type_tall_and_fit")
        case .bigAndTall: return String(localized: "body_type_big_and_tall")
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

private func previewSettingsState(
    availability: OnDeviceAiAvailability
) -> SettingsState {
    SettingsState(
        userProfile: UserProfile(
            bodyType: .athletic, styleProfile: .smartCasual, ageRange: .age2635,
            climate: .temperate, lifestyles: [.workOffice]
        ),
        isLoading: false,
        hasApiKey: true,
        hasYouCamKey: false,
        verifyingYouCam: false,
        youCamError: nil,
        onDeviceAiEnabled: false,
        onDeviceAiAvailability: availability,
        error: nil
    )
}

#Preview("iPhone") {
    SettingsContent(
        state: previewSettingsState(availability: OnDeviceAiAvailabilityAvailable()),
        isCompact: true
    )
}

#Preview("iPhone · Dark") {
    SettingsContent(
        state: previewSettingsState(availability: OnDeviceAiAvailabilityAvailable()),
        isCompact: true
    )
    .preferredColorScheme(.dark)
}

#Preview("iPhone - AI unavailable") {
    SettingsContent(
        state: previewSettingsState(
            availability: OnDeviceAiAvailabilityUnavailable(reason: .unsupportedDevice)
        ),
        isCompact: true
    )
}

#Preview("iPhone - AI unavailable · Dark") {
    SettingsContent(
        state: previewSettingsState(
            availability: OnDeviceAiAvailabilityUnavailable(reason: .unsupportedDevice)
        ),
        isCompact: true
    )
    .preferredColorScheme(.dark)
}

#Preview("iPad Portrait", traits: .portrait) {
    SettingsContent(
        state: previewSettingsState(availability: OnDeviceAiAvailabilityAvailable()),
        isCompact: false
    )
}
