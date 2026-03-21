package com.github.worn.data.source.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.github.worn.data.source.local.db.WardrobeDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = WardrobeDatabase.Schema,
            name = "wardrobe.db",
        )
}
