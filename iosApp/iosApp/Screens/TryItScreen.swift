import SwiftUI
import PhotosUI
import Shared

struct TryItScreen: View {
    @StateObject private var viewModel = TryItViewModelWrapper()
    let onTabSelected: (WornTab) -> Void
    var sharedPhoto: SharedPhoto?

    @State private var showShareReadFailed = false

    @State private var showSourceChooser = false
    @State private var showPhotoPicker = false
    @State private var garmentCover: PhotoCover?
    @State private var photoData: Data?
    @State private var photoImage: UIImage?
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var selectedItem: ClothingItem?

    @State private var showPersonSourceChooser = false
    @State private var showPersonPhotoPicker = false
    @State private var personCover: PhotoCover?
    @State private var selectedPersonPhotoItem: PhotosPickerItem?

    @Environment(\.horizontalSizeClass) private var sizeClass
    private var isCompact: Bool { sizeClass == .compact }
    private var contentPadding: CGFloat { isCompact ? 24 : 32 }

    /// Presentation is driven purely by ViewModel state, so every dismissal path — including an
    /// outside tap, which fires the `.cancel` button's action — goes through an intent. A real
    /// setter would race the FlowAdapter's async state delivery and undo the choice just made.
    private var featureChooserBinding: Binding<Bool> {
        Binding(get: { viewModel.state.featureChoiceRequired }, set: { _ in })
    }

    var body: some View {
        VStack(spacing: 0) {
            if !viewModel.state.hasApiKey && !viewModel.state.hasYouCamKey {
                aiEmptyContent
            } else {
                tryItContent
            }

        }
        .background(WornColors.bgPage)
        .accessibilityIdentifier("try_it_screen")
        .confirmationDialog(String(localized: "add_item_photo_dialog_title"), isPresented: $showSourceChooser) {
            Button(String(localized: "add_item_take_photo")) { garmentCover = .camera }
                .accessibilityIdentifier("photo_source_camera")
            Button(String(localized: "add_item_choose_gallery")) { showPhotoPicker = true }
                .accessibilityIdentifier("photo_source_gallery")
            Button(String(localized: "common_cancel"), role: .cancel) {}
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $selectedPhotoItem, matching: .images)
        .fullScreenCover(item: $garmentCover) { presented in
            switch presented {
            case .camera:
                CameraView(
                    onImageCaptured: { image in
                        photoImage = image
                        photoData = image.jpegData(compressionQuality: 0.9)
                        viewModel.reset()
                    },
                    onDismiss: { garmentCover = nil }
                )
                .ignoresSafeArea()
            case .crop:
                if let data = photoData {
                    CropEditorView(
                        imageData: data,
                        onCropped: { cropped in
                            photoData = cropped
                            photoImage = UIImage(data: cropped)
                            // The previous analysis described the uncropped photo.
                            viewModel.reset()
                            garmentCover = nil
                        },
                        onCancel: { garmentCover = nil }
                    )
                }
            }
        }
        .onChange(of: selectedPhotoItem) { _, newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self) {
                    photoData = data
                    photoImage = UIImage(data: data)
                    viewModel.reset()
                }
            }
        }
        .onChange(of: sharedPhoto) { _, photo in
            guard let photo = photo else { return }
            guard let data = photo.data else {
                showShareReadFailed = true
                return
            }
            photoData = data
            photoImage = UIImage(data: data)
            viewModel.reset()
            viewModel.receiveSharedPhoto()
        }
        .confirmationDialog(
            String(localized: "share_choose_feature_title"),
            isPresented: featureChooserBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "share_choose_analyze")) {
                viewModel.chooseFeature(.analysis)
            }
            .accessibilityIdentifier("share_choose_analyze")
            Button(String(localized: "share_choose_try_on")) {
                viewModel.chooseFeature(.virtualTryOn)
            }
            .accessibilityIdentifier("share_choose_try_on")
            Button(String(localized: "common_cancel"), role: .cancel) {
                viewModel.clearFeatureFocus()
            }
        }
        .alert(String(localized: "share_photo_read_failed"), isPresented: $showShareReadFailed) {
            Button(String(localized: "common_cancel"), role: .cancel) {}
        }
        .sheet(item: $selectedItem) { item in
            ItemDetailSheet(
                item: item,
                isCompact: isCompact,
                showActions: false,
                onEdit: { _ in },
                onDelete: { _ in }
            )
            .presentationDetents([.large])
        }
    }

    // MARK: - AI Empty State

    private var aiEmptyContent: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(WornColors.bgCard)
                    .frame(width: isCompact ? 130 : 150, height: isCompact ? 130 : 150)
                    .overlay(
                        Circle().stroke(WornColors.borderSubtle, lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.03), radius: 15)
                    .shadow(color: .black.opacity(0.03), radius: 4, y: 2)

                Image(systemName: "cpu")
                    .font(.system(size: isCompact ? 52 : 60))
                    .foregroundColor(WornColors.accentIndigo)
            }

            Text(String(localized: "tryit_locked_title"))
                .font(.system(size: isCompact ? 24 : 26, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
                .multilineTextAlignment(.center)

            Text(String(localized: "tryit_locked_description"))
                .font(.system(size: isCompact ? 15 : 16))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: isCompact ? 280 : 380)

            indigoCtaButton(text: String(localized: "tryit_open_settings")) {
                onTabSelected(.settings)
            }
            .accessibilityIdentifier("try_it_connect_cta")

            Spacer()
        }
        .padding(.horizontal, contentPadding)
    }

    // MARK: - Try It Content

    private var tryItContent: some View {
        ScrollViewReader { proxy in
            ScrollView {
                if isCompact {
                    phoneContent
                } else {
                    tabletContent
                }
            }
            .background(WornColors.bgPage)
            .onChange(of: viewModel.state.focusedFeature) { _, feature in
                guard let feature else { return }
                withAnimation {
                    proxy.scrollTo(
                        feature == .virtualTryOn ? Self.tryOnAnchor : Self.analysisAnchor,
                        anchor: .top
                    )
                }
                viewModel.clearFeatureFocus()
            }
        }
    }

    private static let analysisAnchor = "analysis"
    private static let tryOnAnchor = "tryOn"

    private var phoneContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            tryItTitle(fontSize: 28)
                .id(Self.analysisAnchor)
            uploadZone(height: 200)
            if photoData != nil { garmentCropButton }

            if viewModel.state.hasApiKey {
                if photoData != nil && viewModel.state.result == nil && !viewModel.state.isLoading {
                    analyzeButton
                }

                if viewModel.state.isLoading {
                    loadingIndicator
                }

                if let error = viewModel.state.error, !viewModel.state.isLoading {
                    errorContent(message: error as String)
                        .padding(.vertical, 20)
                }

                if let result = viewModel.state.result as? TryItResult {
                    resultsSection(result: result, thumbSize: 80)
                }
            }

            if viewModel.state.hasYouCamKey {
                tryOnSection(isCompact: true)
                    .id(Self.tryOnAnchor)
            }

            Spacer().frame(height: 95)
        }
        .padding(.horizontal, contentPadding)
    }

    private var tabletContent: some View {
        VStack(alignment: .leading, spacing: 28) {
            tryItTitle(fontSize: 32)
                .id(Self.analysisAnchor)

            HStack(alignment: .top, spacing: 32) {
                // Left column
                VStack(alignment: .leading, spacing: 24) {
                    uploadZone(height: 300)
                    if photoData != nil { garmentCropButton }

                    if viewModel.state.hasApiKey {
                        if photoData != nil && viewModel.state.result == nil && !viewModel.state.isLoading {
                            analyzeButton
                        }

                        if viewModel.state.isLoading {
                            loadingIndicator
                        }

                        if let error = viewModel.state.error, !viewModel.state.isLoading {
                            errorContent(message: error as String)
                        }

                        if let result = viewModel.state.result as? TryItResult {
                            pairsSection(matchingItems: result.matchingItems as! [ClothingItem], thumbSize: 90)
                        }
                    }

                    if viewModel.state.hasYouCamKey {
                        tryOnSection(isCompact: false)
                            .id(Self.tryOnAnchor)
                    }
                }
                .frame(maxWidth: .infinity)

                // Right column
                if viewModel.state.hasApiKey, let result = viewModel.state.result as? TryItResult {
                    VStack(alignment: .leading, spacing: 24) {
                        combinationsCard(count: Int(result.combinationsUnlocked), isCompact: false)
                        gapsFilledSection(gaps: result.gapsFilled as! [String], isCompact: false)
                        decisionBanner(worthAdding: result.worthAdding, isCompact: false)
                    }
                    .frame(maxWidth: .infinity)
                }
            }

            Spacer().frame(height: 95)
        }
        .padding(.horizontal, contentPadding)
    }

    // MARK: - Components

    private func tryItTitle(fontSize: CGFloat) -> some View {
        Text(String(localized: "tryit_title"))
            .font(.system(size: fontSize, weight: .semibold))
            .foregroundColor(WornColors.textPrimary)
            .tracking(-0.8)
    }

    private var garmentCropButton: some View {
        CropPhotoButton(action: { garmentCover = .crop })
            .accessibilityIdentifier("try_it_crop_button")
    }

    private func uploadZone(height: CGFloat) -> some View {
        Button { showSourceChooser = true } label: {
            ZStack {
                if let image = photoImage {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxWidth: .infinity, maxHeight: height)
                        .clipped()
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "camera")
                            .font(.system(size: 44))
                            .foregroundColor(WornColors.iconMuted)
                        Text(String(localized: "tryit_upload_hint"))
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(WornColors.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(WornColors.borderStrong, lineWidth: 1.5)
            )
            .shadow(color: .black.opacity(0.03), radius: 2, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("try_it_upload_zone")
    }

    private var analyzeButton: some View {
        WornGradientButton(
            text: String(localized: "tryit_analyze"),
            action: {
                guard let data = photoData else { return }
                viewModel.analyzePhoto(imageData: data)
            },
            gradientColors: WornGradients.indigo,
            cornerRadius: 28,
            shadowRadius: 10,
            shadowColor: WornColors.accentIndigo.opacity(0.15),
            shadowY: 6,
            icon: AnyView(
                Image(systemName: "cpu")
                    .font(.system(size: 18))
                    .foregroundColor(.white)
            ),
            fixedHeight: nil,
            contentPadding: EdgeInsets(top: 14, leading: 0, bottom: 14, trailing: 0)
        )
        .accessibilityIdentifier("try_it_analyze_button")
    }

    /// Analysis error, shared by the phone and tablet layouts. Retrying re-sends the garment photo
    /// that produced the error, so it is a no-op once the photo has been cleared.
    private func errorContent(message: String) -> some View {
        ErrorContentView(
            message: message,
            onRetry: {
                guard let data = photoData else { return }
                viewModel.analyzePhoto(imageData: data)
            },
            retryButtonColor: WornColors.accentIndigo
        )
    }

    private var loadingIndicator: some View {
        HStack {
            Spacer()
            ProgressView()
                .tint(WornColors.accentIndigo)
                .padding(.vertical, 40)
            Spacer()
        }
    }

    private func resultsSection(result: TryItResult, thumbSize: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            pairsSection(matchingItems: result.matchingItems as! [ClothingItem], thumbSize: thumbSize)
            combinationsCard(count: Int(result.combinationsUnlocked), isCompact: true)
            gapsFilledSection(gaps: result.gapsFilled as! [String], isCompact: true)
            decisionBanner(worthAdding: result.worthAdding, isCompact: true)
        }
    }

    private func pairsSection(matchingItems: [ClothingItem], thumbSize: CGFloat) -> some View {
        Group {
            if !matchingItems.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(String(localized: "tryit_pairs_with"))
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .tracking(-0.2)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(matchingItems) { item in
                                Button { selectedItem = item } label: {
                                    itemThumbnail(item: item, size: thumbSize)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
    }

    private func itemThumbnail(item: ClothingItem, size: CGFloat) -> some View {
        ZStack {
            if FileManager.default.fileExists(atPath: item.photoPath) {
                AsyncImage(url: URL(fileURLWithPath: item.photoPath)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Image(systemName: "tshirt")
                        .font(.system(size: 28))
                        .foregroundColor(WornColors.textSecondary)
                }
                .frame(width: size, height: size)
                .clipped()
            } else {
                Image(systemName: "tshirt")
                    .font(.system(size: 28))
                    .foregroundColor(WornColors.textSecondary)
            }
        }
        .frame(width: size, height: size)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(WornColors.borderSubtle, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private func combinationsCard(count: Int, isCompact: Bool) -> some View {
        let cardHeight: CGFloat = isCompact ? 90 : 110
        let valueSize: CGFloat = isCompact ? 40 : 44

        return VStack(alignment: .leading, spacing: 4) {
            Text(String(localized: "tryit_combinations_unlocked"))
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(WornColors.textSecondary)
                .tracking(0.5)
            Text("\(count)")
                .font(.system(size: valueSize, weight: .bold))
                .foregroundColor(WornColors.accentGreen)
                .tracking(-1.2)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: cardHeight)
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(WornColors.borderSubtle, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private func gapsFilledSection(gaps: [String], isCompact: Bool) -> some View {
        Group {
            if !gaps.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(String(localized: "tryit_gaps_filled"))
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(WornColors.textPrimary)
                        .tracking(-0.2)

                    ForEach(Array(gaps.enumerated()), id: \.offset) { _, gap in
                        HStack(spacing: 10) {
                            Circle()
                                .fill(WornColors.accentGreen)
                                .frame(width: 8, height: 8)
                            Text(gap)
                                .font(.system(size: isCompact ? 14 : 15))
                                .foregroundColor(WornColors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    private func decisionBanner(worthAdding: Bool, isCompact: Bool) -> some View {
        let bannerHeight: CGFloat = isCompact ? 56 : 60
        let gradientColors: [Color] = worthAdding
            ? [WornColors.accentGreen, WornColors.accentGreenDark]
            : [Color(hex: "8B7D7D"), Color(hex: "6B5E5E")]
        let iconName = worthAdding ? "checkmark.circle" : "xmark.circle"
        let text = worthAdding ? String(localized: "tryit_worth_adding") : String(localized: "tryit_skip")

        return HStack(spacing: 10) {
            Image(systemName: iconName)
                .font(.system(size: 22))
                .foregroundColor(.white)
            Text(text)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .frame(height: bannerHeight)
        .background(
            LinearGradient(colors: gradientColors, startPoint: .top, endPoint: .bottom)
        )
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .shadow(color: WornColors.accentGreen.opacity(0.15), radius: 10, y: 6)
    }

    private func indigoCtaButton(text: String, action: @escaping () -> Void) -> some View {
        WornGradientButton(
            text: text,
            action: action,
            gradientColors: WornGradients.indigo,
            cornerRadius: 28,
            shadowRadius: 10,
            shadowColor: WornColors.accentIndigo.opacity(0.15),
            shadowY: 6,
            fillMaxWidth: false,
            fixedHeight: nil,
            contentPadding: EdgeInsets(top: 14, leading: 40, bottom: 14, trailing: 40)
        )
    }

    // MARK: - Virtual Try-On

    private var categoryOptions: [(GarmentCategory, String)] {
        [
            (.top, String(localized: "tryit_category_top")),
            (.bottom, String(localized: "tryit_category_bottom")),
            (.fullBody, String(localized: "tryit_category_full")),
            (.shoes, String(localized: "tryit_category_shoes")),
        ]
    }

    @ViewBuilder
    private func tryOnSection(isCompact: Bool) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String(localized: "tryit_your_photo_title"))
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
                .tracking(-0.2)

            personPhotoZone(height: isCompact ? 200 : 260)

            if viewModel.personImageData != nil {
                CropPhotoButton(action: { personCover = .crop })
                    .accessibilityIdentifier("try_on_person_crop_button")
            }

            Text(String(localized: "tryit_tryon_category_title"))
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
                .tracking(-0.2)

            categorySelector

            if photoData != nil && viewModel.state.selectedCategory != nil
                && viewModel.personImageData != nil
                && viewModel.state.tryOnImage == nil && !viewModel.state.tryOnLoading {
                seeItOnMeButton
            }

            if viewModel.state.tryOnLoading {
                tryOnLoadingIndicator
            }

            if let error = viewModel.state.tryOnError, !viewModel.state.tryOnLoading {
                ErrorContentView(
                    message: error as String,
                    onRetry: { generateTryOn() },
                    retryButtonColor: WornColors.accentIndigo
                )
                .padding(.vertical, 16)
            }

            if let imageData = viewModel.tryOnImageData {
                tryOnResultView(imageData: imageData, height: isCompact ? 320 : 400)
            }

            Text(String(localized: "tryit_tryon_cost_note"))
                .font(.system(size: 12))
                .foregroundColor(WornColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityIdentifier("try_on_section")
        .confirmationDialog(String(localized: "add_item_photo_dialog_title"), isPresented: $showPersonSourceChooser) {
            Button(String(localized: "add_item_take_photo")) { personCover = .camera }
            Button(String(localized: "add_item_choose_gallery")) { showPersonPhotoPicker = true }
            Button(String(localized: "common_cancel"), role: .cancel) {}
        }
        .photosPicker(isPresented: $showPersonPhotoPicker, selection: $selectedPersonPhotoItem, matching: .images)
        .fullScreenCover(item: $personCover) { presented in
            switch presented {
            case .camera:
                CameraView(
                    onImageCaptured: { image in
                        if let data = image.jpegData(compressionQuality: 0.9) {
                            viewModel.setPersonPhoto(imageData: data)
                        }
                    },
                    onDismiss: { personCover = nil }
                )
                .ignoresSafeArea()
            case .crop:
                if let data = viewModel.personImageData {
                    CropEditorView(
                        imageData: data,
                        onCropped: { cropped in
                            // Round-tripping through the view model also persists the crop.
                            viewModel.setPersonPhoto(imageData: cropped)
                            personCover = nil
                        },
                        onCancel: { personCover = nil }
                    )
                }
            }
        }
        .onChange(of: selectedPersonPhotoItem) { _, newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self) {
                    viewModel.setPersonPhoto(imageData: data)
                }
            }
        }
    }

    private func personPhotoZone(height: CGFloat) -> some View {
        Button { showPersonSourceChooser = true } label: {
            ZStack {
                if let data = viewModel.personImageData, let image = UIImage(data: data) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxWidth: .infinity, maxHeight: height)
                        .clipped()
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "camera")
                            .font(.system(size: 44))
                            .foregroundColor(WornColors.iconMuted)
                        Text(String(localized: "tryit_your_photo_hint"))
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(WornColors.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(WornColors.borderStrong, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("try_on_person_zone")
    }

    private var categorySelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(categoryOptions.enumerated()), id: \.offset) { _, item in
                    let (category, label) = item
                    let selected = viewModel.state.selectedCategory == category
                    Button { viewModel.selectCategory(category) } label: {
                        Text(label)
                            .font(.system(size: 14, weight: .medium))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(selected ? WornColors.accentIndigo : WornColors.bgCard)
                            .foregroundColor(selected ? .white : WornColors.textPrimary)
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(WornColors.borderSubtle, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .accessibilityIdentifier("try_on_category_selector")
    }

    private var seeItOnMeButton: some View {
        WornGradientButton(
            text: String(localized: "tryit_see_on_me"),
            action: { generateTryOn() },
            gradientColors: WornGradients.indigo,
            cornerRadius: 28,
            shadowRadius: 10,
            shadowColor: WornColors.accentIndigo.opacity(0.15),
            shadowY: 6,
            icon: AnyView(
                Image(systemName: "sparkles")
                    .font(.system(size: 18))
                    .foregroundColor(.white)
            ),
            fixedHeight: nil,
            contentPadding: EdgeInsets(top: 14, leading: 0, bottom: 14, trailing: 0)
        )
        .accessibilityIdentifier("try_on_generate_button")
    }

    private var tryOnLoadingIndicator: some View {
        VStack(spacing: 12) {
            ProgressView().tint(WornColors.accentIndigo)
            Text(String(localized: "tryit_tryon_generating"))
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }

    private func tryOnResultView(imageData: Data, height: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "tryit_tryon_result_title"))
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)
                .tracking(-0.2)

            ZStack {
                if let image = UIImage(data: imageData) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxWidth: .infinity, maxHeight: height)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier("try_on_result")
        }
    }

    private func generateTryOn() {
        guard let data = photoData else { return }
        viewModel.generateTryOn(imageData: data)
    }
}

// MARK: - Previews

#Preview("iPhone") {
    TryItScreen(onTabSelected: { _ in })
}

#Preview("iPad Portrait", traits: .portrait) {
    TryItScreen(onTabSelected: { _ in })
}
