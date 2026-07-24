import SwiftUI
import PhotosUI
import Shared

struct AddItemSheet: View {
    let isSaving: Bool
    let hasApiKey: Bool
    var existingItem: ClothingItem?
    let onSave: (Data, String, Category, [String], [Season], Subcategory?, Fit?, Material?) -> Void
    let onDismiss: () -> Void

    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var photoData: Data?
    @State private var originalPhotoData: Data?
    @State private var photoImage: UIImage?
    @State private var bgRemoved = false
    @State private var isProcessingBg = false
    @State private var showBgError = false
    @State private var name = ""
    @State private var selectedCategory: Category?
    @State private var selectedColors: Set<String> = []
    @State private var selectedSeasons: Set<Season> = []
    @State private var selectedSubcategory: Subcategory?
    @State private var selectedFit: Fit?
    @State private var selectedMaterial: Material?
    @State private var showSourceChooser = false
    @State private var showPhotoPicker = false
    @State private var showCamera = false
    @State private var showAiLockedSheet = false
    @State private var didInitFromExisting = false

    private let colorPalette: [(name: String, color: Color)] = [
        ("White", Color(hex: "FFFFFF")),
        ("Cream", Color(hex: "EDE8E1")),
        ("Black", Color(hex: "2C2924")),
        ("Navy", Color(hex: "2B4570")),
        ("Grey", Color(hex: "808080")),
        ("Charcoal", Color(hex: "36454F")),
        ("Olive", Color(hex: "6B7B3F")),
        ("Beige", Color(hex: "C4A882")),
        ("Khaki", Color(hex: "C3B091")),
        ("Tan", Color(hex: "D2B48C")),
        ("Brown", Color(hex: "8B4513")),
        ("Burgundy", Color(hex: "800020")),
        ("Coral", Color(hex: "A87560")),
        ("Light Blue", Color(hex: "ADD8E6")),
    ]

    private var isEditing: Bool { existingItem != nil }

    private var canSave: Bool {
        let hasPhoto = photoData != nil || (isEditing && existingItem?.photoPath.isEmpty == false)
        return hasPhoto && !name.isEmpty && selectedCategory != nil && !isSaving
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    photoUploadZone
                    if photoData != nil { removeBackgroundToggle }
                    if !isEditing { aiBadge }
                    nameField
                    categoryField
                    if selectedCategory != nil {
                        subcategoryField
                    }
                    colorSection
                    seasonSection
                    fitSection
                    materialSection
                    saveButton
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
            .navigationTitle(isEditing ? String(localized: "add_item_title_edit") : String(localized: "add_item_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common_cancel"), action: onDismiss)
                }
            }
            .confirmationDialog(String(localized: "add_item_photo_dialog_title"), isPresented: $showSourceChooser) {
                Button(String(localized: "add_item_take_photo")) { showCamera = true }
                    .accessibilityIdentifier("photo_source_camera")
                Button(String(localized: "add_item_choose_gallery")) { showPhotoPicker = true }
                    .accessibilityIdentifier("photo_source_gallery")
                Button(String(localized: "common_cancel"), role: .cancel) {}
            }
            .photosPicker(isPresented: $showPhotoPicker, selection: $selectedPhotoItem, matching: .images)
            .fullScreenCover(isPresented: $showCamera) {
                CameraView(
                    onImageCaptured: { image in
                        photoImage = image
                        let data = image.jpegData(compressionQuality: 0.9)
                        photoData = data
                        originalPhotoData = data
                        bgRemoved = false
                    },
                    onDismiss: { showCamera = false }
                )
                .ignoresSafeArea()
            }
            .onAppear {
                if let item = existingItem, !didInitFromExisting {
                    didInitFromExisting = true
                    name = item.name
                    selectedCategory = item.category
                    selectedColors = Set(item.colors)
                    selectedSeasons = Set(item.seasons)
                    selectedSubcategory = item.subcategory
                    selectedFit = item.fit
                    selectedMaterial = item.material
                    if !item.photoPath.isEmpty,
                       let uiImage = UIImage(contentsOfFile: item.photoPath) {
                        photoImage = uiImage
                    }
                }
            }
            .onChange(of: selectedPhotoItem) { _, newItem in
                Task {
                    if let data = try? await newItem?.loadTransferable(type: Data.self) {
                        photoData = data
                        originalPhotoData = data
                        photoImage = UIImage(data: data)
                        bgRemoved = false
                    }
                }
            }
            .alert(String(localized: "add_item_bg_removal_failed"), isPresented: $showBgError) {
                Button(String(localized: "common_ok"), role: .cancel) {}
            }
        }
        .accessibilityIdentifier("add_item_sheet")
    }

    private var photoUploadZone: some View {
        Button { showSourceChooser = true } label: {
            ZStack {
                if let image = photoImage {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxWidth: .infinity, maxHeight: 140)
                        .clipped()
                } else {
                    VStack(spacing: 8) {
                        Image(systemName: "camera")
                            .font(.system(size: 32))
                            .foregroundColor(WornColors.iconMuted)
                        Text(String(localized: "add_item_photo_hint"))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(WornColors.textSecondary)
                    }
                }
                if isProcessingBg {
                    WornColors.bgElevated.opacity(0.6)
                    ProgressView().tint(WornColors.accentGreen)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 140)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(WornColors.borderStrong, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
        .disabled(isProcessingBg)
        .accessibilityIdentifier("add_item_photo_zone")
    }

    private var removeBackgroundToggle: some View {
        Toggle(isOn: Binding(
            get: { bgRemoved },
            set: { onRemoveBackgroundChange($0) }
        )) {
            Text(String(localized: "add_item_remove_background"))
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
        }
        .tint(WornColors.accentGreen)
        .disabled(isProcessingBg)
        .accessibilityIdentifier("add_item_remove_bg_toggle")
    }

    private func onRemoveBackgroundChange(_ enabled: Bool) {
        guard let original = originalPhotoData else { return }
        if !enabled {
            photoData = original
            photoImage = UIImage(data: original)
            bgRemoved = false
            return
        }
        isProcessingBg = true
        Task {
            do {
                let processed = try await BackgroundRemoverService.removeBackground(original)
                photoData = processed
                photoImage = UIImage(data: processed)
                bgRemoved = true
            } catch {
                bgRemoved = false
                showBgError = true
            }
            isProcessingBg = false
        }
    }

    private var aiBadge: some View {
        Button {
            if !hasApiKey { showAiLockedSheet = true }
        } label: {
            HStack(spacing: 6) {
                Text("✦")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                Text(String(localized: "add_item_ai_badge"))
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(WornColors.accentIndigo)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("add_item_ai_badge")
        .sheet(isPresented: $showAiLockedSheet) {
            AiLockedSheet(onDismiss: { showAiLockedSheet = false })
                .presentationDetents([.medium])
        }
    }

    private var nameField: some View {
        TextField(String(localized: "add_item_name_hint"), text: $name)
            .font(.system(size: 15))
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier("add_item_name_field")
    }

    @State private var categoryExpanded = false

    private var categoryField: some View {
        VStack(spacing: 0) {
            Button { withAnimation { categoryExpanded.toggle() } } label: {
                HStack(spacing: 12) {
                    if let cat = selectedCategory {
                        Image(systemName: iconName(for: cat))
                            .font(.system(size: 16))
                            .foregroundColor(WornColors.textSecondary)
                            .frame(width: 20, height: 20)
                    }
                    Text(selectedCategory.map { displayName(for: $0) } ?? String(localized: "label_category"))
                        .font(.system(size: 15))
                        .foregroundColor(selectedCategory != nil ? WornColors.textPrimary : WornColors.iconMuted)
                    Spacer()
                    Image(systemName: categoryExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.iconMuted)
                }
                .padding(16)
            }
            .buttonStyle(.plain)

            if categoryExpanded {
                Divider().overlay(WornColors.borderSubtle)
                ForEach(Array(categoryOptions.enumerated()), id: \.offset) { index, item in
                    let (category, label) = item
                    Button {
                        selectedCategory = category
                        withAnimation { categoryExpanded = false }
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: iconName(for: category))
                                .font(.system(size: 16))
                                .foregroundColor(WornColors.textSecondary)
                                .frame(width: 20, height: 20)
                            Text(label)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(WornColors.textPrimary)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)

                    if index < categoryOptions.count - 1 {
                        Divider().overlay(WornColors.borderSubtle.opacity(0.5))
                    }
                }
            }
        }
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(WornColors.borderSubtle, lineWidth: 1)
        )
        .accessibilityIdentifier("add_item_category_dropdown")
    }

    private var colorSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: "label_color"))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 7), spacing: 12) {
                ForEach(colorPalette, id: \.name) { item in
                    let isSelected = selectedColors.contains(item.name)
                    Button {
                        if isSelected {
                            selectedColors.remove(item.name)
                        } else {
                            selectedColors.insert(item.name)
                        }
                    } label: {
                        ZStack {
                            Circle()
                                .fill(item.color)
                                .frame(width: 28, height: 28)
                                .overlay(
                                    Circle()
                                        .stroke(isSelected ? WornColors.accentGreen : Color.clear, lineWidth: 2)
                                )
                            if isSelected {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(item.color.isBright ? .black : .white)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var seasonSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: "label_season"))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                ForEach(seasonOptions, id: \.0) { season, label in
                    let isActive = selectedSeasons.contains(season)
                    WornChip(label: label, isActive: isActive) {
                        if isActive {
                            selectedSeasons.remove(season)
                        } else {
                            selectedSeasons.insert(season)
                        }
                    }
                }
            }
        }
    }

    private var saveButton: some View {
        WornGradientButton(
            text: isSaving ? String(localized: "common_saving") : (isEditing ? String(localized: "common_save_changes") : String(localized: "add_item_save_to_wardrobe")),
            action: {
                guard let cat = selectedCategory else { return }
                let data = photoData ?? Data()
                onSave(data, name, cat, Array(selectedColors), Array(selectedSeasons),
                       selectedSubcategory, selectedFit, selectedMaterial)
            },
            enabled: canSave,
            shadowRadius: 12,
            shadowColor: WornColors.saveGradientStart.opacity(0.2),
            shadowY: 8
        )
        .accessibilityIdentifier("add_item_save_button")
    }

    private var categoryOptions: [(Category, String)] {
        [
            (.top, String(localized: "category_tops")), (.bottom, String(localized: "category_bottoms")),
            (.outerwear, String(localized: "category_outerwear")), (.shoes, String(localized: "category_shoes")), (.accessory, String(localized: "category_accessories")),
        ]
    }

    private var seasonOptions: [(Season, String)] {
        [(.spring, String(localized: "season_spring")), (.summer, String(localized: "season_summer")), (.fall, String(localized: "season_fall")), (.winter, String(localized: "season_winter"))]
    }

    private func iconName(for category: Category) -> String {
        switch category {
        case .top: return "tshirt"
        case .bottom: return "ruler"
        case .outerwear: return "wind"
        case .shoes: return "shoe"
        case .accessory: return "eyeglasses"
        default: return "questionmark"
        }
    }

    private func displayName(for category: Category) -> String {
        switch category {
        case .top: return String(localized: "category_tops")
        case .bottom: return String(localized: "category_bottoms")
        case .outerwear: return String(localized: "category_outerwear")
        case .shoes: return String(localized: "category_shoes")
        case .accessory: return String(localized: "category_accessories")
        default: return ""
        }
    }

    // MARK: - Subcategory

    @State private var subcategoryExpanded = false

    private var subcategoryOptions: [(Subcategory, String)] {
        guard let cat = selectedCategory else { return [] }
        return SubcategoryKt.subcategoriesFor(category: cat).map { sub in
            (sub, localizedSubcategoryName(sub))
        }
    }

    private func localizedSubcategoryName(_ subcategory: Subcategory) -> String {
        let key = "subcategory_\(subcategory.name.lowercased())"
        return String(localized: String.LocalizationValue(key))
    }

    private var subcategoryField: some View {
        VStack(spacing: 0) {
            Button { withAnimation { subcategoryExpanded.toggle() } } label: {
                HStack {
                    Text(selectedSubcategory.map {
                        localizedSubcategoryName($0)
                    } ?? String(localized: "label_subcategory"))
                        .font(.system(size: 15))
                        .foregroundColor(selectedSubcategory != nil ? WornColors.textPrimary : WornColors.iconMuted)
                    Spacer()
                    Image(systemName: subcategoryExpanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 14))
                        .foregroundColor(WornColors.iconMuted)
                }
                .padding(16)
            }
            .buttonStyle(.plain)

            if subcategoryExpanded {
                Divider().overlay(WornColors.borderSubtle)
                ForEach(Array(subcategoryOptions.enumerated()), id: \.offset) { index, item in
                    let (subcategory, label) = item
                    Button {
                        selectedSubcategory = subcategory
                        withAnimation { subcategoryExpanded = false }
                    } label: {
                        Text(label)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(WornColors.textPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)

                    if index < subcategoryOptions.count - 1 {
                        Divider().overlay(WornColors.borderSubtle.opacity(0.5))
                    }
                }
            }
        }
        .background(WornColors.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(WornColors.borderSubtle, lineWidth: 1)
        )
    }

    // MARK: - Fit

    private var fitOptions: [(Fit, String)] {
        [
            (.slimFit, String(localized: "fit_slim")), (.regular, String(localized: "fit_regular")),
            (.relaxed, String(localized: "fit_relaxed")), (.oversized, String(localized: "fit_oversized")),
        ]
    }

    private var fitSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: "label_fit"))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                ForEach(fitOptions, id: \.0) { fit, label in
                    let isActive = selectedFit == fit
                    WornChip(label: label, isActive: isActive) {
                        selectedFit = isActive ? nil : fit
                    }
                }
            }
        }
    }

    // MARK: - Material

    private var materialOptions: [(Material, String)] {
        [
            (.cotton, String(localized: "material_cotton")), (.linen, String(localized: "material_linen")),
            (.denim, String(localized: "material_denim")), (.wool, String(localized: "material_wool")),
            (.synthetic, String(localized: "material_synthetic")), (.leather, String(localized: "material_leather")),
            (.silk, String(localized: "material_silk")), (.knit, String(localized: "material_knit")),
        ]
    }

    private var materialSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: "label_material"))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(materialOptions, id: \.0) { material, label in
                    let isActive = selectedMaterial == material
                    WornChip(label: label, isActive: isActive) {
                        selectedMaterial = isActive ? nil : material
                    }
                }
            }
        }
    }
}

#Preview("iPhone") {
    AddItemSheet(isSaving: false, hasApiKey: false, onSave: { _, _, _, _, _, _, _, _ in }, onDismiss: {})
}

#Preview("iPad Portrait") {
    AddItemSheet(isSaving: false, hasApiKey: false, onSave: { _, _, _, _, _, _, _, _ in }, onDismiss: {})
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
