import Foundation
import SwiftUI
import Shared

@MainActor
class GapsViewModelWrapper: ObservableObject {
    private let viewModel: GapsViewModel
    private var cancellable: Cancellable?

    @Published var state: GapsState

    init() {
        let vm = KoinHelper.shared.gapsViewModel
        self.viewModel = vm

        let adapter = FlowAdapter<GapsState>(flow: vm.state)
        self.state = adapter.currentValue
        cancellable = adapter.subscribe { [weak self] newState in
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func loadGaps() {
        viewModel.onIntent(intent: GapsIntentLoadGaps())
    }

    deinit {
        cancellable?.cancel()
    }
}
