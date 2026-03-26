package com.github.worn.domain.model

enum class Subcategory {
    // TOP
    T_SHIRT,
    POLO,
    DRESS_SHIRT,
    HENLEY,
    SWEATER,
    HOODIE,

    // BOTTOM
    JEANS,
    CHINOS,
    TAILORED_PANTS,
    SHORTS,
    CARGO_PANTS,
    SWEATPANTS,

    // OUTERWEAR
    BOMBER,
    TRUCKER,
    PUFFER,
    BLAZER,
    COAT,
    WINDBREAKER,

    // SHOES
    SNEAKERS,
    BOOTS_MILITARY,
    BOOTS_CHELSEA,
    DERBY,
    OXFORD,
    LOAFER,
    SANDALS,

    // ACCESSORY
    WATCH,
    BELT,
    SUNGLASSES,
    HAT_CAP,
    SCARF,
    BAG_BACKPACK,
}

fun subcategoriesFor(category: Category): List<Subcategory> = when (category) {
    Category.TOP -> listOf(
        Subcategory.T_SHIRT,
        Subcategory.POLO,
        Subcategory.DRESS_SHIRT,
        Subcategory.HENLEY,
        Subcategory.SWEATER,
        Subcategory.HOODIE,
    )
    Category.BOTTOM -> listOf(
        Subcategory.JEANS,
        Subcategory.CHINOS,
        Subcategory.TAILORED_PANTS,
        Subcategory.SHORTS,
        Subcategory.CARGO_PANTS,
        Subcategory.SWEATPANTS,
    )
    Category.OUTERWEAR -> listOf(
        Subcategory.BOMBER,
        Subcategory.TRUCKER,
        Subcategory.PUFFER,
        Subcategory.BLAZER,
        Subcategory.COAT,
        Subcategory.WINDBREAKER,
    )
    Category.SHOES -> listOf(
        Subcategory.SNEAKERS,
        Subcategory.BOOTS_MILITARY,
        Subcategory.BOOTS_CHELSEA,
        Subcategory.DERBY,
        Subcategory.OXFORD,
        Subcategory.LOAFER,
        Subcategory.SANDALS,
    )
    Category.ACCESSORY -> listOf(
        Subcategory.WATCH,
        Subcategory.BELT,
        Subcategory.SUNGLASSES,
        Subcategory.HAT_CAP,
        Subcategory.SCARF,
        Subcategory.BAG_BACKPACK,
    )
}
