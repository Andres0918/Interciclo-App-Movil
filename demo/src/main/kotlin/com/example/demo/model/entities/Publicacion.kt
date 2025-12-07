package com.example.demo.model.entities

import com.google.cloud.firestore.annotation.DocumentId
import java.util.UUID

data class Publicacion(
    @DocumentId
    var uuid: String = UUID.randomUUID().toString(),

    var accountId: String = "",

    var description: String? = null,

    var likes: Int = 0,

    var comentarios: MutableList<String> = mutableListOf(),

    var imageUrl: String? = null,
    var filterApplied: String? = null,
    var createdAt: Long = System.currentTimeMillis()

) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        uuid = UUID.randomUUID().toString(),
        accountId = "",
        description = null,
        likes = 0,
        comentarios = mutableListOf()
    )
}