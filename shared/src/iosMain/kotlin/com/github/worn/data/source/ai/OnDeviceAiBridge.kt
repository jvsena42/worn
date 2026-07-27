package com.github.worn.data.source.ai

/**
 * The Swift half of the iOS on-device AI engine.
 *
 * Apple's `FoundationModels` is a **Swift-only** framework: it exposes no Objective-C interface,
 * and Kotlin/Native interop reaches C and Objective-C only. So unlike `Vision` in
 * [BackgroundRemover.ios.kt][com.github.worn.data.source.image.BackgroundRemover], it cannot be
 * called from `iosMain` — the implementation lives in `OnDeviceAiService.swift` and is handed to
 * Kotlin at launch through [OnDeviceAiBridgeRegistry].
 *
 * Callbacks rather than `suspend` members because Swift types cannot implement Kotlin `suspend`
 * functions; [IosOnDeviceAiEngine] converts them back into suspending calls.
 */
interface OnDeviceAiBridge {

    fun availability(onResult: (OnDeviceAiAvailabilityToken) -> Unit)

    /**
     * Runs the prompt and calls back with `(responseText, errorMessage)` — exactly one is non-null.
     */
    fun generate(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray?,
        onResult: (String?, String?) -> Unit,
    )
}

/**
 * What Swift reports back, kept separate from [OnDeviceAiAvailability][
 * com.github.worn.domain.model.OnDeviceAiAvailability] because a flat enum bridges to Swift as
 * plain cases (`.available`) whereas a sealed interface would make Swift construct Kotlin
 * instances. Mapping to the domain model stays in [IosOnDeviceAiEngine].
 */
enum class OnDeviceAiAvailabilityToken {
    AVAILABLE,
    DOWNLOADABLE,
    UNSUPPORTED_DEVICE,
    UNSUPPORTED_OS,
    DISABLED_BY_USER,
    UNKNOWN,
}

/** Set once from `iOSApp.init()`, before any screen can resolve the engine from Koin. */
object OnDeviceAiBridgeRegistry {
    var bridge: OnDeviceAiBridge? = null
}
