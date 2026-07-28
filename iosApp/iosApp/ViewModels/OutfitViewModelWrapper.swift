import Foundation
import SwiftUI
import Shared

@MainActor
class OutfitViewModelWrapper: ObservableObject {
    private let viewModel: OutfitViewModel
    private var stateCancellable: Cancellable?
    private var effectsCancellable: Cancellable?

    @Published var state: OutfitState
    @Published var outfitCreated = false

    init() {
        let vm = KoinHelper.shared.outfitViewModel
        self.viewModel = vm

        let stateAdapter = FlowAdapter<OutfitState>(flow: vm.state)
        self.state = stateAdapter.currentValue
        stateCancellable = stateAdapter.subscribe { [weak self] newState in
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }

        let effectsAdapter = EffectAdapter(flow: vm.effects)
        effectsCancellable = effectsAdapter.subscribe { [weak self] effect in
            guard let effect = effect as? OutfitEffect else { return }
            DispatchQueue.main.async {
                if effect is OutfitEffectOutfitCreated {
                    self?.outfitCreated = true
                }
            }
        }
    }

    func filterItemsByCategory(_ category: Shared.Category?) {
        let intent = OutfitIntentFilterItemsByCategory(category: category)
        viewModel.onIntent(intent: intent)
    }

    func toggleItemSelection(_ itemId: String) {
        viewModel.onIntent(intent: OutfitIntentToggleItemSelection(itemId: itemId))
    }

    func toggleSelection(_ outfitId: String) {
        viewModel.onIntent(intent: OutfitIntentToggleSelection(outfitId: outfitId))
    }

    func clearSelection() {
        viewModel.onIntent(intent: OutfitIntentClearSelection())
    }

    func deleteSelected() {
        viewModel.onIntent(intent: OutfitIntentDeleteSelected())
    }

    func createOutfit(name: String) {
        viewModel.onIntent(intent: OutfitIntentCreateOutfit(name: name))
    }

    func deleteOutfit(_ outfitId: String) {
        viewModel.onIntent(intent: OutfitIntentDeleteOutfit(outfitId: outfitId))
    }

    func updateOutfit(_ outfit: Outfit) {
        viewModel.onIntent(intent: OutfitIntentUpdateOutfit(outfit: outfit))
    }

    deinit {
        stateCancellable?.cancel()
        effectsCancellable?.cancel()
    }
}
