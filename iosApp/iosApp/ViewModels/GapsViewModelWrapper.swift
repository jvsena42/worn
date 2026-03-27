import Foundation
import Shared

@MainActor
class GapsViewModelWrapper: ObservableObject {
    private let viewModel: GapsViewModel
    private var cancellable: Cancellable?

    @Published var state: GapsState

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: GapsViewModel.self) as! GapsViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let adapter = FlowAdapter(flow: vm.state)
        cancellable = adapter.subscribe { [weak self] newState in
            guard let newState = newState as? GapsState else { return }
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func loadGaps() {
        viewModel.onIntent(intent: GapsIntent.LoadGaps())
    }

    deinit {
        cancellable?.cancel()
    }
}
