import Foundation

/// Which full-screen photo modal a photo flow is currently presenting.
///
/// SwiftUI does not reliably support two `.fullScreenCover(isPresented:)` modifiers attached to the
/// same view, so each flow drives a single `.fullScreenCover(item:)` from one of these instead of
/// stacking separate boolean flags.
enum PhotoCover: Int, Identifiable {
    case camera
    case crop

    var id: Int { rawValue }
}
