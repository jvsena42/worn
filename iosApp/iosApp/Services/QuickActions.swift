import Shared
import SwiftUI

/// One Home Screen quick-action tap, waiting to be routed.
///
/// The `id` makes every tap distinct, so tapping the same action twice in a row still reads as a
/// change — the same reason `SharedPhoto` carries one.
struct ShortcutCommand: Equatable {
    let id = UUID()
    let shortcut: AppShortcut
}

/// Receives quick-action taps from `SceneDelegate` and hands them to SwiftUI.
///
/// SwiftUI has no hook for `performActionFor`, so the delegates below are the only way in.
@MainActor
final class QuickActionInbox: ObservableObject {
    static let shared = QuickActionInbox()

    @Published var pending: ShortcutCommand?

    private init() {}

    func receive(_ item: UIApplicationShortcutItem) {
        guard let shortcut = AppShortcut.companion.fromId(id: item.type) else { return }
        pending = ShortcutCommand(shortcut: shortcut)
    }

    /// Registered at launch rather than declared in `Info.plist` so the titles come from
    /// `Localizable.strings` like every other user-facing string, and the icons are SF Symbols like
    /// the rest of the app. The trade-off: the actions appear after the first launch, not at install.
    static func register() {
        UIApplication.shared.shortcutItems = [
            UIApplicationShortcutItem(
                type: AppShortcut.addItem.id,
                localizedTitle: String(localized: "shortcut_add_item_short"),
                localizedSubtitle: String(localized: "shortcut_add_item_long"),
                icon: UIApplicationShortcutIcon(systemImageName: "plus")
            ),
            UIApplicationShortcutItem(
                type: AppShortcut.tryIt.id,
                localizedTitle: String(localized: "shortcut_try_it_short"),
                localizedSubtitle: String(localized: "shortcut_try_it_long"),
                icon: UIApplicationShortcutIcon(systemImageName: "viewfinder")
            ),
        ]
    }
}

/// Exists only to attach `SceneDelegate`; SwiftUI still owns the window.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(
            name: connectingSceneSession.configuration.name,
            sessionRole: connectingSceneSession.role
        )
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }
}

final class SceneDelegate: NSObject, UIWindowSceneDelegate {

    /// Cold launch: the tap arrives here, not in `performActionFor`.
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let item = connectionOptions.shortcutItem else { return }
        QuickActionInbox.shared.receive(item)
    }

    /// The app was already running.
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        QuickActionInbox.shared.receive(shortcutItem)
        completionHandler(true)
    }
}
