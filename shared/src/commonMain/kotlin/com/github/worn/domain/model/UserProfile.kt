package com.github.worn.domain.model

enum class BodyType {
    SLIM,
    ATHLETIC,
    AVERAGE,
    STOCKY,
    SHORT,
    TALL_AND_SLIM,
    BIG_AND_TALL,
}

enum class StyleProfile {
    CLASSIC,
    CASUAL,
    STREETWEAR,
    SMART_CASUAL,
    MINIMALIST,
}

enum class AgeRange {
    AGE_18_25,
    AGE_26_35,
    AGE_36_45,
    AGE_46_PLUS,
}

enum class Climate {
    TROPICAL,
    TEMPERATE,
    COLD,
    MIXED,
}

enum class Lifestyle {
    WORK_OFFICE,
    WORK_MANUAL,
    SOCIAL,
    SPORTS,
    FORMAL_EVENTS,
}

data class UserProfile(
    val bodyType: BodyType? = null,
    val styleProfile: StyleProfile? = null,
    val ageRange: AgeRange? = null,
    val climate: Climate? = null,
    val lifestyles: Set<Lifestyle> = emptySet(),
)
