import SwiftUI

/// Worn's corner-radius scale, mirroring `WornShapes.kt` step for step.
///
/// The radii are the ones already scattered across the UI as `cornerRadius:` literals, collapsed
/// onto one named scale. The handful of near-duplicate one-offs (10/22/26/30) fold into the
/// nearest step, so the app stops shipping four radii that differ by 2pt and read as the same
/// curve.
enum WornShape {
    static let extraSmall: CGFloat = 4
    static let small: CGFloat = 8
    /// Chips, small tiles, input fields.
    static let medium: CGFloat = 12
    /// Cards and photo frames.
    static let large: CGFloat = 16
    static let largeIncreased: CGFloat = 20
    /// Sheets and dialogs.
    static let extraLarge: CGFloat = 24
    /// Pill buttons and the FAB.
    static let extraLargeIncreased: CGFloat = 28
    /// The bottom bar.
    static let extraExtraLarge: CGFloat = 36
}
