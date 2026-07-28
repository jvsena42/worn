import SwiftUI

struct PropertyRow: View {
    let label: String
    let value: String
    var fontSize: CGFloat = 15

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: fontSize, weight: .medium))
                .foregroundColor(WornColors.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: fontSize, weight: .medium))
                .foregroundColor(WornColors.textPrimary)
        }
    }
}

#Preview("iPhone") {
    VStack(spacing: 12) {
        PropertyRow(label: "Season", value: "Summer")
        PropertyRow(label: "Fit", value: "Regular")
    }
    .padding()
}

#Preview("iPad Portrait", traits: .portrait) {
    VStack(spacing: 12) {
        PropertyRow(label: "Season", value: "Summer")
        PropertyRow(label: "Fit", value: "Regular")
    }
    .padding()
}
