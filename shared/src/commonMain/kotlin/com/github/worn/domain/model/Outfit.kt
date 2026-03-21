package com.github.worn.domain.model

data class Outfit(
    val id: String,
    val name: String,
    val itemIds: List<String>,
    val createdAt: Long,
)
