package com.github.worn.domain.model

data class GapRecommendation(
    val itemName: String,
    val category: String,
    val pairingCount: Int,
    val subcategory: Subcategory? = null,
    val colors: List<String> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val fit: Fit? = null,
    val material: Material? = null,
    val mappedCategory: Category = Category.TOP,
)

/**
 * Drops every recommendation for a [Subcategory] the wardrobe already covers.
 *
 * Applied to the AI list as well as the capsule fallback. The model is given the wardrobe but is
 * not forbidden from suggesting something already in it, and re-asking after every add would cost
 * one paid request per item — filtering here keeps both lists honest for free, and reactively.
 *
 * Matching is on [Subcategory] alone, deliberately ignoring [GapRecommendation.mappedCategory]:
 * the two can disagree (the "Navy zip-up hoodie" gap is OUTERWEAR while `subcategoriesFor` files
 * HOODIE under TOP), so pairing them would resurrect suggestions the user has already satisfied.
 *
 * A recommendation with no subcategory is kept. [GapRecommendation.subcategory] is null whenever
 * the AI omitted the field or sent a value we don't model — lenient parsing nulls it — so there is
 * nothing to compare against; dropping it would hide a legitimate suggestion, whereas keeping it
 * can at worst repeat one.
 */
fun List<GapRecommendation>.excludingOwned(ownedItems: List<ClothingItem>): List<GapRecommendation> {
    val owned = ownedItems.mapNotNullTo(mutableSetOf()) { it.subcategory }
    return filter { recommendation ->
        recommendation.subcategory?.let { it !in owned } ?: true
    }
}
