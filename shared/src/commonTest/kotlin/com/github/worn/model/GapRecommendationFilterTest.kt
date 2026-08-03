package com.github.worn.model

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import com.github.worn.domain.model.excludingOwned
import com.github.worn.fake.clothingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GapRecommendationFilterTest {

    private fun recommendation(name: String, subcategory: Subcategory?) = GapRecommendation(
        itemName = name,
        category = "TOPS",
        pairingCount = 0,
        subcategory = subcategory,
    )

    @Test
    fun `drops recommendations whose subcategory is already owned`() {
        val recommendations = listOf(
            recommendation("Polo shirt", Subcategory.POLO),
            recommendation("White dress shirt", Subcategory.DRESS_SHIRT),
        )

        val filtered = recommendations.excludingOwned(
            listOf(clothingItem(id = "owned-polo", subcategory = Subcategory.POLO)),
        )

        assertEquals(listOf("White dress shirt"), filtered.map { it.itemName })
    }

    @Test
    fun `keeps recommendations for subcategories that are not owned`() {
        val recommendations = listOf(recommendation("Chino pants", Subcategory.CHINOS))

        val filtered = recommendations.excludingOwned(
            listOf(clothingItem(subcategory = Subcategory.JEANS)),
        )

        assertEquals(recommendations, filtered)
    }

    @Test
    fun `keeps a recommendation with no subcategory`() {
        val recommendations = listOf(recommendation("Something the AI did not tag", null))

        val filtered = recommendations.excludingOwned(
            listOf(clothingItem(subcategory = Subcategory.POLO)),
        )

        assertEquals(recommendations, filtered)
    }

    @Test
    fun `items with no subcategory suppress nothing`() {
        val recommendations = listOf(recommendation("Polo shirt", Subcategory.POLO))

        val filtered = recommendations.excludingOwned(listOf(clothingItem(subcategory = null)))

        assertEquals(recommendations, filtered)
    }

    @Test
    fun `an empty wardrobe returns the whole list`() {
        assertEquals(
            capsuleWardrobeSuggestions,
            capsuleWardrobeSuggestions.excludingOwned(emptyList()),
        )
    }

    @Test
    fun `matches on subcategory regardless of the item category`() {
        // The capsule hoodie is mappedCategory = OUTERWEAR while the item is filed as a TOP;
        // matching on subcategory alone is what keeps the suggestion from coming back.
        val filtered = capsuleWardrobeSuggestions.excludingOwned(
            listOf(clothingItem(category = Category.TOP, subcategory = Subcategory.HOODIE)),
        )

        assertTrue(filtered.none { it.subcategory == Subcategory.HOODIE })
    }

    @Test
    fun `capsule suggestions exclude every owned subcategory`() {
        val owned = listOf(
            clothingItem(id = "owned-1", subcategory = Subcategory.JEANS),
            clothingItem(id = "owned-2", subcategory = Subcategory.T_SHIRT),
            clothingItem(id = "owned-3", subcategory = Subcategory.SNEAKERS),
        )

        val filtered = capsuleWardrobeSuggestions.excludingOwned(owned)

        assertTrue(filtered.none { it.subcategory == Subcategory.JEANS })
        assertTrue(filtered.none { it.subcategory == Subcategory.T_SHIRT })
        assertTrue(filtered.none { it.subcategory == Subcategory.SNEAKERS })
        assertTrue(filtered.any { it.subcategory == Subcategory.CHINOS })
        assertTrue(filtered.any { it.subcategory == Subcategory.HENLEY })
    }
}
