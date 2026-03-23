import Foundation
import Shared

@MainActor
class OutfitViewModelWrapper: ObservableObject {
    private let viewModel: OutfitViewModel
    private var cancellable: Cancellable?

    @Published var state: OutfitState

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: OutfitViewModel.self) as! OutfitViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let adapter = FlowAdapter(flow: vm.state)
        cancellable = adapter.subscribe { [weak self] newState in
            guard let newState = newState as? OutfitState else { return }
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func loadOutfits() {
        viewModel.onIntent(intent: OutfitIntent.LoadOutfits())
    }

    func toggleSelection(_ outfitId: String) {
        viewModel.onIntent(intent: OutfitIntent.ToggleSelection(outfitId: outfitId))
    }

    func clearSelection() {
        viewModel.onIntent(intent: OutfitIntent.ClearSelection())
    }

    func deleteSelected() {
        viewModel.onIntent(intent: OutfitIntent.DeleteSelected())
    }

    deinit {
        cancellable?.cancel()
    }
}
