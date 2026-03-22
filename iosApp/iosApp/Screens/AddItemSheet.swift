import SwiftUI
import PhotosUI
import Shared

struct AddItemSheet: View {
    let isSaving: Bool
    let onSave: (Data, String, Category, [String], [Season]) -> Void
    let onDismiss: () -> Void

    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var photoData: Data?
    @State private var photoImage: UIImage?
    @State private var name = ""
    @State private var selectedCategory: Category?
    @State private var selectedColors: Set<String> = []
    @State private var selectedSeasons: Set<Season> = []

    private let colorPalette: [(name: String, color: Color)] = [
        ("Cream", Color(hex: "EDE8E1")),
        ("Black", Color(hex: "2C2924")),
        ("Navy", Color(hex: "2B4570")),
        ("Grey", Color(hex: "808080")),
        ("Olive", Color(hex: "6B7B3F")),
        ("Beige", Color(hex: "C4A882")),
        ("Brown", Color(hex: "8B4513")),
        ("Coral", Color(hex: "A87560")),
    ]

    private var canSave: Bool {
        photoData != nil && !name.isEmpty && selectedCategory != nil && !isSaving
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    photoUploadZone
                    aiBadge
                    nameField
                    categoryField
                    colorSection
                    seasonSection
                    saveButton
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
            .background(WornColors.bgElevated)
            .navigationTitle("Add new item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
            }
        }
    }

    private var photoUploadZone: some View {
        PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
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
        .onChange(of: selectedPhotoItem) { _, newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self) {
                    photoData = data
                    photoImage = UIImage(data: data)
                }
            }
        }
    }

    private var aiBadge: some View {
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
        .background(Color(hex: "6B7B8E"))
        .clipShape(Capsule())
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

    private var categoryField: some View {
        Menu {
            ForEach(categoryOptions, id: \.0) { category, label in
                Button(label) { selectedCategory = category }
            }
        } label: {
            HStack {
                Text(selectedCategory.map { displayName(for: $0) } ?? "Category")
                    .font(.system(size: 15))
                    .foregroundColor(selectedCategory != nil ? WornColors.textPrimary : WornColors.iconMuted)
                Spacer()
                Image(systemName: "chevron.down")
                    .font(.system(size: 14))
                    .foregroundColor(WornColors.iconMuted)
            }
            .padding(16)
            .background(WornColors.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(WornColors.borderSubtle, lineWidth: 1)
            )
        }
    }

    private var colorSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Color")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WornColors.textPrimary)

            HStack(spacing: 12) {
                ForEach(colorPalette, id: \.name) { item in
                    let isSelected = selectedColors.contains(item.name)
                    Button {
                        if isSelected {
                            selectedColors.remove(item.name)
                        } else {
                            selectedColors.insert(item.name)
                        }
                    } label: {
                        Circle()
                            .fill(item.color)
                            .frame(width: 28, height: 28)
                            .overlay(
                                Circle()
                                    .stroke(isSelected ? WornColors.accentGreen : Color.clear, lineWidth: 2)
                            )
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
            guard let data = photoData, let cat = selectedCategory else { return }
            let bytes = [UInt8](data)
            let kotlinByteArray = KotlinByteArray(size: Int32(bytes.count))
            for (index, byte) in bytes.enumerated() {
                kotlinByteArray.set(index: Int32(index), value: Int8(bitPattern: byte))
            }
            onSave(
                data,
                name,
                cat,
                Array(selectedColors),
                Array(selectedSeasons)
            )
        } label: {
            Text(isSaving ? "Saving…" : "Save to wardrobe")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    LinearGradient(
                        colors: canSave
                            ? [Color(hex: "8FA47D"), Color(hex: "6B7F5E")]
                            : [WornColors.textMuted, WornColors.iconMuted],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: Color(hex: "8FA47D").opacity(0.2), radius: 12, x: 0, y: 8)
        }
        .disabled(!canSave)
    }

    private var categoryOptions: [(Category, String)] {
        [
            (.top, "Tops"),
            (.bottom, "Bottoms"),
            (.dress, "Dresses"),
            (.outerwear, "Outerwear"),
            (.shoes, "Shoes"),
            (.accessory, "Accessories"),
        ]
    }

    private var seasonOptions: [(Season, String)] {
        [
            (.spring, "Spring"),
            (.summer, "Summer"),
            (.fall, "Fall"),
            (.winter, "Winter"),
        ]
    }

    private func displayName(for category: Category) -> String {
        switch category {
        case .top: return "Tops"
        case .bottom: return "Bottoms"
        case .dress: return "Dresses"
        case .outerwear: return "Outerwear"
        case .shoes: return "Shoes"
        case .accessory: return "Accessories"
        default: return ""
        }
    }
}

#Preview("iPhone") {
    AddItemSheet(isSaving: false, onSave: { _, _, _, _, _ in }, onDismiss: {})
}

#Preview("iPad Portrait") {
    AddItemSheet(isSaving: false, onSave: { _, _, _, _, _ in }, onDismiss: {})
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
