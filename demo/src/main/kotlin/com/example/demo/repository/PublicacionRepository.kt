package com.example.demo.repository

import com.example.demo.model.entities.Publicacion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PublicacionRepository: JpaRepository<Publicacion, Long> {
    fun findByAccountId(accountId: String): MutableList<Publicacion>
    fun findByUuid(publicacionId: UUID): Optional<Publicacion>
}