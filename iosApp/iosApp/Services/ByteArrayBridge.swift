import Foundation
import Shared

// Kotlin's `ByteArray` has no automatic Objective-C bridging, so every photo crossing into or out
// of the shared module goes through these two conversions. They live in one file because Swift
// treats a `private` copy in one file and an `internal` copy in another as a redeclaration.

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let bytes = [UInt8](self)
        let array = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}

extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        for index in 0..<Int(size) {
            data[index] = UInt8(bitPattern: get(index: Int32(index)))
        }
        return data
    }
}
