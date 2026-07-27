# Worn — Journey Tests

Journeys are XML-specified, end-to-end UI test cases for the Worn app. Each journey is a
sequential list of `<action>` steps that describe a real user flow in plain language. They are
evaluated by driving the running app (via the `android` CLI's journey tooling), inspecting the
on-screen layout, and checking that every step succeeds.

A journey **passes** only if all of its actions succeed; if the app crashes, freezes, or an
action cannot be performed as written, the journey **fails**.

## Locating elements

Interactive and assertable components carry stable test identifiers so steps can target them
unambiguously. On Android these are Compose `Modifier.testTag(...)` values surfaced as
`resource-id` in the layout tree (`testTagsAsResourceId = true`); iOS mirrors the same string
values as `accessibilityIdentifier`. The identifiers are plain snake_case strings applied at each
component's call site (e.g. `tab_wardrobe`, `add_item_save_button`, `api_key_field`). Steps below
reference the visible text a user sees; the identifiers are the fallback the evaluator uses to
disambiguate.

## Preconditions

These journeys assume a **fresh install**: an empty wardrobe, no saved outfits, and no credentials
configured (neither the Claude API key nor YouCam try-on credentials, and no saved model photo).
That state exercises the empty-state and locked flows without needing a real photo, network access,
or credentials. Journeys that would require capturing a photo or calling an external API stop at the
point where that external input is needed and verify the UI is in the expected state.

## Running

With a device or emulator connected and the debug app installed:

```shell
# Build & install the debug app
./gradlew :composeApp:assembleDebug
android run --apks composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Then evaluate a journey file with the android journey tooling, e.g.
android layout --pretty          # inspect the current screen's tree
```

Each journey is self-contained; evaluate them independently.

### When `android layout` fails

On some devices `android layout` cannot dump the view tree at all, and the failure has nothing to
do with the app under test. Xiaomi/MIUI builds are a known case: the dump crashes inside MIUI's
own resource loading with

```
Failed to retrieve UI dump: java.io.FileNotFoundException:
/data/system/theme_config/theme_compatibility.xml: open failed: ENOENT
```

Every journey step is still evaluable — locate elements visually instead of by tree:

```shell
android screen capture --annotate -o screen.png   # labels each element with a number
adb shell input $(android screen resolve --screenshot screen.png --string "tap #26")
```

Note the flag is `--screenshot`, not `--screen`. Re-capture after each interaction, since the
labels are assigned per screenshot and do not survive a screen change. The `resource-id` test tags
described above are unavailable on this path, so fall back to the visible text a user would see.

## Journeys

| File | Flow |
|------|------|
| `bottom-navigation.xml` | Switch through all five bottom-bar tabs and verify each screen. |
| `add-first-item.xml` | From the empty wardrobe, open the Add-item sheet and the photo-source dialog. |
| `remove-background.xml` | Verify the Add-item "Remove background" toggle is gated on a photo being present (absent until one is chosen). |
| `crop-photo.xml` | Verify the Add-item "Crop" button is gated on a photo being present (absent until one is chosen). |
| `create-first-outfit.xml` | From the empty Outfits tab, open the Create-outfit sheet. |
| `connect-api-key.xml` | Open Settings and reach the Claude API key entry sheet. |
| `connect-youcam.xml` | Open Settings and reach the YouCam try-on credentials sheet. |
| `edit-profile.xml` | Open Settings and reach the Your-Profile sheet with its chip groups. |
| `try-it-ai-locked.xml` | Verify the Try It locked state (neither key) routes to Settings. |
| `gaps-common-suggestions.xml` | Verify the Gaps common-suggestions banner opens the AI-locked sheet. |
