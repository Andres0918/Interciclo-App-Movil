package com.example.demo.model.responses

import java.util.UUID

data class PublicacionResponse(
    val accountId: String,
    val uuid: String,
    val description: String,
    val likes: Int,
    val comentarios: List<String>,
)
