package com.github.worn.di

import com.github.worn.data.repository.OutfitRepositoryImpl
import com.github.worn.data.repository.WardrobeRepositoryImpl
import com.github.worn.data.source.local.createDatabase
import com.github.worn.data.source.remote.ClaudeApiClient
import com.github.worn.domain.repository.OutfitRepository
import com.github.worn.domain.repository.WardrobeRepository
import com.github.worn.presentation.viewmodel.OutfitViewModel
import com.github.worn.presentation.viewmodel.WardrobeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single { createDatabase(get()) }
    singleOf(::ClaudeApiClient)
    single<WardrobeRepository> {
        WardrobeRepositoryImpl(
            db = get(),
            fileStorage = get(),
            aiClient = get(),
            dispatcher = get(),
        )
    }
    single<OutfitRepository> {
        OutfitRepositoryImpl(db = get(), dispatcher = get())
    }
    factoryOf(::WardrobeViewModel)
    factoryOf(::OutfitViewModel)
}
