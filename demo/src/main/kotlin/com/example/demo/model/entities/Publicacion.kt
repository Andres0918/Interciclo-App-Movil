package com.example.demo.model.entities

import jakarta.persistence.*
import java.util.UUID

@Entity
class Publicacion (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var uuid: UUID = UUID.randomUUID(),

    var accountId: String ,

    var description: String? = null,

    var likes: Int? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comentarios")
    var comentarios: MutableList<String> = mutableListOf(),

    ) {
}