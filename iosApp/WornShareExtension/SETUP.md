# WornShareExtension

Shares a photo from any app into Worn's Try It screen.

The target is committed to `iosApp.xcodeproj` — there is no manual Xcode setup left to do. Open
the project and it builds alongside the app.

## How the handoff works

The extension deliberately does **not** link the `Shared` framework. `shared/build.gradle.kts`
sets `isStatic = true`, so linking it would copy the whole Kotlin binary into a process that runs
under a tight memory cap. The entire handoff is one file in an App Group:

1. `ShareViewController` writes the image to `group.com.github.worn/pending_share.jpg`.
2. It then walks the responder chain to reach `UIApplication` and open `worn://tryit`.
3. `RootView.receiveSharedPhoto()` calls `SharedPhotoInbox.consume()`, which reads the file and
   deletes it, and switches to the Try It tab.

Step 2 is a workaround: `NSExtensionContext.open(_:)` does not launch the host app from a share
extension on current iOS. It is widely shipped but is not blessed API and has occasionally drawn
App Review attention. If you would rather not ship it, delete `openHostApp()` and its call — the
extension still parks the file, and `RootView`'s `scenePhase` observer picks it up the next time
the user opens Worn themselves.

## Target configuration

Set in `project.pbxproj`, and worth preserving if the target is ever recreated:

| Setting | Value |
|---|---|
| `PRODUCT_BUNDLE_IDENTIFIER` | `$(inherited).ShareExtension` — resolves to `com.github.worn.Worn$(TEAM_ID).ShareExtension`, and must stay a child of the app id or embedded-binary validation fails |
| `INFOPLIST_FILE` | `WornShareExtension/Info.plist` |
| `GENERATE_INFOPLIST_FILE` | `YES` — the checked-in plist only carries `NSExtension` and the display name; Xcode synthesises `CFBundleIdentifier` and friends, same as the app target |
| `CODE_SIGN_ENTITLEMENTS` | `WornShareExtension/WornShareExtension.entitlements` |
| `IPHONEOS_DEPLOYMENT_TARGET` | `18.2` — matches the app |

Both targets carry `group.com.github.worn`; the app's entitlement lives in
`iosApp/iosApp.entitlements`. Without it on **both** sides the extension writes to a container
the app cannot read and the share silently does nothing.

The extension has no `Compile Kotlin Framework` build phase. Do not add one.

## Verifying

The app side is deterministic and does not need the share sheet:

```shell
xcrun simctl boot "iPhone 16"
xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -sdk iphonesimulator -arch arm64 \
  -configuration Debug CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM= build
xcrun simctl install booted iosApp/build/Debug-iphonesimulator/Worn.app

GROUP=$(xcrun simctl get_app_container booted com.github.worn.Worn group.com.github.worn)
cp some.jpg "$GROUP/pending_share.jpg"
xcrun simctl launch booted com.github.worn.Worn
```

Worn should open on **Try It** with the photo in the upload zone, and `pending_share.jpg` should
be gone — `consume()` deletes it either way, so an unreadable file cannot re-trigger forever.

The extension binary should stay tiny, which is how you know it is not pulling in `Shared`:

```shell
du -k iosApp/build/Debug-iphonesimulator/Worn.app/PlugIns/WornShareExtension.appex/WornShareExtension
# ~128 KB, against ~28 MB for the app binary
```

Then exercise the real share sheet — Photos → pick an image → Share → Worn — and walk the
credential matrix in Settings, sharing an image after each change:

| Claude key | YouCam creds | Expected |
|---|---|---|
| ✅ | ❌ | Try It, no dialog, *Analyze* visible |
| ❌ | ✅ | Try It, scrolled to the try-on section, no dialog |
| ✅ | ✅ | Chooser dialog; each choice scrolls to its section |
| ❌ | ❌ | Locked state with *Open Settings* |

Try the simulator first. If Worn does not appear in the share sheet, or `openHostApp()` does not
bring the app forward, repeat on a real device before concluding anything is broken.
