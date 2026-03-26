import Foundation
import Shared

@MainActor
class SettingsViewModelWrapper: ObservableObject {
    private let viewModel: SettingsViewModel
    private var cancellable: Cancellable?

    @Published var state: SettingsState

    init() {
        let koin = KoinHelper.shared.koin
        let vm = koin.get(objCClass: SettingsViewModel.self) as! SettingsViewModel
        self.viewModel = vm
        self.state = vm.state.value

        let adapter = FlowAdapter(flow: vm.state)
        cancellable = adapter.subscribe { [weak self] newState in
            guard let newState = newState as? SettingsState else { return }
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func selectBodyType(_ bodyType: BodyType?) {
        viewModel.onIntent(intent: SettingsIntent.SelectBodyType(bodyType: bodyType))
    }

    func selectStyleProfile(_ styleProfile: StyleProfile?) {
        viewModel.onIntent(intent: SettingsIntent.SelectStyleProfile(styleProfile: styleProfile))
    }

    func selectAgeRange(_ ageRange: AgeRange?) {
        viewModel.onIntent(intent: SettingsIntent.SelectAgeRange(ageRange: ageRange))
    }

    func selectClimate(_ climate: Climate?) {
        viewModel.onIntent(intent: SettingsIntent.SelectClimate(climate: climate))
    }

    func toggleLifestyle(_ lifestyle: Lifestyle) {
        viewModel.onIntent(intent: SettingsIntent.ToggleLifestyle(lifestyle: lifestyle))
    }

    func saveApiKey(_ key: String) {
        viewModel.onIntent(intent: SettingsIntent.SaveApiKey(key: key))
    }

    func clearApiKey() {
        viewModel.onIntent(intent: SettingsIntent.ClearApiKey())
    }

    deinit {
        cancellable?.cancel()
    }
}
