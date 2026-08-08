import SwiftUI
import ImageIO

/// Displays a stored wardrobe photo, decoded at the size it is actually drawn.
///
/// `AsyncImage` decodes the full bitmap regardless of the frame it renders into. Photos are stored
/// at capture resolution, so a grid of them would decode ~48MB each — enough to make scrolling
/// stutter and, on a large wardrobe, to get the app killed. `CGImageSourceCreateThumbnailAtIndex`
/// decodes straight to the target size instead.
///
/// It also drops the `FileManager.fileExists` check the call sites used to run: that is a blocking
/// `stat` on the main thread for every visible cell, and a failed load already falls back to
/// `placeholder`.
struct StoredPhotoImage<Placeholder: View>: View {
    let path: String
    @ViewBuilder var placeholder: () -> Placeholder

    @Environment(\.displayScale) private var displayScale
    @State private var image: UIImage?

    var body: some View {
        GeometryReader { proxy in
            let target = LoadTarget(path: path, size: proxy.size)
            Group {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } else {
                    placeholder()
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
            .clipped()
            .task(id: target) { await load(target) }
        }
    }

    private func load(_ target: LoadTarget) async {
        guard target.maxEdge > 0 else { return }
        let pixels = target.maxEdge * displayScale
        let path = target.path
        image = await Task.detached(priority: .userInitiated) {
            thumbnail(atPath: path, maxPixelSize: pixels)
        }.value
    }
}

/// The inputs a decode depends on. Rounded into buckets so a fractional layout change during
/// scrolling does not re-decode, and `Equatable` so `task(id:)` reloads a recycled cell.
private struct LoadTarget: Equatable {
    let path: String
    let maxEdge: CGFloat

    init(path: String, size: CGSize) {
        self.path = path
        let longest = max(size.width, size.height)
        self.maxEdge = longest > 0 ? (longest / 32).rounded(.up) * 32 : 0
    }
}

private func thumbnail(atPath path: String, maxPixelSize: CGFloat) -> UIImage? {
    let url = URL(fileURLWithPath: path) as CFURL
    guard let source = CGImageSourceCreateWithURL(url, nil) else { return nil }
    let options: [CFString: Any] = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        // Honours the EXIF tag, so a camera photo is not rendered sideways.
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceShouldCacheImmediately: true,
        kCGImageSourceThumbnailMaxPixelSize: max(1, maxPixelSize),
    ]
    guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else {
        return nil
    }
    return UIImage(cgImage: cgImage)
}
