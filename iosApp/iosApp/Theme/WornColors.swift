import SwiftUI

enum WornColors {
    static let bgPage = Color(hex: "F5F0EB")
    static let bgCard = Color.white
    static let bgElevated = Color(hex: "EDE8E1")
    static let borderSubtle = Color(hex: "E0D9D0")
    static let borderStrong = Color(hex: "C8C0B5")
    static let accentGreen = Color(hex: "7A9468")
    static let textPrimary = Color(hex: "2C2924")
    static let textSecondary = Color(hex: "7D776F")
    static let textMuted = Color(hex: "B5AFA8")
    static let textOnColor = Color.white
    static let iconMuted = Color(hex: "A09A92")
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }
}
