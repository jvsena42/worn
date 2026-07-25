package com.github.worn.data.source.image

/**
 * On-device background removal for clothing photos.
 *
 * The subject (garment) is isolated with the platform's native segmentation API and
 * composited onto a solid neutral background, then re-encoded as JPEG. Keeping the output
 * as JPEG means storage, display and the Claude API media type are all unaffected — callers
 * simply receive different bytes.
 *
 * Implemented per platform (`expect`/`actual`): Android uses ML Kit Subject Segmentation,
 * iOS uses the Vision framework. Throws if segmentation is unavailable or fails so callers
 * can fall back to the original photo.
 */
expect class BackgroundRemover {
    suspend fun removeBackground(bytes: ByteArray): ByteArray
}
