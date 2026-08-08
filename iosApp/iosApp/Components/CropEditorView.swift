import SwiftUI
import Shared

/// Free-form crop editor for a photo already picked by one of the add-photo flows.
///
/// Presented as a `.fullScreenCover`, mirroring the full-screen Dialog the Android editor uses.
/// The selection math lives in the shared `CropGeometry` so both platforms agree on it; this view
/// only owns the gestures and the drawing.
struct CropEditorView: View {
    let imageData: Data
    let onCropped: (Data) -> Void
    let onCancel: () -> Void

    @State private var image: UIImage?
    @State private var viewSize: CGSize = .zero
    @State private var bounds: CropViewRect?
    @State private var selection: CropViewRect?
    @State private var dragStart: CropViewRect?
    @State private var activeCorner: CropCorner?
    @State private var isProcessing = false
    @State private var showError = false

    private let handleTouchRadius: CGFloat = 28
    private let minCropEdge: Float = 48
    private let scrimOpacity: Double = 0.55
    private let handleArm: CGFloat = 56

    var body: some View {
        VStack(spacing: 0) {
            topBar
            editorCanvas
            bottomBar
        }
        .background(Color.black.ignoresSafeArea())
        .accessibilityIdentifier("crop_editor")
        .task { await loadImage() }
        .alert(String(localized: "crop_failed"), isPresented: $showError) {
            Button(String(localized: "common_ok"), role: .cancel) {}
        }
    }

    // MARK: - Chrome

    private var topBar: some View {
        HStack {
            Button(String(localized: "common_cancel"), action: onCancel)
                .foregroundColor(.white)
                .accessibilityIdentifier("crop_editor_cancel")
            Spacer()
            Text(String(localized: "crop_title"))
                .font(.callout.weight(.semibold))
                .foregroundColor(.white)
            Spacer()
            Button(String(localized: "crop_apply"), action: applyCrop)
                .font(.callout.weight(.semibold))
                .foregroundColor(canApply ? WornColors.accentGreen : WornColors.textMuted)
                .disabled(!canApply)
                .accessibilityIdentifier("crop_editor_apply")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private var bottomBar: some View {
        Button(String(localized: "crop_reset")) { selection = bounds }
            .font(.subheadline)
            .foregroundColor(.white)
            .disabled(bounds == nil || isProcessing)
            .padding(.vertical, 12)
            .accessibilityIdentifier("crop_editor_reset")
    }

    private var canApply: Bool { selection != nil && bounds != nil && !isProcessing }

    // MARK: - Canvas

    private var editorCanvas: some View {
        GeometryReader { geo in
            ZStack {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(width: geo.size.width, height: geo.size.height)
                    Canvas { context, size in drawOverlay(in: context, size: size) }
                        .allowsHitTesting(false)
                } else {
                    ProgressView().tint(WornColors.accentGreen)
                }
                if isProcessing {
                    Color.black.opacity(0.4)
                    ProgressView().tint(WornColors.accentGreen)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .contentShape(Rectangle())
            .gesture(dragGesture)
            .onAppear { viewSize = geo.size }
            .onChange(of: geo.size) { _, newValue in viewSize = newValue }
            .onChange(of: viewSize) { _, _ in recomputeBounds() }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier("crop_editor_canvas")
    }

    private func drawOverlay(in context: GraphicsContext, size: CGSize) {
        guard let selection else { return }
        let rect = cgRect(selection)

        var scrim = Path(CGRect(origin: .zero, size: size))
        scrim.addRect(rect)
        context.fill(scrim, with: .color(.black.opacity(scrimOpacity)), style: FillStyle(eoFill: true))

        context.stroke(Path(rect), with: .color(.white), lineWidth: 1.5)

        var guides = Path()
        for step in 1...2 {
            let x = rect.minX + rect.width * CGFloat(step) / 3
            let y = rect.minY + rect.height * CGFloat(step) / 3
            guides.move(to: CGPoint(x: x, y: rect.minY))
            guides.addLine(to: CGPoint(x: x, y: rect.maxY))
            guides.move(to: CGPoint(x: rect.minX, y: y))
            guides.addLine(to: CGPoint(x: rect.maxX, y: y))
        }
        context.stroke(guides, with: .color(.white.opacity(0.35)), lineWidth: 0.75)

        let arm = min(handleArm, rect.width / 3, rect.height / 3)
        var handles = Path()
        let corners: [(CGPoint, CGFloat, CGFloat)] = [
            (CGPoint(x: rect.minX, y: rect.minY), 1, 1),
            (CGPoint(x: rect.maxX, y: rect.minY), -1, 1),
            (CGPoint(x: rect.minX, y: rect.maxY), 1, -1),
            (CGPoint(x: rect.maxX, y: rect.maxY), -1, -1),
        ]
        for (corner, dirX, dirY) in corners {
            handles.move(to: corner)
            handles.addLine(to: CGPoint(x: corner.x + arm * dirX, y: corner.y))
            handles.move(to: corner)
            handles.addLine(to: CGPoint(x: corner.x, y: corner.y + arm * dirY))
        }
        context.stroke(handles, with: .color(.white), lineWidth: 4)
    }

    // MARK: - Gestures

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard let bounds, let current = selection else { return }
                if dragStart == nil {
                    dragStart = current
                    activeCorner = cornerAt(value.startLocation, in: current)
                }
                guard let base = dragStart else { return }
                // SwiftUI reports cumulative translation, so the base is the drag-start selection.
                let dx = Float(value.translation.width)
                let dy = Float(value.translation.height)
                if let corner = activeCorner {
                    selection = CropGeometry.shared.resizeSelection(
                        base: base, corner: corner, dx: dx, dy: dy, bounds: bounds, minEdge: minCropEdge
                    )
                } else {
                    selection = CropGeometry.shared.moveSelection(base: base, dx: dx, dy: dy, bounds: bounds)
                }
            }
            .onEnded { _ in
                dragStart = nil
                activeCorner = nil
            }
    }

    private func cornerAt(_ point: CGPoint, in selection: CropViewRect) -> CropCorner? {
        let rect = cgRect(selection)
        let candidates: [(CropCorner, CGPoint)] = [
            (.topLeft, CGPoint(x: rect.minX, y: rect.minY)),
            (.topRight, CGPoint(x: rect.maxX, y: rect.minY)),
            (.bottomLeft, CGPoint(x: rect.minX, y: rect.maxY)),
            (.bottomRight, CGPoint(x: rect.maxX, y: rect.maxY)),
        ]
        let withinReach = candidates
            .map { (corner: $0.0, distance: hypot($0.1.x - point.x, $0.1.y - point.y)) }
            .filter { $0.distance <= handleTouchRadius }
        return withinReach.min { $0.distance < $1.distance }?.corner
    }

    // MARK: - Work

    private func loadImage() async {
        let data = imageData
        let prepared = await Task.detached(priority: .userInitiated) {
            ImageCropService.prepareForEditing(data)
        }.value
        guard let prepared else {
            // Leave the editor open on the error alert; Cancel returns the photo untouched.
            showError = true
            return
        }
        image = prepared
        recomputeBounds()
    }

    /// Resetting the selection on resize/rotation keeps it inside the newly fitted image.
    private func recomputeBounds() {
        guard let image, viewSize.width > 0, viewSize.height > 0 else { return }
        let fitted = CropGeometry.shared.fitBounds(
            imageWidth: Int32(image.size.width),
            imageHeight: Int32(image.size.height),
            viewWidth: Float(viewSize.width),
            viewHeight: Float(viewSize.height)
        )
        bounds = fitted
        selection = fitted
    }

    private func applyCrop() {
        guard let selection, let bounds else { return }
        isProcessing = true
        // Crops the original data, not the downsampled preview, so the saved photo keeps its
        // capture resolution.
        let data = imageData
        Task {
            let cropped = await Task.detached(priority: .userInitiated) {
                ImageCropService.crop(data, selection: selection, bounds: bounds)
            }.value
            isProcessing = false
            if let cropped {
                onCropped(cropped)
            } else {
                showError = true
            }
        }
    }

    private func cgRect(_ rect: CropViewRect) -> CGRect {
        CGRect(
            x: CGFloat(rect.left),
            y: CGFloat(rect.top),
            width: CGFloat(rect.width),
            height: CGFloat(rect.height)
        )
    }
}

private func previewImageData() -> Data {
    let size = CGSize(width: 600, height: 800)
    let image = UIGraphicsImageRenderer(size: size).image { context in
        WornColors.accentGreen.uiColor.setFill()
        context.fill(CGRect(origin: .zero, size: size))
    }
    return image.jpegData(compressionQuality: 0.9) ?? Data()
}

private extension Color {
    var uiColor: UIColor { UIColor(self) }
}

#Preview("iPhone") {
    CropEditorView(imageData: previewImageData(), onCropped: { _ in }, onCancel: {})
}

#Preview("iPad Portrait", traits: .portrait) {
    CropEditorView(imageData: previewImageData(), onCropped: { _ in }, onCancel: {})
}
