import SwiftUI
import PhotosUI
import Shared

struct TryItScreen: View {
    @StateObject private var viewModel = TryItViewModelWrapper()
    let onTabSelected: (WornTab) -> Void

    @State private var showSourceChooser = false
    @State private var showPhotoPicker = false
    @State private var showCamera = false
    @State private var photoData: Data?
    @State private var photoImage: UIImage?
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var selectedItem: ClothingItem?

    @Environment(\.horizontalSizeClass) private var sizeClass
    private var isCompact: Bool { sizeClass == .compact }
    private var contentPadding: CGFloat { isCompact ? 24 : 32 }

    var body: some View {
        VStack(spacing: 0) {
            if !viewModel.state.hasApiKey {
                aiEmptyContent
            } else {
                tryItContent
            }

            WornBottomBar(activeTab: .tryIt, onTabSelected: onTabSelected, isCompact: isCompact)
        }
        .background(WornColors.bgPage)
        .confirmationDialog("Add photo", isPresented: $showSourceChooser) {
            Button("Take Photo") { showCamera = true }
            Button("Choose from Library") { showPhotoPicker = true }
            Button("Cancel", role: .cancel) {}
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $selectedPhotoItem, matching: .images)
        .fullScreenCover(isPresented: $showCamera) {
            CameraView(
                onImageCaptured: { image in
                    photoImage = image
                    photoData = image.jpegData(compressionQuality: 0.9)
                    viewModel.reset()
                },
                onDismiss: { showCamera = false }
            )
            .ignoresSafeArea()
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

            Text("AI powered analysis")
                .font(.system(size: isCompact ? 24 : 26, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
                .multilineTextAlignment(.center)

            Text("Connect your Claude API key in Settings to analyze items against your wardrobe.")
                .font(.system(size: isCompact ? 15 : 16))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: isCompact ? 280 : 380)

            indigoCtaButton(text: "Connect Claude AI") {
                onTabSelected(.settings)
            }

            Spacer()
        }
        .padding(.horizontal, contentPadding)
    }

    // MARK: - Try It Content

    private var tryItContent: some View {
        ScrollView {
            if isCompact {
                phoneContent
            } else {
                tabletContent
            }
        }
        .background(WornColors.bgPage)
    }

    private var phoneContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            tryItTitle(fontSize: 28)
            uploadZone(height: 200)

            if photoData != nil && viewModel.state.result == nil && !viewModel.state.isLoading {
                analyzeButton
            }

            if viewModel.state.isLoading {
                loadingIndicator
            }

            if let error = viewModel.state.error, !viewModel.state.isLoading {
                errorMessage(error)
            }

            if let result = viewModel.state.result as? TryItResult {
                resultsSection(result: result, thumbSize: 80)
            }

            Spacer().frame(height: 12)
        }
        .padding(.horizontal, contentPadding)
    }

    private var tabletContent: some View {
        VStack(alignment: .leading, spacing: 28) {
            tryItTitle(fontSize: 32)

            HStack(alignment: .top, spacing: 32) {
                // Left column
                VStack(alignment: .leading, spacing: 24) {
                    uploadZone(height: 300)

                    if photoData != nil && viewModel.state.result == nil && !viewModel.state.isLoading {
                        analyzeButton
                    }

                    if viewModel.state.isLoading {
                        loadingIndicator
                    }

                    if let error = viewModel.state.error, !viewModel.state.isLoading {
                        errorMessage(error)
                    }

                    if let result = viewModel.state.result as? TryItResult {
                        pairsSection(matchingItems: result.matchingItems as! [ClothingItem], thumbSize: 90)
                    }
                }
                .frame(maxWidth: .infinity)

                // Right column
                if let result = viewModel.state.result as? TryItResult {
                    VStack(alignment: .leading, spacing: 24) {
                        combinationsCard(count: Int(result.combinationsUnlocked), isCompact: false)
                        gapsFilledSection(gaps: result.gapsFilled as! [String], isCompact: false)
                        decisionBanner(worthAdding: result.worthAdding, isCompact: false)
                    }
                    .frame(maxWidth: .infinity)
                }
            }

            Spacer().frame(height: 32)
        }
        .padding(.horizontal, contentPadding)
    }

    // MARK: - Components

    private func tryItTitle(fontSize: CGFloat) -> some View {
        Text("Would it fit your wardrobe?")
            .font(.system(size: fontSize, weight: .semibold))
            .foregroundColor(WornColors.textPrimary)
            .tracking(-0.8)
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
                        Text("Upload a photo of the item\nyou're considering")
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
    }

    private var analyzeButton: some View {
        Button {
            guard let data = photoData else { return }
            viewModel.analyzePhoto(imageData: data)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "cpu")
                    .font(.system(size: 18))
                Text("Analyze with Claude")
                    .font(.system(size: 16, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                LinearGradient(
                    colors: [WornColors.accentIndigo, Color(hex: "556070")],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 28))
            .shadow(color: WornColors.accentIndigo.opacity(0.15), radius: 10, y: 6)
        }
        .buttonStyle(.plain)
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

    private func errorMessage(_ message: String) -> some View {
        Text(message)
            .font(.system(size: 14))
            .foregroundColor(WornColors.deleteRed)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(WornColors.deleteRed.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
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
                    Text("It would pair with...")
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
            Text("Combinations unlocked")
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
                    Text("Wardrobe gaps it fills")
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
        let text = worthAdding ? "Worth adding" : "Skip this one"

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
        Button(action: action) {
            Text(text)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 40)
                .padding(.vertical, 14)
                .background(
                    LinearGradient(
                        colors: [WornColors.accentIndigo, Color(hex: "556070")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .shadow(color: WornColors.accentIndigo.opacity(0.15), radius: 10, y: 6)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Previews

#Preview("iPhone") {
    TryItScreen(onTabSelected: { _ in })
}

#Preview("iPad") {
    TryItScreen(onTabSelected: { _ in })
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
