import Foundation
import Shared

@MainActor
class WardrobeViewModelWrapper: ObservableObject {
    private let viewModel: WardrobeViewModel
    private var cancellable: Cancellable?

    @Published var state: WardrobeState

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: WardrobeViewModel.self) as! WardrobeViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let adapter = FlowAdapter(flow: vm.state)
        cancellable = adapter.subscribe { [weak self] newState in
            guard let newState = newState as? WardrobeState else { return }
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

    func filterByCategory(_ category: Category?) {
        let intent = WardrobeIntent.FilterByCategory(category: category)
        viewModel.onIntent(intent: intent)
    }

    func loadItems() {
        viewModel.onIntent(intent: WardrobeIntent.LoadItems())
    }

    func addItem(
        imageData: Data, name: String, category: Category, colors: [String], seasons: [Season],
        subcategory: Subcategory? = nil, fit: Fit? = nil, material: Material? = nil
    ) {
        let bytes = [UInt8](imageData)
        let kotlinBytes = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            kotlinBytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        let intent = WardrobeIntent.AddItem(
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
        viewModel.onIntent(intent: WardrobeIntent.ToggleSelection(itemId: itemId))
    }

    func clearSelection() {
        viewModel.onIntent(intent: WardrobeIntent.ClearSelection())
    }

    func deleteSelected() {
        viewModel.onIntent(intent: WardrobeIntent.DeleteSelected())
    }

    func deleteItem(_ itemId: String) {
        viewModel.onIntent(intent: WardrobeIntent.DeleteItem(itemId: itemId))
    }

    func updateItem(_ item: ClothingItem) {
        viewModel.onIntent(intent: WardrobeIntent.UpdateItem(item: item))
    }

    deinit {
        cancellable?.cancel()
    }
}
