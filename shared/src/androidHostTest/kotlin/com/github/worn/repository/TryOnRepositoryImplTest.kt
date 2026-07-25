package com.github.worn.repository

import com.github.worn.data.repository.TryOnRepositoryImpl
import com.github.worn.data.source.remote.YouCamApiClient
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TryOnRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val youCamClient = mockk<YouCamApiClient>()
    private val settingsRepository = mockk<SettingsRepository>()

    private lateinit var repository: TryOnRepositoryImpl

    private val garmentBytes = byteArrayOf(1, 2, 3)
    private val personBytes = byteArrayOf(4, 5, 6)
    private val resultBytes = byteArrayOf(7, 8, 9)

    @BeforeTest
    fun setup() {
        repository = TryOnRepositoryImpl(youCamClient, settingsRepository, testDispatcher)
    }

    @Test
    fun `generateTryOn returns the generated image on success`() = runTest {
        coEvery { settingsRepository.getModelPhoto() } returns Result.success(personBytes)
        coEvery { youCamClient.tryOn(personBytes, garmentBytes, GarmentCategory.TOP) } returns resultBytes

        val result = repository.generateTryOn(garmentBytes, GarmentCategory.TOP)

        assertTrue(result.isSuccess)
        assertContentEquals(resultBytes, result.getOrThrow())
    }

    @Test
    fun `generateTryOn fails when no model photo is saved`() = runTest {
        coEvery { settingsRepository.getModelPhoto() } returns Result.success(null)

        val result = repository.generateTryOn(garmentBytes, GarmentCategory.TOP)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("model photo", ignoreCase = true) == true)
        coVerify(exactly = 0) { youCamClient.tryOn(any(), any(), any()) }
    }

    @Test
    fun `generateTryOn forwards the selected category to the client`() = runTest {
        coEvery { settingsRepository.getModelPhoto() } returns Result.success(personBytes)
        coEvery { youCamClient.tryOn(any(), any(), any()) } returns resultBytes

        repository.generateTryOn(garmentBytes, GarmentCategory.SHOES)

        coVerify { youCamClient.tryOn(personBytes, garmentBytes, GarmentCategory.SHOES) }
    }
}
