import SwiftUI

struct ErrorContentView: View {
    let message: String
    let onRetry: () -> Void
    var retryButtonColor: Color = WornColors.accentGreen

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(WornColors.deleteRed.opacity(0.1))
                    .frame(width: 72, height: 72)
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 32))
                    .foregroundColor(WornColors.deleteRed)
            }
            .frame(maxWidth: .infinity)

            Text(message)
                .font(.system(size: 14))
                .foregroundColor(WornColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 24)

            Button(action: onRetry) {
                Text(String(localized: "common_retry"))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(retryButtonColor)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(WornColors.bgCard)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .padding(.top, 20)
        }
    }
}

#Preview("iPhone") {
    ErrorContentView(
        message: "Invalid API key. Check your key in Settings.",
        onRetry: {}
    )
    .padding(.vertical, 60)
}

#Preview("iPad Portrait", traits: .portrait) {
    ErrorContentView(
        message: "Something went wrong. Please try again.",
        onRetry: {},
        retryButtonColor: WornColors.accentIndigo
    )
    .padding(.vertical, 60)
}
