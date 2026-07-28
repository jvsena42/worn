import Foundation
import SwiftUI
import Shared

@MainActor
class TryItViewModelWrapper: ObservableObject {
    private let viewModel: TryItViewModel
    private var cancellable: Cancellable?

    @Published var state: TryItState

    init() {
        let vm = KoinHelper.shared.tryItViewModel
        self.viewModel = vm

        let adapter = FlowAdapter<TryItState>(flow: vm.state)
        self.state = adapter.currentValue
        cancellable = adapter.subscribe { [weak self] newState in
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func analyzePhoto(imageData: Data) {
        viewModel.onIntent(intent: TryItIntentAnalyzePhoto(imageBytes: imageData.toKotlinByteArray()))
    }

    func reset() {
        viewModel.onIntent(intent: TryItIntentReset())
    }

    func selectCategory(_ category: GarmentCategory) {
        viewModel.onIntent(intent: TryItIntentSelectCategory(category: category))
    }

    func generateTryOn(imageData: Data) {
        viewModel.onIntent(intent: TryItIntentGenerateTryOn(garmentBytes: imageData.toKotlinByteArray()))
    }

    func resetTryOn() {
        viewModel.onIntent(intent: TryItIntentResetTryOn())
    }

    func setPersonPhoto(imageData: Data) {
        viewModel.onIntent(intent: TryItIntentSetPersonPhoto(imageBytes: imageData.toKotlinByteArray()))
    }

    /// Signals that a photo arrived from the share sheet; the bytes stay on the Swift side.
    func receiveSharedPhoto() {
        viewModel.onIntent(intent: TryItIntentReceiveSharedPhoto())
    }

    func chooseFeature(_ feature: TryItFeature) {
        viewModel.onIntent(intent: TryItIntentChooseFeature(feature: feature))
    }

    func clearFeatureFocus() {
        viewModel.onIntent(intent: TryItIntentClearFeatureFocus())
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
