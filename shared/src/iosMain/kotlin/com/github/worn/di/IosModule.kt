package com.github.worn.di

import com.github.worn.data.source.local.DatabaseDriverFactory
import com.github.worn.data.source.local.PhotoFileStorage
import com.github.worn.util.secret.IosSecretStore
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

val iosModule = module {
    single { DatabaseDriverFactory() }
    single { get<DatabaseDriverFactory>().create() }
    single { PhotoFileStorage() }
    single<SecretStore> { IosSecretStore() }
    single { HttpClient(Darwin) }
    single<CoroutineContext> { Dispatchers.IO }
}
