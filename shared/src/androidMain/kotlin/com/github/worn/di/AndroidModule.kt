package com.github.worn.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.github.worn.data.source.ai.AndroidOnDeviceAiEngine
import com.github.worn.data.source.ai.OnDeviceAiEngine
import com.github.worn.data.source.image.BackgroundRemover
import com.github.worn.data.source.image.ImageDownscaler
import com.github.worn.data.source.local.DatabaseDriverFactory
import com.github.worn.data.source.local.PhotoFileStorage
import com.github.worn.data.source.local.createDataStore
import com.github.worn.util.crypto.RsaEncryptor
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
    single { BackgroundRemover(get()) }
    single { ImageDownscaler(get()) }
    single<OnDeviceAiEngine> { AndroidOnDeviceAiEngine(get()) }
    single<SecretStore> { AndroidSecretStore(get()) }
    single { RsaEncryptor() }
    single { HttpClient(OkHttp) }
    single<CoroutineContext> { Dispatchers.IO }
    single<DataStore<Preferences>> { createDataStore(context = get()) }
}
