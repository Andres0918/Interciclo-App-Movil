package com.example.demo.model.responses

import java.util.UUID

data class PublicacionResponse(
    val uuid: String,
    val accountId: String,
    val description: String,
    val imageUrl: String?,
    val filterApplied: String?,
    val likes: Int,
    val comentarios: List<String>,
    val createdAt: Long
)
