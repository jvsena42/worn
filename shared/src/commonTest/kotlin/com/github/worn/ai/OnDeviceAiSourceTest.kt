package com.github.worn.ai

import com.github.worn.data.source.ai.OnDeviceAiSource
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.model.BodyType
import com.github.worn.fake.FakeOnDeviceAiEngine
import com.github.worn.fake.clothingItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnDeviceAiSourceTest {

    private val engine = FakeOnDeviceAiEngine()
    private val source = OnDeviceAiSource(engine)

    // region analyzeImage

    @Test
    fun `analyzeImage maps a well-formed reply to the domain model`() = runTest {
        engine.response = """
            {
              "description": "A navy wool overcoat",
              "suggested_category": "OUTERWEAR",
              "colors": ["navy"],
              "seasons": ["FALL", "WINTER"],
              "tags": ["formal", "warm"],
              "suggested_subcategory": "COAT",
              "suggested_fit": "REGULAR",
              "suggested_material": "WOOL"
            }
        """.trimIndent()

        val result = source.analyzeImage(byteArrayOf(1, 2, 3))

        assertEquals("A navy wool overcoat", result.description)
        assertEquals(Category.OUTERWEAR, result.suggestedCategory)
        assertEquals(listOf("navy"), result.colors)
        assertEquals(listOf(Season.FALL, Season.WINTER), result.seasons)
        assertEquals(listOf("formal", "warm"), result.tags)
        assertEquals(Subcategory.COAT, result.suggestedSubcategory)
        assertEquals(Fit.REGULAR, result.suggestedFit)
        assertEquals(Material.WOOL, result.suggestedMaterial)
    }

    @Test
    fun `analyzeImage passes the image through to the engine`() = runTest {
        engine.response = MINIMAL_ANALYSIS
        val bytes = byteArrayOf(9, 8, 7)

        source.analyzeImage(bytes)

        assertEquals(bytes, engine.lastImageBytes)
    }

    @Test
    fun `analyzeImage tells the model to skip markdown`() = runTest {
        engine.response = MINIMAL_ANALYSIS

        source.analyzeImage(byteArrayOf(1))

        assertContains(engine.lastSystemPrompt.orEmpty(), "Output raw JSON only")
    }

    /** Small models fence their JSON despite the instruction; the parser has to cope. */
    @Test
    fun `analyzeImage unwraps a fenced reply`() = runTest {
        engine.response = "```json\n$MINIMAL_ANALYSIS\n```"

        val result = source.analyzeImage(byteArrayOf(1))

        assertEquals("A plain tee", result.description)
    }

    @Test
    fun `analyzeImage falls back for unknown enum values rather than failing`() = runTest {
        engine.response = """
            {
              "description": "Something new",
              "suggested_category": "SPACESUIT",
              "colors": [],
              "seasons": ["MONSOON", "SUMMER"],
              "tags": [],
              "suggested_fit": "SNUG"
            }
        """.trimIndent()

        val result = source.analyzeImage(byteArrayOf(1))

        assertEquals(Category.TOP, result.suggestedCategory)
        assertEquals(listOf(Season.SUMMER), result.seasons)
        assertNull(result.suggestedFit)
    }

    @Test
    fun `analyzeImage fails when the reply is not JSON`() = runTest {
        engine.response = "Sure! Here is a description of the shirt."

        assertFailsWith<Exception> { source.analyzeImage(byteArrayOf(1)) }
    }

    @Test
    fun `analyzeImage surfaces engine failures without retrying`() = runTest {
        engine.failure = IllegalStateException("Model unavailable")

        val error = assertFailsWith<IllegalStateException> { source.analyzeImage(byteArrayOf(1)) }

        assertEquals("Model unavailable", error.message)
        assertEquals(1, engine.generateCount)
    }

    // endregion

    // region getGapRecommendations

    @Test
    fun `getGapRecommendations maps a JSON array to recommendations`() = runTest {
        engine.response = """
            [{"item_name": "White crew tee", "category": "BASICS", "pairing_count": 12,
              "subcategory": "T_SHIRT", "colors": ["white"], "seasons": ["SUMMER"]}]
        """.trimIndent()

        val result = source.getGapRecommendations(listOf(clothingItem()))

        assertEquals(1, result.size)
        assertEquals("White crew tee", result.first().itemName)
        assertEquals(Subcategory.T_SHIRT, result.first().subcategory)
        assertEquals(Category.TOP, result.first().mappedCategory)
    }

    @Test
    fun `getGapRecommendations sends no image and summarizes the wardrobe`() = runTest {
        engine.response = "[]"

        source.getGapRecommendations(listOf(clothingItem(name = "Blue T-Shirt")))

        assertNull(engine.lastImageBytes)
        assertContains(engine.lastUserText.orEmpty(), "Blue T-Shirt")
    }

    @Test
    fun `getGapRecommendations includes the user profile when present`() = runTest {
        engine.response = "[]"

        source.getGapRecommendations(
            items = listOf(clothingItem()),
            userProfile = UserProfile(bodyType = BodyType.ATHLETIC),
        )

        assertContains(engine.lastUserText.orEmpty(), "Body type: athletic")
    }

    @Test
    fun `getGapRecommendations omits profile context when there is no profile`() = runTest {
        engine.response = "[]"

        source.getGapRecommendations(items = listOf(clothingItem()), userProfile = null)

        val userText = assertNotNull(engine.lastUserText)
        assertTrue(userText.startsWith("My wardrobe:"), "Unexpected prompt: $userText")
    }

    // endregion

    private companion object {
        val MINIMAL_ANALYSIS = """
            {
              "description": "A plain tee",
              "suggested_category": "TOP",
              "colors": ["white"],
              "seasons": ["SUMMER"],
              "tags": []
            }
        """.trimIndent()
    }
}
