package com.github.worn.di

import com.github.worn.data.source.local.DatabaseDriverFactory
import com.github.worn.data.source.local.PhotoFileStorage
import com.github.worn.util.secret.AndroidSecretStore
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

val androidModule = module {
    single { DatabaseDriverFactory(get()) }
    single { get<DatabaseDriverFactory>().create() }
    single { PhotoFileStorage(get()) }
    single<SecretStore> { AndroidSecretStore(get()) }
    single { HttpClient(OkHttp) }
    single<CoroutineContext> { Dispatchers.IO }
}
