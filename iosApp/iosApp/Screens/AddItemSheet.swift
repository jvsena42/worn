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
    @State private var photoImage: UIImage?
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
            .navigationTitle(isEditing ? "Edit item" : "Add new item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
            }
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
                        photoImage = UIImage(data: data)
                    }
                }
            }
        }
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
                        Text("Tap to add photo")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(WornColors.textSecondary)
                    }
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
    }

    private var aiBadge: some View {
        Button {
            if !hasApiKey { showAiLockedSheet = true }
        } label: {
            HStack(spacing: 6) {
                Text("✦")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                Text("Auto-tag with AI")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(WornColors.accentIndigo)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $showAiLockedSheet) {
            AiLockedSheet(onDismiss: { showAiLockedSheet = false })
                .presentationDetents([.medium])
        }
    }

    private var nameField: some View {
        TextField("Item name", text: $name)
            .font(.system(size: 15))
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
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
                    Text(selectedCategory.map { displayName(for: $0) } ?? "Category")
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
    }

    private var colorSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Color")
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
            Text("Season")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                ForEach(seasonOptions, id: \.0) { season, label in
                    let isActive = selectedSeasons.contains(season)
                    Button {
                        if isActive {
                            selectedSeasons.remove(season)
                        } else {
                            selectedSeasons.insert(season)
                        }
                    } label: {
                        Text(label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var saveButton: some View {
        Button {
            guard let cat = selectedCategory else { return }
            let data = photoData ?? Data()
            onSave(data, name, cat, Array(selectedColors), Array(selectedSeasons),
                   selectedSubcategory, selectedFit, selectedMaterial)
        } label: {
            Text(isSaving ? "Saving…" : (isEditing ? "Save Changes" : "Save to wardrobe"))
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    LinearGradient(
                        colors: canSave
                            ? [WornColors.saveGradientStart, WornColors.saveGradientEnd]
                            : [WornColors.textMuted, WornColors.iconMuted],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: WornColors.saveGradientStart.opacity(0.2), radius: 12, x: 0, y: 8)
        }
        .disabled(!canSave)
    }

    private var categoryOptions: [(Category, String)] {
        [
            (.top, "Tops"), (.bottom, "Bottoms"),
            (.outerwear, "Outerwear"), (.shoes, "Shoes"), (.accessory, "Accessories"),
        ]
    }

    private var seasonOptions: [(Season, String)] {
        [(.spring, "Spring"), (.summer, "Summer"), (.fall, "Fall"), (.winter, "Winter")]
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
        case .top: return "Tops"
        case .bottom: return "Bottoms"
        case .outerwear: return "Outerwear"
        case .shoes: return "Shoes"
        case .accessory: return "Accessories"
        default: return ""
        }
    }

    // MARK: - Subcategory

    @State private var subcategoryExpanded = false

    private var subcategoryOptions: [(Subcategory, String)] {
        guard let cat = selectedCategory else { return [] }
        return SubcategoryKt.subcategoriesFor(category: cat).map { sub in
            (sub, sub.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized)
        }
    }

    private var subcategoryField: some View {
        VStack(spacing: 0) {
            Button { withAnimation { subcategoryExpanded.toggle() } } label: {
                HStack {
                    Text(selectedSubcategory.map {
                        $0.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
                    } ?? "Subcategory")
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

    private let fitOptions: [(Fit, String)] = [
        (.slimFit, "Slim Fit"), (.regular, "Regular"), (.relaxed, "Relaxed"), (.oversized, "Oversized"),
    ]

    private var fitSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Fit")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 8) {
                ForEach(fitOptions, id: \.0) { fit, label in
                    let isActive = selectedFit == fit
                    Button {
                        selectedFit = isActive ? nil : fit
                    } label: {
                        Text(label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Material

    private let materialOptions: [(Material, String)] = [
        (.cotton, "Cotton"), (.linen, "Linen"), (.denim, "Denim"), (.wool, "Wool"),
        (.synthetic, "Synthetic"), (.leather, "Leather"), (.silk, "Silk"), (.knit, "Knit"),
    ]

    private var materialSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Material")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(materialOptions, id: \.0) { material, label in
                    let isActive = selectedMaterial == material
                    Button {
                        selectedMaterial = isActive ? nil : material
                    } label: {
                        Text(label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(isActive ? WornColors.textOnColor : WornColors.textSecondary)
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity)
                            .background(isActive ? WornColors.accentGreen : WornColors.bgCard)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(isActive ? Color.clear : WornColors.borderSubtle, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
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
