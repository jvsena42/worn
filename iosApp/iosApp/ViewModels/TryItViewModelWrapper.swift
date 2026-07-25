import Foundation
import Shared

@MainActor
class TryItViewModelWrapper: ObservableObject {
    private let viewModel: TryItViewModel
    private var cancellable: Cancellable?

    @Published var state: TryItState

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: TryItViewModel.self) as! TryItViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let adapter = FlowAdapter(flow: vm.state)
        cancellable = adapter.subscribe { [weak self] newState in
            guard let newState = newState as? TryItState else { return }
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func analyzePhoto(imageData: Data) {
        viewModel.onIntent(intent: TryItIntent.AnalyzePhoto(imageBytes: imageData.toKotlinByteArray()))
    }

    func reset() {
        viewModel.onIntent(intent: TryItIntent.Reset())
    }

    func selectCategory(_ category: GarmentCategory) {
        viewModel.onIntent(intent: TryItIntent.SelectCategory(category: category))
    }

    func generateTryOn(imageData: Data) {
        viewModel.onIntent(intent: TryItIntent.GenerateTryOn(garmentBytes: imageData.toKotlinByteArray()))
    }

    func resetTryOn() {
        viewModel.onIntent(intent: TryItIntent.ResetTryOn())
    }

    func setPersonPhoto(imageData: Data) {
        viewModel.onIntent(intent: TryItIntent.SetPersonPhoto(imageBytes: imageData.toKotlinByteArray()))
    }

    /// The saved person photo, if any, as displayable `Data`.
    var personImageData: Data? {
        state.personImage?.toData()
    }

    /// The generated try-on image, if any, as displayable `Data`.
    var tryOnImageData: Data? {
        state.tryOnImage?.toData()
    }

    deinit {
        cancellable?.cancel()
    }
}

extension Data {
    /// Bridges Swift `Data` to a Kotlin `KotlinByteArray` for shared-module calls.
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
    /// Bridges a Kotlin `KotlinByteArray` back to Swift `Data`.
    func toData() -> Data {
        var data = Data(count: Int(size))
        for index in 0..<Int(size) {
            data[index] = UInt8(bitPattern: get(index: Int32(index)))
        }
        return data
    }
}
