import Foundation
import Shared

/// Bridges the shared `BackgroundRemover` (Vision-backed on iOS) to Swift, handling the
/// `Data` <-> `KotlinByteArray` conversion. The Kotlin `suspend` function is exposed to Swift
/// as `async throws`, so failures propagate and callers can fall back to the original photo.
enum BackgroundRemoverService {

    static func removeBackground(_ data: Data) async throws -> Data {
        let remover = KoinHelper.shared.koin.get(objCClass: BackgroundRemover.self) as! BackgroundRemover
        let output = try await remover.removeBackground(bytes: data.toKotlinByteArray())
        return output.toData()
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let bytes = [UInt8](self)
        let array = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}

private extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        for index in 0..<Int(size) {
            data[index] = UInt8(bitPattern: get(index: Int32(index)))
        }
        return data
    }
}
