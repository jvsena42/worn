package com.github.worn.domain.model

/**
 * The kind of garment being tried on, chosen by the user in the TRY IT screen. Determines which
 * YouCam try-on endpoint is used and the `garment_category` sent to the Clothes API.
 */
enum class GarmentCategory {
    TOP,
    BOTTOM,
    FULL_BODY,
    SHOES,
}
