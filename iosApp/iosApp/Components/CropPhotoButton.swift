import SwiftUI

/// Opens the crop editor for the photo shown above it.
///
/// Rendered under a photo preview zone rather than overlaid on it: every preview zone is itself a
/// `Button`, and SwiftUI does not deliver taps to a `Button` nested inside another button's label.
struct CropPhotoButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "crop")
                    .font(.system(size: 15))
                    .foregroundColor(WornColors.textSecondary)
                Text(String(localized: "crop_photo_button"))
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(WornColors.textPrimary)
            }
        }
        .buttonStyle(.plain)
    }
}

#Preview("iPhone") {
    CropPhotoButton(action: {})
}

#Preview("iPad Portrait", traits: .portrait) {
    CropPhotoButton(action: {})
}
