import Foundation
import SwiftUI
import Shared

@MainActor
class GapsViewModelWrapper: ObservableObject {
    private let viewModel: GapsViewModel
    private var stateCancellable: Cancellable?
    private var effectsCancellable: Cancellable?

    @Published var state: GapsState
    @Published var itemAdded = false

    init() {
        let vm = KoinHelper.shared.gapsViewModel
        self.viewModel = vm

        let stateAdapter = FlowAdapter<GapsState>(flow: vm.state)
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
            guard let effect = effect as? GapsEffect else { return }
            DispatchQueue.main.async {
                if effect is GapsEffectItemAdded {
                    self?.itemAdded = true
                }
            }
        }
    }

    func loadGaps() {
        viewModel.onIntent(intent: GapsIntentLoadGaps())
    }

    func addItem(
        imageData: Data, name: String, category: Shared.Category, colors: [String], seasons: [Season],
        subcategory: Subcategory? = nil, fit: Fit? = nil, material: Shared.Material? = nil
    ) {
        let bytes = [UInt8](imageData)
        let kotlinBytes = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            kotlinBytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        let intent = GapsIntentAddItem(
            imageBytes: kotlinBytes,
            name: name,
            category: category,
            colors: colors,
            seasons: seasons,
            subcategory: subcategory,
            fit: fit,
            material: material
        )
        viewModel.onIntent(intent: intent)
    }

    deinit {
        stateCancellable?.cancel()
        effectsCancellable?.cancel()
    }
}
