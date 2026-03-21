package com.github.worn.data.source.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.worn.data.source.local.db.WardrobeDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(
            schema = WardrobeDatabase.Schema,
            context = context,
            name = "wardrobe.db",
        )
}
