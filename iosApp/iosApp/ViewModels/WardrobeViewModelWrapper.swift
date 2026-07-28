import Foundation
import SwiftUI
import Shared

@MainActor
class WardrobeViewModelWrapper: ObservableObject {
    private let viewModel: WardrobeViewModel
    private var cancellable: Cancellable?

    @Published var state: WardrobeState

    init() {
        let vm = KoinHelper.shared.wardrobeViewModel
        self.viewModel = vm

        let adapter = FlowAdapter<WardrobeState>(flow: vm.state)
        self.state = adapter.currentValue
        cancellable = adapter.subscribe { [weak self] newState in
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func onIntent(_ intent: WardrobeIntent) {
        viewModel.onIntent(intent: intent)
    }

    func filterByCategory(_ category: Shared.Category?) {
        let intent = WardrobeIntentFilterByCategory(category: category)
        viewModel.onIntent(intent: intent)
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
        let intent = WardrobeIntentAddItem(
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

    func toggleSelection(_ itemId: String) {
        viewModel.onIntent(intent: WardrobeIntentToggleSelection(itemId: itemId))
    }

    func clearSelection() {
        viewModel.onIntent(intent: WardrobeIntentClearSelection())
    }

    func deleteSelected() {
        viewModel.onIntent(intent: WardrobeIntentDeleteSelected())
    }

    func deleteItem(_ itemId: String) {
        viewModel.onIntent(intent: WardrobeIntentDeleteItem(itemId: itemId))
    }

    func updateItem(_ item: ClothingItem) {
        viewModel.onIntent(intent: WardrobeIntentUpdateItem(item: item))
    }

    deinit {
        cancellable?.cancel()
    }
}
