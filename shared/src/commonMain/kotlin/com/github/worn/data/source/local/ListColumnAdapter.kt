package com.github.worn.data.source.local

import app.cash.sqldelight.ColumnAdapter

val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> =
        if (databaseValue.isEmpty()) emptyList()
        else databaseValue.split(",")

    override fun encode(value: List<String>): String =
        value.joinToString(",")
}
