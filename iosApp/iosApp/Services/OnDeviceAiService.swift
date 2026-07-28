import Foundation
import Shared
import UIKit

#if canImport(FoundationModels)
import FoundationModels
#endif

/// Apple Intelligence implementation of the shared `OnDeviceAiBridge`.
///
/// This lives in Swift rather than `iosMain` because `FoundationModels` exposes no Objective-C
/// interface, and Kotlin/Native interop only reaches C and Objective-C. Everything else about the
/// on-device provider — prompts, JSON parsing, routing — stays in shared Kotlin.
///
/// The framework needs iOS 26 while the app deploys back to 18.2, so every use is behind
/// `#available` and the framework is weak-linked (see `Config.xcconfig`).
///
/// Error messages are plain English here to match `AndroidOnDeviceAiEngine` and `ClaudeApiClient`,
/// which also raise unlocalized messages from the data layer.
final class OnDeviceAiService: NSObject, OnDeviceAiBridge {

    func availability(onResult: @escaping (OnDeviceAiAvailabilityToken) -> Void) {
        #if canImport(FoundationModels)
        guard #available(iOS 26, *) else {
            onResult(.unsupportedOs)
            return
        }
        switch SystemLanguageModel.default.availability {
        case .available:
            onResult(.available)
        case .unavailable(.deviceNotEligible):
            onResult(.unsupportedDevice)
        case .unavailable(.appleIntelligenceNotEnabled):
            onResult(.disabledByUser)
        case .unavailable(.modelNotReady):
            onResult(.downloadable)
        case .unavailable:
            onResult(.unknown)
        }
        #else
        onResult(.unsupportedOs)
        #endif
    }

    func generate(
        systemPrompt: String,
        userText: String,
        imageBytes: KotlinByteArray?,
        onResult: @escaping (String?, String?) -> Void
    ) {
        #if canImport(FoundationModels)
        guard #available(iOS 26, *) else {
            onResult(nil, Self.unsupportedMessage)
            return
        }
        // FoundationModels is text-only as of iOS 26.5: `Prompt` accepts `PromptRepresentable`
        // values and the framework exposes no image or attachment content type. Callers that need
        // vision (photo analysis) have to stay on Claude; text-only callers (gap recommendations)
        // run fully on device. Android differs here — Gemini Nano does take an `ImagePart`.
        if imageBytes != nil {
            onResult(nil, Self.noVisionMessage)
            return
        }
        Task {
            do {
                let session = LanguageModelSession(instructions: systemPrompt)
                let content = try await session.respond(to: userText).content
                onResult(content, nil)
            } catch {
                onResult(nil, error.localizedDescription)
            }
        }
        #else
        onResult(nil, Self.unsupportedMessage)
        #endif
    }

    private static let unsupportedMessage =
        "On-device AI isn't available on this device. Turn it off in Settings."

    private static let noVisionMessage =
        "On-device AI can't analyse photos on iOS. Connect a Claude API key in Settings."
}
