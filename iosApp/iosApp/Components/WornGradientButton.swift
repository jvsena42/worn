import SwiftUI

enum WornGradients {
    static let save = [WornColors.saveGradientStart, WornColors.saveGradientEnd]
    static let green = [WornColors.accentGreen, WornColors.accentGreenDark]
    static let greenCta = [WornColors.accentGreen, WornColors.accentGreenEnd]
    static let indigo = [WornColors.accentIndigo, Color(hex: 0x556070)]
    static let disabled = [WornColors.textMuted, WornColors.iconMuted]
}

struct WornGradientButton: View {
    let text: String
    let action: () -> Void
    var enabled: Bool = true
    var gradientColors: [Color] = WornGradients.save
    var disabledGradientColors: [Color] = WornGradients.disabled
    var cornerRadius: CGFloat = 16
    var shadowRadius: CGFloat = 0
    var shadowColor: Color = Color.clear
    var shadowY: CGFloat = 0
    var icon: AnyView? = nil
    var fillMaxWidth: Bool = true
    var fixedHeight: CGFloat? = 52
    var contentPadding: EdgeInsets? = nil

    var body: some View {
        Button(action: action) {
            Group {
                if let icon = icon {
                    HStack(spacing: 8) {
                        icon
                        buttonText
                    }
                } else {
                    buttonText
                }
            }
            .frame(maxWidth: fillMaxWidth ? .infinity : nil)
            .frame(height: fixedHeight)
            .padding(contentPadding ?? EdgeInsets())
            .background(
                LinearGradient(
                    colors: enabled ? gradientColors : disabledGradientColors,
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .shadow(color: shadowColor, radius: shadowRadius, x: 0, y: shadowY)
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }

    private var buttonText: some View {
        Text(text)
            .font(.system(size: 16, weight: .semibold))
            .foregroundColor(.white)
    }
}

private extension Color {
    init(hex: UInt) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0
        )
    }
}

#Preview("iPhone") {
    VStack(spacing: 16) {
        WornGradientButton(text: "Save to Wardrobe", action: {})
        WornGradientButton(text: "Save to Wardrobe", action: {}, enabled: false)
        WornGradientButton(
            text: "Analyze",
            action: {},
            gradientColors: WornGradients.indigo,
            cornerRadius: 28,
            shadowRadius: 10,
            shadowColor: WornColors.accentIndigo.opacity(0.15),
            shadowY: 6
        )
    }
    .padding()
}

#Preview("iPad", traits: .landscapeLeft) {
    VStack(spacing: 16) {
        WornGradientButton(text: "Save to Wardrobe", action: {})
    }
    .padding()
}
