import UIKit

/// How the app encodes a photo it is about to store.
///
/// Stored photos keep their capture quality; the reduction for AI and API requests happens in the
/// shared `ImageDownscaler` on the request path, not here.
enum PhotoEncoding {

    /// High enough that a re-encode is visually lossless, which matters because a photo can be
    /// re-encoded more than once — capture, then crop, then background removal.
    static let storageJpegQuality: CGFloat = 0.95

    /// `UIImagePickerController` hands back a `UIImage` rather than file data, so a camera capture
    /// is the one place the app has to encode a stored photo itself.
    static func jpegForStorage(_ image: UIImage) -> Data? {
        image.jpegData(compressionQuality: storageJpegQuality)
    }
}
