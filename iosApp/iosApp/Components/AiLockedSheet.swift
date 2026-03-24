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

            Text("Unlock AI features")
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(WornColors.textPrimary)

            Text("Add your Claude API key in Settings to enable this.")
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .lineSpacing(7)
                .frame(maxWidth: 280)

            Button {
                onGoToSettings()
                onDismiss()
            } label: {
                Text("Go to Settings")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 40)
                    .padding(.vertical, 14)
                    .background(
                        LinearGradient(
                            colors: [WornColors.accentGreen, WornColors.accentGreenDark],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: WornColors.accentGreen.opacity(0.2), radius: 6, x: 0, y: 4)
            }
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
