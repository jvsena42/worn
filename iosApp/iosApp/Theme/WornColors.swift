import SwiftUI

enum WornColors {
    // Backgrounds
    static let bgPage = Color(hex: "F5F0EB")
    static let bgCard = Color.white
    static let bgElevated = Color(hex: "EDE8E1")

    // Borders
    static let borderSubtle = Color(hex: "E0D9D0")
    static let borderStrong = Color(hex: "C8C0B5")

    // Accents
    static let accentGreen = Color(hex: "7A9468")
    static let accentGreenEnd = Color(hex: "6B8A58")
    static let accentGreenDark = Color(hex: "5C6E50")
    static let accentIndigo = Color(hex: "6B7B8E")
    static let accentCoral = Color(hex: "A87560")
    static let deleteRed = Color(hex: "C45B4A")

    // Gradients
    static let saveGradientStart = Color(hex: "8FA47D")
    static let saveGradientEnd = Color(hex: "6B7F5E")

    // Text
    static let textPrimary = Color(hex: "2C2924")
    static let textSecondary = Color(hex: "7D776F")
    static let textMuted = Color(hex: "B5AFA8")
    static let textOnColor = Color.white

    // Icons
    static let iconMuted = Color(hex: "A09A92")

    // Category dots
    static let categoryDotTop = Color(hex: "444444")
    static let categoryDotBottom = Color(hex: "2B4570")
    static let categoryDotDress = Color(hex: "A87560")
    static let categoryDotOuterwear = Color(hex: "7A9468")
    static let categoryDotShoes = Color(hex: "8B6914")
    static let categoryDotAccessory = Color(hex: "B59D6E")
}

// MARK: - KMP model Identifiable conformance

extension ClothingItem: @retroactive Identifiable {}
extension Outfit: @retroactive Identifiable {}

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

    var isBright: Bool {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: nil)
        let brightness = r * 0.299 + g * 0.587 + b * 0.114
        return brightness > 0.5
    }
}
