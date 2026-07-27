# WornShareExtension — Xcode setup

The Swift sources, `Info.plist`, and entitlements in this folder are complete. The target itself
must be created in Xcode, because `iosApp.xcodeproj/project.pbxproj` uses the synchronized-folder
layout (`objectVersion = 77`) and hand-editing it is error-prone.

Do this once, on a machine with Xcode.

## 1. Create the target

**File → New → Target… → iOS → Share Extension**

- Product Name: `WornShareExtension`
- Embed in Application: `Worn`
- Language: Swift
- When prompted to activate the new scheme: **Cancel** (keep the `iosApp` scheme).

Xcode generates its own `ShareViewController.swift`, `MainInterface.storyboard`, and `Info.plist`
under a new group. **Delete all three** (Move to Trash), then drag this `WornShareExtension/`
folder into the project and assign it to the `WornShareExtension` target.

## 2. Build settings for the extension target

| Setting | Value |
|---|---|
| `INFOPLIST_FILE` | `WornShareExtension/Info.plist` |
| `GENERATE_INFOPLIST_FILE` | `NO` |
| `PRODUCT_BUNDLE_IDENTIFIER` | `$(inherited).ShareExtension` — must be a child of the app id |
| `IPHONEOS_DEPLOYMENT_TARGET` | `18.2` — match the app |
| `CODE_SIGN_ENTITLEMENTS` | `WornShareExtension/WornShareExtension.entitlements` |

The app id comes from `iosApp/Configuration/Config.xcconfig`
(`com.github.worn.Worn$(TEAM_ID)`), so make sure the extension target also picks up that xcconfig.

If Xcode copied the **Compile Kotlin Framework** run-script build phase onto the new target,
delete it. The extension does not link the `Shared` framework on purpose: it is a static framework
(`shared/build.gradle.kts`, `isStatic = true`), so linking would duplicate the whole binary into a
process that runs under a tight memory cap. Parking one file in the App Group is the entire handoff.

## 3. App Groups capability — required on BOTH targets

**Signing & Capabilities → + Capability → App Groups**, then add `group.com.github.worn` to:

- the `Worn` app target — also point its `CODE_SIGN_ENTITLEMENTS` at `iosApp/iosApp.entitlements`
- the `WornShareExtension` target

Without the group on both sides, the extension writes to a container the app cannot read and the
share silently does nothing.

## 4. Verify

Share extensions do **not** appear in the Simulator's Photos share sheet, so this needs a real
device.

1. Run the `Worn` scheme on a device.
2. Photos → pick an image → Share → Worn.
3. The app should open on **Try It** with the photo already in the upload zone.

Then walk the credential matrix in Settings, sharing an image after each change:

| Claude key | YouCam creds | Expected |
|---|---|---|
| ✅ | ❌ | Lands on Try It, no dialog, *Analyze* button visible |
| ❌ | ✅ | Lands on Try It, scrolled to the try-on section, no dialog |
| ✅ | ✅ | Chooser dialog; each choice scrolls to its section, both stay visible |
| ❌ | ❌ | Locked state with *Open Settings* |

## Known caveat: launching the app from the extension

`NSExtensionContext.open(_:)` does not launch the host app from a share extension on current iOS.
`ShareViewController.openHostApp()` therefore walks the responder chain to reach `UIApplication`.
This is widely shipped but is not blessed API and has occasionally drawn App Review attention.

If you would rather not ship it, delete `openHostApp()` and its call. The extension still writes
the handoff file, and `iOSApp.receiveSharedPhoto()` already picks it up on the next foreground via
the `scenePhase` observer — correct behaviour, but the user has to open Worn themselves.
