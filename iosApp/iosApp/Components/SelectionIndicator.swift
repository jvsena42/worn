import SwiftUI

struct SelectionIndicator: View {
    let isSelected: Bool
    var size: CGFloat = 28
    var iconSize: CGFloat = 12

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size / 2)
                .fill(isSelected ? WornColors.accentGreen : WornColors.bgCard)
                .frame(width: size, height: size)
                .overlay(
                    RoundedRectangle(cornerRadius: size / 2)
                        .stroke(
                            isSelected ? Color.clear : WornColors.borderSubtle,
                            lineWidth: 1.5
                        )
                )
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.system(size: iconSize, weight: .bold))
                    .foregroundColor(.white)
            }
        }
    }
}

#Preview("iPhone") {
    HStack(spacing: 16) {
        SelectionIndicator(isSelected: false)
        SelectionIndicator(isSelected: true)
        SelectionIndicator(isSelected: false, size: 20, iconSize: 10)
        SelectionIndicator(isSelected: true, size: 20, iconSize: 10)
    }
    .padding()
}

#Preview("iPad Portrait", traits: .portrait) {
    HStack(spacing: 16) {
        SelectionIndicator(isSelected: false)
        SelectionIndicator(isSelected: true)
    }
    .padding()
}
