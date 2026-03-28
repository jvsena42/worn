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
        let bytes = [UInt8](imageData)
        let kotlinBytes = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            kotlinBytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        viewModel.onIntent(intent: TryItIntent.AnalyzePhoto(imageBytes: kotlinBytes))
    }

    func reset() {
        viewModel.onIntent(intent: TryItIntent.Reset())
    }

    deinit {
        cancellable?.cancel()
    }
}
