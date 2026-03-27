package com.github.worn.repository

import com.github.worn.data.source.remote.GapRecommendationJson
import com.github.worn.data.source.remote.toDomain
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GapRecommendationParsingTest {

    @Test
    fun `toDomain parses full response with all fields`() {
        val json = GapRecommendationJson(
            itemName = "White crew-neck tee",
            category = "BASICS",
            pairingCount = 12,
            subcategory = "T_SHIRT",
            colors = listOf("White"),
            seasons = listOf("SPRING", "SUMMER", "FALL", "WINTER"),
            fit = "SLIM_FIT",
            material = "COTTON",
        )

        val result = json.toDomain()

        assertEquals("White crew-neck tee", result.itemName)
        assertEquals("BASICS", result.category)
        assertEquals(12, result.pairingCount)
        assertEquals(Subcategory.T_SHIRT, result.subcategory)
        assertEquals(listOf("White"), result.colors)
        assertEquals(Season.entries.toList(), result.seasons)
        assertEquals(Fit.SLIM_FIT, result.fit)
        assertEquals(Material.COTTON, result.material)
        assertEquals(Category.TOP, result.mappedCategory)
    }

    @Test
    fun `toDomain handles missing optional fields`() {
        val json = GapRecommendationJson(
            itemName = "Navy hoodie",
            category = "LAYERING",
            pairingCount = 8,
        )

        val result = json.toDomain()

        assertEquals("Navy hoodie", result.itemName)
        assertEquals(8, result.pairingCount)
        assertNull(result.subcategory)
        assertTrue(result.colors.isEmpty())
        assertTrue(result.seasons.isEmpty())
        assertNull(result.fit)
        assertNull(result.material)
        assertEquals(Category.OUTERWEAR, result.mappedCategory)
    }

    @Test
    fun `toDomain handles invalid enum values gracefully`() {
        val json = GapRecommendationJson(
            itemName = "Custom item",
            category = "TOPS",
            pairingCount = 5,
            subcategory = "INVALID_TYPE",
            fit = "EXTRA_SLIM",
            material = "POLYESTER",
            seasons = listOf("SPRING", "INVALID_SEASON"),
        )

        val result = json.toDomain()

        assertNull(result.subcategory)
        assertNull(result.fit)
        assertNull(result.material)
        assertEquals(listOf(Season.SPRING), result.seasons)
    }

    @Test
    fun `toDomain maps display categories correctly`() {
        val categories = mapOf(
            "BASICS" to Category.TOP,
            "TOPS" to Category.TOP,
            "LAYERING" to Category.OUTERWEAR,
            "BOTTOMS" to Category.BOTTOM,
            "SHOES" to Category.SHOES,
            "ACCESSORIES" to Category.ACCESSORY,
        )

        categories.forEach { (displayCategory, expectedCategory) ->
            val json = GapRecommendationJson(
                itemName = "Test",
                category = displayCategory,
                pairingCount = 1,
            )
            assertEquals(
                expectedCategory,
                json.toDomain().mappedCategory,
                "Failed for category: $displayCategory",
            )
        }
    }

    @Test
    fun `capsule wardrobe suggestions are properly structured`() {
        assertTrue(capsuleWardrobeSuggestions.isNotEmpty())

        capsuleWardrobeSuggestions.forEach { suggestion ->
            assertTrue(suggestion.itemName.isNotBlank(), "Item name should not be blank")
            assertTrue(suggestion.category.isNotBlank(), "Category should not be blank")
            assertTrue(suggestion.seasons.isNotEmpty(), "Seasons should not be empty")
            assertTrue(suggestion.colors.isNotEmpty(), "Colors should not be empty")
        }
    }

    @Test
    fun `capsule wardrobe filtering excludes owned subcategories`() {
        val ownedSubcategories = setOf(Subcategory.JEANS, Subcategory.T_SHIRT, Subcategory.SNEAKERS)

        val filtered = capsuleWardrobeSuggestions.filter { it.subcategory !in ownedSubcategories }

        assertTrue(filtered.none { it.subcategory == Subcategory.JEANS })
        assertTrue(filtered.none { it.subcategory == Subcategory.T_SHIRT })
        assertTrue(filtered.none { it.subcategory == Subcategory.SNEAKERS })
        assertTrue(filtered.any { it.subcategory == Subcategory.CHINOS })
        assertTrue(filtered.any { it.subcategory == Subcategory.HENLEY })
    }
}
