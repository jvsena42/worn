import SwiftUI

struct SelectionHeader: View {
    let count: Int
    var onCancel: () -> Void = {}
    var onDelete: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(String(format: String(localized: "selected_count"), count))
                    .font(.system(size: 28, weight: .medium))
                    .tracking(-0.8)
                    .foregroundColor(WornColors.textPrimary)
                Spacer()
                Button {
                    onDelete()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "trash")
                            .font(.system(size: 15))
                        Text(String(localized: "common_delete"))
                            .font(.system(size: 15, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(WornColors.deleteRed)
                    .clipShape(Capsule())
                }
            }
            Button(String(localized: "common_cancel")) { onCancel() }
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
        }
    }
}

#Preview("iPhone") {
    SelectionHeader(count: 3)
        .padding()
}

#Preview("iPad", traits: .landscapeLeft) {
    SelectionHeader(count: 5)
        .padding()
}
