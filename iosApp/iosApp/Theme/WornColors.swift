import SwiftUI
import Shared

/// Worn's palette, mirroring `WornColorScheme.kt` on Android value for value.
///
/// Every token is built from a light/dark pair through `UIColor(dynamicProvider:)`, so SwiftUI
/// resolves it against the current trait collection and the whole app follows the system
/// appearance with no `@Environment(\.colorScheme)` checks at any call site.
///
/// Light keeps the established warm beige + sage brand unchanged. Dark sits on a warm #211D18
/// base rather than a neutral near-black — the latter reads cold against the sage and drops the
/// brand's warmth — with the ramp spaced widely enough that page, card and bar actually separate.
enum WornColors {
    // Backgrounds
    static let bgPage = Color(light: "F5F0EB", dark: "211D18")
    static let bgCard = Color(light: "FFFFFF", dark: "1A1713")
    static let bgElevated = Color(light: "EDE8E1", dark: "363029")

    // Borders
    static let borderSubtle = Color(light: "E0D9D0", dark: "554E45")
    static let borderStrong = Color(light: "C8C0B5", dark: "A0988B")

    // Accents
    static let accentGreen = Color(light: "7A9468", dark: "A8C295")
    static let accentIndigo = Color(light: "6B7B8E", dark: "A9BACE")
    static let accentCoral = Color(light: "A87560", dark: "D8A88F")
    static let deleteRed = Color(light: "C45B4A", dark: "F2B8AC")

    /// Banner and gradient-end fills that always carry white text, so dark in *both* appearances.
    static let accentGreenDark = Color(light: "5C6E50", dark: "4E6641")

    // Gradient stops.
    //
    // Deliberately not derived from `accentGreen` / `accentIndigo`: those invert between
    // appearances (sage goes from #7A9468 to a much lighter #A8C295) and the gradient buttons
    // always draw white label text, so deriving them would silently drop the label to about
    // 1.8:1 in dark. These stay saturated enough for white either way.
    static let saveGradientStart = Color(light: "8FA47D", dark: "6E8A5C")
    static let saveGradientEnd = Color(light: "6B7F5E", dark: "546B45")
    static let greenCtaStart = Color(light: "7A9468", dark: "6E8A5C")
    static let greenCtaEnd = Color(light: "6B8A58", dark: "5C7A4B")
    static let indigoGradientStart = Color(light: "6B7B8E", dark: "5E6E80")
    static let indigoGradientEnd = Color(light: "556070", dark: "4A5462")

    // Text
    static let textPrimary = Color(light: "2C2924", dark: "EFE8DD")
    static let textSecondary = Color(light: "7D776F", dark: "D2C9BC")
    static let textMuted = Color(light: "B5AFA8", dark: "8A8378")
    /// Label colour on top of a filled accent — white in both appearances by design.
    static let textOnColor = Color.white

    // Icons
    static let iconMuted = Color(light: "A09A92", dark: "9B9488")

    // Category dots.
    //
    // These do double duty: small dots on the page surface, and 36pt tile fills behind *white*
    // icons in Gaps. So they can only be lifted far enough to stay visible on #211D18, not so far
    // that white stops reading on top. Only the two darkest needed moving for dark.
    static let categoryDotTop = Color(light: "444444", dark: "6E6862")
    static let categoryDotBottom = Color(light: "2B4570", dark: "42618F")
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
        self.init(uiColor: UIColor(hex: hex))
    }

    /// Builds a colour that resolves per appearance, so it tracks the system theme automatically.
    init(light: String, dark: String) {
        self.init(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
        })
    }

    var isBright: Bool {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: nil)
        let brightness = r * 0.299 + g * 0.587 + b * 0.114
        return brightness > 0.5
    }
}

extension UIColor {
    convenience init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
