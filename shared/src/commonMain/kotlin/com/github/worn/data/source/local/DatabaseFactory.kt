package com.github.worn.data.source.local

import app.cash.sqldelight.db.SqlDriver
import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.data.source.local.db.ClothingItem as DbClothingItem

fun createDatabase(driver: SqlDriver): WardrobeDatabase =
    WardrobeDatabase(
        driver = driver,
        clothingItemAdapter = DbClothingItem.Adapter(
            colorsAdapter = listOfStringsAdapter,
            seasonsAdapter = listOfStringsAdapter,
            tagsAdapter = listOfStringsAdapter,
        ),
    )
