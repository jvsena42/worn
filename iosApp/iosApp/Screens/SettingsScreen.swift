import SwiftUI
import PhotosUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModelWrapper()
    let onTabSelected: (Tab) -> Void

    @State private var showProfileSheet = false
    @State private var showApiKeySheet = false
    @State private var showYouCamSheet = false
    @State private var showModelPhotoDialog = false
    @State private var showModelPhotoPicker = false
    @State private var showModelCamera = false
    @State private var selectedModelPhotoItem: PhotosPickerItem?

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
                    .accessibilityIdentifier("settings_profile_card")

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
                    .accessibilityIdentifier("settings_api_key_card")

                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "tshirt",
                        title: String(localized: "settings_youcam_title"),
                        subtitle: viewModel.state.hasYouCamKey ? String(localized: "settings_youcam_connected") : String(localized: "settings_youcam_required"),
                        action: { showYouCamSheet = true }
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_youcam_card")

                    settingsCard(
                        iconColor: WornColors.accentIndigo,
                        iconName: "camera",
                        title: String(localized: "settings_model_photo_title"),
                        subtitle: viewModel.state.hasModelPhoto ? String(localized: "settings_model_photo_saved") : String(localized: "settings_model_photo_required"),
                        action: { showModelPhotoDialog = true }
                    )
                    .padding(.top, 10)
                    .accessibilityIdentifier("settings_model_photo_card")

                    sectionLabel(String(localized: "settings_section_about"))
                        .padding(.top, 24)
                    aboutCard
                        .padding(.top, 10)

                    donationCard
                        .padding(.top, 16)

                    Spacer().frame(height: 95)
                }
                .padding(.horizontal, 24)
            }
            .background(WornColors.bgPage)
        }
        .accessibilityIdentifier("settings_screen")
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
                onSave: { viewModel.saveYouCamCredentials(clientId: $0, clientSecret: $1) },
                onClear: { viewModel.clearYouCamCredentials() }
            )
            .presentationDetents([.medium, .large])
        }
        .confirmationDialog(String(localized: "settings_model_photo_title"), isPresented: $showModelPhotoDialog, titleVisibility: .visible) {
            Button(String(localized: "settings_model_photo_add")) { showModelCamera = true }
            Button(String(localized: "settings_model_photo_change")) { showModelPhotoPicker = true }
            if viewModel.state.hasModelPhoto {
                Button(String(localized: "settings_model_photo_remove"), role: .destructive) { viewModel.clearModelPhoto() }
            }
            Button(String(localized: "common_cancel"), role: .cancel) {}
        } message: {
            Text(String(localized: "settings_model_photo_description"))
        }
        .photosPicker(isPresented: $showModelPhotoPicker, selection: $selectedModelPhotoItem, matching: .images)
        .fullScreenCover(isPresented: $showModelCamera) {
            CameraView(
                onImageCaptured: { image in
                    if let data = image.jpegData(compressionQuality: 0.9) {
                        viewModel.saveModelPhoto(data)
                    }
                },
                onDismiss: { showModelCamera = false }
            )
            .ignoresSafeArea()
        }
        .onChange(of: selectedModelPhotoItem) { _, newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self) {
                    viewModel.saveModelPhoto(data)
                }
            }
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
                if let url = URL(string: feedbackURL) {
                    UIApplication.shared.open(url)
                }
            } label: {
                HStack {
                    Text(String(localized: "settings_suggestions_bugs"))
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

    @State private var showCopied = false

    private var donationCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(String(localized: "settings_donate_title"))
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(WornColors.textPrimary)

            Text(String(localized: "settings_donate_subtitle"))
                .font(.system(size: 13))
                .foregroundColor(WornColors.textSecondary)

            Button {
                UIPasteboard.general.string = donationLNAddress
                showCopied = true
            } label: {
                HStack {
                    Text(donationLNAddress)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(WornColors.accentGreen)
                    Spacer()
                    Text(showCopied ? String(localized: "settings_donate_copied") : String(localized: "settings_donate_copy"))
                        .font(.system(size: 12, weight: .medium))
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
         (.stocky, String(localized: "body_type_stocky")), (.short_, String(localized: "body_type_short")), (.tallAndSlim, String(localized: "body_type_tall_slim")),
         (.tallAndFit, String(localized: "body_type_tall_and_fit")), (.bigAndTall, String(localized: "body_type_big_tall"))]
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
                            .font(.system(size: 14, weight: .medium))
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
    let onSave: (String, String) -> Void
    let onClear: () -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var clientId = ""
    @State private var clientSecret = ""
    @State private var idVisible = true
    @State private var secretVisible = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(String(localized: "settings_youcam_title"))
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(WornColors.textPrimary)
                Text(String(localized: "settings_youcam_description"))
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.textSecondary)
                Text(String(localized: "settings_youcam_get_key"))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(WornColors.accentGreen)

                fieldLabel(String(localized: "settings_youcam_client_id_hint"))
                credentialField(text: hasCredentials ? .constant("••••••••••••") : $clientId, visible: $idVisible, enabled: !hasCredentials)
                    .accessibilityIdentifier("youcam_client_id_field")
                fieldLabel(String(localized: "settings_youcam_client_secret_hint"))
                credentialField(text: hasCredentials ? .constant("••••••••••••") : $clientSecret, visible: $secretVisible, enabled: !hasCredentials)
                    .accessibilityIdentifier("youcam_client_secret_field")

                saveGradientButton(
                    text: String(localized: "settings_youcam_save"),
                    enabled: !hasCredentials && !clientId.isEmpty && !clientSecret.isEmpty
                ) {
                    onSave(clientId, clientSecret)
                    clientId = ""
                    clientSecret = ""
                    dismiss()
                }
                .accessibilityIdentifier("youcam_save_button")

                if hasCredentials {
                    HStack {
                        Spacer()
                        Button {
                            onClear()
                            dismiss()
                        } label: {
                            Text(String(localized: "settings_youcam_remove"))
                                .font(.system(size: 14, weight: .medium))
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
            .font(.system(size: 13, weight: .semibold))
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
            .font(.system(size: 15))

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
            .font(.system(size: 14, weight: .semibold))
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
        case .tallAndSlim: return String(localized: "body_type_tall_slim")
        case .tallAndFit: return String(localized: "body_type_tall_and_fit")
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
