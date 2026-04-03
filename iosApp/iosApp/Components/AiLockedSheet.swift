import SwiftUI

struct AiLockedSheet: View {
    let onDismiss: () -> Void
    var onGoToSettings: () -> Void = {}

    var body: some View {
        VStack(spacing: 20) {
            HStack {
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16))
                        .foregroundColor(WornColors.iconMuted)
                }
                .buttonStyle(.plain)
            }

            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(WornColors.accentIndigo)
                    .frame(width: 44, height: 44)
                Image(systemName: "cpu")
                    .font(.system(size: 20))
                    .foregroundColor(.white)
            }

            Text(String(localized: "ai_locked_title"))
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(WornColors.textPrimary)

            Text(String(localized: "ai_locked_description"))
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(7)
                .frame(maxWidth: 280)

            WornGradientButton(
                text: String(localized: "ai_locked_cta"),
                action: {
                    onGoToSettings()
                    onDismiss()
                },
                gradientColors: WornGradients.green,
                shadowRadius: 6,
                shadowColor: WornColors.accentGreen.opacity(0.2),
                shadowY: 4,
                fillMaxWidth: false,
                fixedHeight: nil,
                contentPadding: EdgeInsets(top: 14, leading: 40, bottom: 14, trailing: 40)
            )
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .padding(.bottom, 32)
        .background(WornColors.bgElevated)
    }
}

#Preview("iPhone") {
    AiLockedSheet(onDismiss: {})
}

#Preview("iPad Portrait") {
    AiLockedSheet(onDismiss: {})
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
