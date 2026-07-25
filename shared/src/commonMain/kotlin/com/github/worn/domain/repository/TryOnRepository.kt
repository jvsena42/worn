package com.github.worn.domain.repository

import com.github.worn.domain.model.GarmentCategory

interface TryOnRepository {
    /**
     * Renders [garmentBytes] onto the user's saved model photo via YouCam, returning the generated
     * JPEG. Fails if no model photo is saved or the remote call errors.
     */
    suspend fun generateTryOn(garmentBytes: ByteArray, category: GarmentCategory): Result<ByteArray>
}
