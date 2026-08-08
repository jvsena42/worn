package com.github.worn.data.source.image

/**
 * How far photos are shrunk on the way out to each AI/API provider.
 *
 * These bound the *request*, never what is written to disk — see [ImageDownscaler].
 */
object AiImageLimits {

    /**
     * Anthropic downscales anything above ~1.15MP to roughly this long edge before the model sees
     * it, so sending more pixels buys image tokens rather than accuracy. Comfortably under the
     * 5MB per-image limit once base64 inflation is accounted for.
     */
    const val CLAUDE_MAX_EDGE = 1568

    /**
     * Try-on returns a *generated* image rather than a description, so detail lost on the way in
     * shows up in the result. Kept higher than [CLAUDE_MAX_EDGE] while still bounding the upload.
     */
    const val TRY_ON_MAX_EDGE = 2048

    /** Requests are transient, so trade a little fidelity for payload size. Storage uses more. */
    const val JPEG_QUALITY = 85
}
