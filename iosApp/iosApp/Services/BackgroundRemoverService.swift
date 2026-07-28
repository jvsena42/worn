import Foundation
import Shared

/// Bridges the shared `BackgroundRemover` (Vision-backed on iOS) to Swift, handling the
/// `Data` <-> `KotlinByteArray` conversion. The Kotlin `suspend` function is exposed to Swift
/// as `async throws`, so failures propagate and callers can fall back to the original photo.
enum BackgroundRemoverService {

    static func removeBackground(_ data: Data) async throws -> Data {
        let remover = KoinHelper.shared.backgroundRemover
        let output = try await remover.removeBackground(bytes: data.toKotlinByteArray())
        return output.toData()
    }
}
