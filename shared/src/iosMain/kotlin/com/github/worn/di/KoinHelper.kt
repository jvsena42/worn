package com.github.worn.di

import com.github.worn.data.source.image.BackgroundRemover
import com.github.worn.presentation.viewmodel.GapsViewModel
import com.github.worn.presentation.viewmodel.OutfitViewModel
import com.github.worn.presentation.viewmodel.SettingsViewModel
import com.github.worn.presentation.viewmodel.TryItViewModel
import com.github.worn.presentation.viewmodel.WardrobeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}

/**
 * Typed entry point into Koin for Swift.
 *
 * Koin's own types (`Koin`, `Scope`, `KoinComponent`) come from a dependency that is not part of
 * the `Shared` framework's exported API, so they never reach the generated Objective-C header and
 * Swift cannot call `koin.get(...)` directly. Exposing one property per dependency keeps every
 * type in the signature a shared type, which does get exported.
 */
object KoinHelper : KoinComponent {
    val wardrobeViewModel: WardrobeViewModel get() = get()
    val outfitViewModel: OutfitViewModel get() = get()
    val gapsViewModel: GapsViewModel get() = get()
    val tryItViewModel: TryItViewModel get() = get()
    val settingsViewModel: SettingsViewModel get() = get()
    val backgroundRemover: BackgroundRemover get() = get()
}
