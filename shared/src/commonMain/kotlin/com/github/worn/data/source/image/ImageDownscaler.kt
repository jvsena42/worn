package com.github.worn.data.source.image

/**
 * Shrinks a photo for an outbound AI/API request.
 *
 * Photos are stored at capture quality — cataloguing a wardrobe is worth the disk — but sending
 * those bytes verbatim is wasteful and, past a point, broken: base64 inflates a request by ~33%,
 * and Anthropic rejects images over 5MB while internally downscaling anything above ~1.15MP
 * anyway. So the reduction belongs on the request path only, never on the storage path.
 *
 * Output is always JPEG when re-encoded, which is what makes the hardcoded `image/jpeg` media
 * type in [com.github.worn.data.source.remote.ClaudeApiClient] honest — a gallery pick can be
 * HEIC or PNG, and Claude rejects a mislabelled one.
 *
 * Implemented per platform (`expect`/`actual`) because decoding and re-encoding need
 * `Bitmap`/`UIImage`: Android uses `BitmapFactory` + `Bitmap.compress`, iOS uses
 * `UIGraphicsImageRenderer` + `UIImageJPEGRepresentation`.
 */
expect class ImageDownscaler {
    /**
     * Returns [bytes] re-encoded as JPEG with its long edge at most [maxEdge].
     *
     * Never upscales, and returns [bytes] untouched when the image already fits and is already
     * JPEG. Returns [bytes] untouched rather than throwing when the image cannot be decoded, so a
     * format this platform does not understand still reaches the API and fails with the API's own
     * message instead of a generic local one.
     */
    suspend fun downscale(bytes: ByteArray, maxEdge: Int): ByteArray
}
