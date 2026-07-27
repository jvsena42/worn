package com.github.worn.domain.model

/** The two independently unlockable Try It features, each gated on its own credential. */
enum class TryItFeature {
    /** Claude-powered "does this fit my wardrobe" analysis. */
    ANALYSIS,

    /** YouCam-powered rendering of the garment onto the user's model photo. */
    VIRTUAL_TRY_ON,
}
