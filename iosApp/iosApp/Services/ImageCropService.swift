import UIKit
import Shared

/// Pixel work behind the crop editor. The geometry itself lives in the shared `CropGeometry`;
/// only decoding, orientation handling and encoding are platform-specific.
enum ImageCropService {

    private static let jpegQuality: CGFloat = 0.9

    /// Returns an `.up`-oriented, scale-1 image whose long edge is at most `maxEdge`.
    ///
    /// Both properties matter. `CGImage.cropping(to:)` works on the raw pixel buffer and ignores
    /// `UIImage.imageOrientation`, so a camera photo (usually `.right`) would otherwise be cropped
    /// on the wrong axis. Drawing through `UIImage.draw(in:)` bakes the orientation in. Forcing
    /// `scale = 1` makes points equal pixels, so the view-space geometry maps onto the pixel buffer
    /// with a single scale factor. Downsampling keeps a 12MP photo from staying resident for the
    /// length of a drag.
    static func prepareForEditing(_ data: Data, maxEdge: CGFloat = 2048) -> UIImage? {
        guard let source = UIImage(data: data) else { return nil }
        let pixelWidth = source.size.width * source.scale
        let pixelHeight = source.size.height * source.scale
        guard pixelWidth > 0, pixelHeight > 0 else { return nil }

        let factor = min(1, maxEdge / max(pixelWidth, pixelHeight))
        let target = CGSize(
            width: max(1, (pixelWidth * factor).rounded()),
            height: max(1, (pixelHeight * factor).rounded())
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            source.draw(in: CGRect(origin: .zero, size: target))
        }
    }

    /// Crops an image produced by `prepareForEditing` to `rect` and re-encodes it as JPEG,
    /// matching the quality used everywhere else in the photo pipeline.
    static func crop(_ image: UIImage, to rect: CropRect) -> Data? {
        guard let cgImage = image.cgImage else { return nil }
        let full = CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height)
        // Defensive: the shared geometry already guarantees containment.
        let target = CGRect(
            x: CGFloat(rect.left),
            y: CGFloat(rect.top),
            width: CGFloat(rect.width),
            height: CGFloat(rect.height)
        ).intersection(full)

        guard !target.isNull, target.width >= 1, target.height >= 1,
              let cropped = cgImage.cropping(to: target) else { return nil }

        return UIImage(cgImage: cropped, scale: 1, orientation: .up)
            .jpegData(compressionQuality: jpegQuality)
    }
}
