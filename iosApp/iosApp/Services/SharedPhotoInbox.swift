import Foundation

/// One photo handed to the app from the share extension. `data` is nil when the handoff file
/// could not be read.
///
/// The `id` makes every share distinct, so `.onChange` fires even when the user shares the same
/// photo twice in a row.
struct SharedPhoto: Equatable {
    let id = UUID()
    let data: Data?
}

/// Picks up photos parked in the App Group container by `WornShareExtension`.
enum SharedPhotoInbox {
    private static let appGroup = "group.com.github.worn"
    private static let fileName = "pending_share.jpg"

    private static var handoffURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroup)?
            .appendingPathComponent(fileName)
    }

    /// Consumes the pending photo, if there is one. Returns nil when nothing was shared, so a
    /// plain foreground does not disturb the current screen.
    ///
    /// The file is removed either way: leaving an unreadable one behind would re-trigger the
    /// failure on every foreground.
    static func consume() -> SharedPhoto? {
        guard let url = handoffURL, FileManager.default.fileExists(atPath: url.path) else {
            return nil
        }
        let data = try? Data(contentsOf: url)
        try? FileManager.default.removeItem(at: url)
        return SharedPhoto(data: data)
    }
}
