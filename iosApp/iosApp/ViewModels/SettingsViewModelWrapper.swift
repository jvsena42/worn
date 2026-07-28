import Foundation
import SwiftUI
import Shared

@MainActor
class SettingsViewModelWrapper: ObservableObject {
    private let viewModel: SettingsViewModel
    private var cancellable: Cancellable?

    @Published var state: SettingsState

    init() {
        let vm = KoinHelper.shared.settingsViewModel
        self.viewModel = vm

        let adapter = FlowAdapter<SettingsState>(flow: vm.state)
        self.state = adapter.currentValue
        cancellable = adapter.subscribe { [weak self] newState in
            DispatchQueue.main.async {
                withAnimation(.easeInOut(duration: 0.3)) {
                    self?.state = newState
                }
            }
        }
    }

    func selectBodyType(_ bodyType: BodyType?) {
        viewModel.onIntent(intent: SettingsIntentSelectBodyType(bodyType: bodyType))
    }

    func selectStyleProfile(_ styleProfile: StyleProfile?) {
        viewModel.onIntent(intent: SettingsIntentSelectStyleProfile(styleProfile: styleProfile))
    }

    func selectAgeRange(_ ageRange: AgeRange?) {
        viewModel.onIntent(intent: SettingsIntentSelectAgeRange(ageRange: ageRange))
    }

    func selectClimate(_ climate: Climate?) {
        viewModel.onIntent(intent: SettingsIntentSelectClimate(climate: climate))
    }

    func toggleLifestyle(_ lifestyle: Lifestyle) {
        viewModel.onIntent(intent: SettingsIntentToggleLifestyle(lifestyle: lifestyle))
    }

    func saveApiKey(_ key: String) {
        viewModel.onIntent(intent: SettingsIntentSaveApiKey(key: key))
    }

    func clearApiKey() {
        viewModel.onIntent(intent: SettingsIntentClearApiKey())
    }

    func saveYouCamCredentials(clientId: String, clientSecret: String) {
        viewModel.onIntent(
            intent: SettingsIntentSaveYouCamCredentials(clientId: clientId, clientSecret: clientSecret)
        )
    }

    func clearYouCamCredentials() {
        viewModel.onIntent(intent: SettingsIntentClearYouCamCredentials())
    }

    func setOnDeviceAi(_ enabled: Bool) {
        viewModel.onIntent(intent: SettingsIntentSetOnDeviceAi(enabled: enabled))
    }

    deinit {
        cancellable?.cancel()
    }
}
