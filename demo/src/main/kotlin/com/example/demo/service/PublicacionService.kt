package com.example.demo.service

import com.example.demo.model.entities.Publicacion
import com.example.demo.model.request.PublicacionRequest
import com.example.demo.model.responses.PublicacionResponse
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FieldValue
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PublicacionService(
    private val firestore: Firestore
) {
    private val collectionName = "publicaciones"

    fun crearPublicacion(accountId: UUID, request: PublicacionRequest): PublicacionResponse {
        val uuid = UUID.randomUUID().toString()
        val publicacion = Publicacion(
            accountId = accountId.toString(),
            uuid = uuid,
            description = request.descripcion,
            likes = 0,
            comentarios = mutableListOf()
        )

        firestore.collection(collectionName)
            .document(uuid.toString())
            .set(publicacion)
            .get()

        return toResponse(publicacion)
    }

    fun getPublicacionesByAccountId(accountId: String): List<PublicacionResponse> {
        val snapshot = firestore.collection(collectionName)
            .whereEqualTo("accountId", accountId)
            .get()
            .get()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Publicacion::class.java)?.let { toResponse(it) }
        }
    }

    fun getPublicacionById(publicacionId: UUID): PublicacionResponse {
        val docRef = firestore.collection(collectionName).document(publicacionId.toString())
        val snapshot = docRef.get().get()

        if (!snapshot.exists()) {
            throw IllegalArgumentException("Publicacion does not exist")
        }

        val publicacion = snapshot.toObject(Publicacion::class.java)
            ?: throw IllegalArgumentException("Error al deserializar publicación")

        return toResponse(publicacion)
    }

    fun darLike(publicacionId: UUID): PublicacionResponse {
        val docRef = firestore.collection(collectionName).document(publicacionId.toString())

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef).get()

            if (!snapshot.exists()) {
                throw IllegalArgumentException("Publicación no encontrada")
            }

            val likesActuales = snapshot.getLong("likes") ?: 0
            transaction.update(docRef, "likes", likesActuales + 1)
        }.get()

        return getPublicacionById(publicacionId)
    }

    fun quitLike(publicacionId: UUID): PublicacionResponse {
        val docRef = firestore.collection(collectionName).document(publicacionId.toString())

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef).get()

            if (!snapshot.exists()) {
                throw IllegalArgumentException("Publicación no encontrada")
            }

            val likesActuales = snapshot.getLong("likes") ?: 0
            if (likesActuales > 0) {
                transaction.update(docRef, "likes", likesActuales - 1)
            }
        }.get()

        return getPublicacionById(publicacionId)
    }

    fun comentarPublicacion(publicacionId: UUID, comentario: String): PublicacionResponse {
        val docRef = firestore.collection(collectionName).document(publicacionId.toString())

        docRef.update("comentarios", FieldValue.arrayUnion(comentario)).get()

        return getPublicacionById(publicacionId)
    }

    fun cambiarDescripcionDePublicacion(publicacionId: UUID, descripcion: String): PublicacionResponse {
        val docRef = firestore.collection(collectionName).document(publicacionId.toString())

        val snapshot = docRef.get().get()
        if (!snapshot.exists()) {
            throw IllegalArgumentException("Publicación no encontrada")
        }

        docRef.update("description", descripcion).get()

        return getPublicacionById(publicacionId)
    }

    private fun toResponse(publicacion: Publicacion): PublicacionResponse {
        return PublicacionResponse(
            accountId = publicacion.accountId,
            uuid = publicacion.uuid,
            description = publicacion.description ?: "",
            likes = publicacion.likes ?: 0,
            comentarios = publicacion.comentarios
        )
    }
}