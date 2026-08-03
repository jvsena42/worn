package com.github.worn.model

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import com.github.worn.domain.model.subcategoriesFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubcategoryTest {

    @Test
    fun `every subcategory belongs to exactly one category`() {
        val mapped = Category.entries.flatMap { subcategoriesFor(it) }

        assertEquals(mapped.size, mapped.toSet().size, "a subcategory is listed under more than one category")
        assertEquals(Subcategory.entries.toSet(), mapped.toSet(), "a subcategory is missing from subcategoriesFor()")
    }

    @Test
    fun `hoodie and sweater are outerwear`() {
        val outerwear = subcategoriesFor(Category.OUTERWEAR)

        assertTrue(Subcategory.HOODIE in outerwear)
        assertTrue(Subcategory.SWEATER in outerwear)
    }

    @Test
    fun `capsule wardrobe suggestions pair each subcategory with its own category`() {
        capsuleWardrobeSuggestions.forEach { suggestion ->
            val subcategory = suggestion.subcategory ?: return@forEach
            assertTrue(
                subcategory in subcategoriesFor(suggestion.mappedCategory),
                "${suggestion.itemName}: $subcategory is not a ${suggestion.mappedCategory} subcategory",
            )
        }
    }
}
