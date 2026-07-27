import UIKit
import UniformTypeIdentifiers

/// Receives an image from the system share sheet, parks it in the shared App Group container,
/// and hands off to the host app.
///
/// Deliberately headless and free of the Kotlin `Shared` framework: that framework is static, so
/// linking it would copy the whole binary into an extension that runs under a tight memory cap.
/// Writing one file is all the handoff needs.
class ShareViewController: UIViewController {

    private enum Handoff {
        static let appGroup = "group.com.github.worn"
        static let fileName = "pending_share.jpg"
        static let hostAppURL = URL(string: "worn://tryit")!
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let provider = item.attachments?.first(where: {
                  $0.hasItemConformingToTypeIdentifier(UTType.image.identifier)
              }) else {
            finish()
            return
        }

        _ = provider.loadDataRepresentation(forTypeIdentifier: UTType.image.identifier) { [weak self] data, _ in
            if let data = data { self?.writeHandoffFile(data) }
            DispatchQueue.main.async {
                self?.openHostApp()
                self?.finish()
            }
        }
    }

    private func writeHandoffFile(_ data: Data) {
        guard let container = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: Handoff.appGroup) else { return }
        try? data.write(to: container.appendingPathComponent(Handoff.fileName), options: .atomic)
    }

    /// `NSExtensionContext.open(_:)` does not launch the host app from a share extension, so this
    /// walks the responder chain to reach `UIApplication`. If the open fails the file still sits in
    /// the container and the app picks it up the next time it comes to the foreground.
    private func openHostApp() {
        var responder: UIResponder? = self
        while let current = responder {
            if let application = current as? UIApplication {
                application.open(Handoff.hostAppURL, options: [:], completionHandler: nil)
                return
            }
            responder = current.next
        }
    }

    private func finish() {
        extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
    }
}
