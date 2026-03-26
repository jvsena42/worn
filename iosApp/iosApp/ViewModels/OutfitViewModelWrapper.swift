import Foundation
import Shared

@MainActor
class OutfitViewModelWrapper: ObservableObject {
    private let viewModel: OutfitViewModel
    private var stateCancellable: Cancellable?
    private var effectsCancellable: Cancellable?

    @Published var state: OutfitState
    @Published var outfitCreated = false

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: OutfitViewModel.self) as! OutfitViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let stateAdapter = FlowAdapter(flow: vm.state)
        stateCancellable = stateAdapter.subscribe { [weak self] newState in
            guard let newState = newState as? OutfitState else { return }
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }

        let effectsAdapter = FlowAdapter(flow: vm.effects)
        effectsCancellable = effectsAdapter.subscribe { [weak self] effect in
            guard let effect = effect as? OutfitEffect else { return }
            DispatchQueue.main.async {
                if effect is OutfitEffect.OutfitCreated {
                    self?.outfitCreated = true
                }
            }
        }
    }

    func loadOutfits() {
        viewModel.onIntent(intent: OutfitIntent.LoadOutfits())
    }

    func loadClothingItems() {
        viewModel.onIntent(intent: OutfitIntent.LoadClothingItems())
    }

    func filterItemsByCategory(_ category: Category?) {
        let intent = OutfitIntent.FilterItemsByCategory(category: category)
        viewModel.onIntent(intent: intent)
    }

    func toggleItemSelection(_ itemId: String) {
        viewModel.onIntent(intent: OutfitIntent.ToggleItemSelection(itemId: itemId))
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

    func createOutfit(name: String) {
        viewModel.onIntent(intent: OutfitIntent.CreateOutfit(name: name))
    }

    func deleteOutfit(_ outfitId: String) {
        viewModel.onIntent(intent: OutfitIntent.DeleteOutfit(outfitId: outfitId))
    }

    func updateOutfit(_ outfit: Outfit) {
        viewModel.onIntent(intent: OutfitIntent.UpdateOutfit(outfit: outfit))
    }

    deinit {
        stateCancellable?.cancel()
        effectsCancellable?.cancel()
    }
}
