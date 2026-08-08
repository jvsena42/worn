import UIKit
import Shared

/// Pixel work behind the crop editor. The geometry itself lives in the shared `CropGeometry`;
/// only decoding, orientation handling and encoding are platform-specific.
enum ImageCropService {

    private static let previewMaxEdge: CGFloat = 2048

    /// Returns a downsampled image for the editor to display and drag against.
    ///
    /// This is a *preview*: never crop or save what it returns — `crop(_:selection:bounds:)`
    /// re-reads the original so the saved photo keeps its capture resolution. Downsampling keeps a
    /// 12MP photo from staying resident for the length of a drag.
    static func prepareForEditing(_ data: Data, maxEdge: CGFloat = previewMaxEdge) -> UIImage? {
        upright(data, maxEdge: maxEdge)
    }

    /// Crops the original `data` to the part of `bounds` covered by `selection`, re-encoding the
    /// result as JPEG at storage quality. Returns nil if the photo cannot be decoded or the
    /// selection maps to nothing.
    ///
    /// Takes the encoded data rather than the preview image so the crop lands on full-resolution
    /// pixels, and maps the selection against the full-size buffer's own dimensions, which removes
    /// any chance of the rect being measured against one image and applied to another.
    static func crop(_ data: Data, selection: CropViewRect, bounds: CropViewRect) -> Data? {
        guard let source = upright(data, maxEdge: nil), let cgImage = source.cgImage else { return nil }

        let rect = CropGeometry.shared.toSourceRect(
            selection: selection,
            bounds: bounds,
            imageWidth: Int32(cgImage.width),
            imageHeight: Int32(cgImage.height)
        )
        // Defensive: the shared geometry already guarantees containment.
        let full = CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height)
        let target = CGRect(
            x: CGFloat(rect.left),
            y: CGFloat(rect.top),
            width: CGFloat(rect.width),
            height: CGFloat(rect.height)
        ).intersection(full)

        guard !target.isNull, target.width >= 1, target.height >= 1,
              let cropped = cgImage.cropping(to: target) else { return nil }

        return PhotoEncoding.jpegForStorage(
            UIImage(cgImage: cropped, scale: 1, orientation: .up)
        )
    }

    /// Redraws `data` `.up`-oriented at scale 1, optionally capping the long edge at `maxEdge`.
    ///
    /// Both properties matter. `CGImage.cropping(to:)` works on the raw pixel buffer and ignores
    /// `UIImage.imageOrientation`, so a camera photo (usually `.right`) would otherwise be cropped
    /// on the wrong axis. Drawing through `UIImage.draw(in:)` bakes the orientation in. Forcing
    /// `scale = 1` makes points equal pixels, so the view-space geometry maps onto the pixel buffer
    /// with a single scale factor.
    private static func upright(_ data: Data, maxEdge: CGFloat?) -> UIImage? {
        guard let source = UIImage(data: data) else { return nil }
        let pixelWidth = source.size.width * source.scale
        let pixelHeight = source.size.height * source.scale
        guard pixelWidth > 0, pixelHeight > 0 else { return nil }

        let factor = maxEdge.map { min(1, $0 / max(pixelWidth, pixelHeight)) } ?? 1
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
}
