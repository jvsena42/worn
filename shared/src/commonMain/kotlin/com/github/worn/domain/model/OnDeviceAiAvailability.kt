package com.github.worn.domain.model

/**
 * Whether this device can run the on-device AI model.
 *
 * Drives the Settings toggle: it is only interactive for [Available] and [Downloadable].
 */
sealed interface OnDeviceAiAvailability {
    data object Available : OnDeviceAiAvailability

    /** Supported, but the model is fetched on first use. */
    data object Downloadable : OnDeviceAiAvailability

    data class Unavailable(val reason: OnDeviceAiUnavailableReason) : OnDeviceAiAvailability
}

/**
 * Why on-device AI can't be used. An enum rather than a message so the UI can localize it —
 * each platform words these differently ("Apple Intelligence" vs. "Gemini Nano").
 */
enum class OnDeviceAiUnavailableReason {
    /** The hardware can't run the model (no Neural Engine / no AICore support). */
    UNSUPPORTED_DEVICE,

    /** The OS is older than the on-device model API requires. */
    UNSUPPORTED_OS,

    /** Supported, but the user has switched the platform AI feature off in system settings. */
    DISABLED_BY_USER,

    UNKNOWN,
}

val OnDeviceAiAvailability.isUsable: Boolean
    get() = this is OnDeviceAiAvailability.Available || this is OnDeviceAiAvailability.Downloadable
