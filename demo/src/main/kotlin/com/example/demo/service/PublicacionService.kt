package com.example.demo.service

import com.example.demo.model.entities.Publicacion
import com.example.demo.model.request.PublicacionRequest
import com.example.demo.model.responses.PublicacionResponse
import com.example.demo.repository.PublicacionRepository
import org.bouncycastle.asn1.x500.style.RFC4519Style.description
import org.bouncycastle.asn1.x509.ObjectDigestInfo.publicKey
import org.springframework.data.rest.webmvc.ResourceNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PublicacionService (
    private val publicacionRepository: PublicacionRepository
){
    fun crearPublicacion(accountId: UUID, request: PublicacionRequest): PublicacionResponse {
        val publicacion = Publicacion(
            accountId = accountId.toString(),
            description = request.descripcion,
            likes = 0,
            comentarios = mutableListOf()
        )

        val publicacionGuardada = publicacionRepository.save(publicacion)

        return PublicacionResponse(
            accountId = publicacionGuardada.accountId,
            uuid = publicacionGuardada.uuid,
            description = publicacionGuardada.description!!,
            likes = publicacionGuardada.likes!!,
            comentarios = publicacionGuardada.comentarios
        )
    }

    fun getPublicacionesByAccountId(accountId: String): List<PublicacionResponse> {
        val publicaciones = publicacionRepository.findByAccountId(accountId)

        return publicaciones.map { toResponse(it) }
    }

    fun getPublicacionById(publicacionId: UUID): PublicacionResponse? {
        val publicacion = publicacionRepository.findByUuid(publicacionId).orElseThrow {
            throw ResourceNotFoundException()
        }

        return toResponse(publicacion)
    }

    fun darLike(publicacionId: UUID): PublicacionResponse {
        val publicacion = publicacionRepository.findByUuid(publicacionId).orElseThrow {
            throw ResourceNotFoundException()
        }

        publicacion.likes = publicacion.likes!! + 1

        val guardada = publicacionRepository.save(publicacion)

        return this.toResponse(guardada)
    }

    fun comentarPublicacion(publicacionId: UUID, comentario: String): PublicacionResponse {
        val publicacion = publicacionRepository.findByUuid(publicacionId).orElseThrow {
            throw ResourceNotFoundException()
        }

        publicacion.comentarios.add(comentario)

        val guardada = publicacionRepository.save(publicacion)
        return this.toResponse(guardada)
    }

    fun cambiarDescripcionDePublicacion(publicacionId: UUID, descripcion: String): PublicacionResponse {
        val publicacion = publicacionRepository.findByUuid(publicacionId).orElseThrow {
            throw ResourceNotFoundException()
        }

        publicacion.description = descripcion

        val guardada = publicacionRepository.save(publicacion)
        return this.toResponse(guardada)
    }

    private fun toResponse(publicacion: Publicacion): PublicacionResponse {
        return PublicacionResponse(
            accountId = publicacion.accountId,
            uuid = publicacion.uuid,
            description = publicacion.description!!,
            likes = 0,
            comentarios = publicacion.comentarios
        )
    }
}