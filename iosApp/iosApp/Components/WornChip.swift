import SwiftUI

struct WornChip: View {
    let label: String
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
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

#Preview("iPhone") {
    HStack(spacing: 8) {
        WornChip(label: "Summer", isActive: false, onTap: {})
        WornChip(label: "Winter", isActive: true, onTap: {})
    }
    .padding()
}

#Preview("iPad Portrait", traits: .portrait) {
    HStack(spacing: 8) {
        WornChip(label: "Summer", isActive: false, onTap: {})
        WornChip(label: "Winter", isActive: true, onTap: {})
    }
    .padding()
}
