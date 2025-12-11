package com.example.demo.service

import com.example.demo.model.entities.Publicacion
import com.example.demo.model.responses.PublicacionResponse

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

@Service

class PublicacionService(
    private val firestore: Firestore,
    private val storageService: StorageService,
    private val cudaClient: CudaServiceClient
) {
    private val collectionName = "publicaciones"
    private val log = LoggerFactory.getLogger(PublicacionService::class.java)

    fun crearPublicacion(
        accountId: String,
        descripcion: String,
        imagen: ByteArray,
        filterName: String?
    ): Mono<PublicacionResponse> {
        log.info("Iniciando publicacion")
        return Mono.just(imagen)
            .flatMap { imageBytes ->
                // Si hay filtro, procesar con CUDA
                if (!filterName.isNullOrBlank() && filterName != "none") {
                    cudaClient.processImage(imageBytes, filterName)
                } else {
                    Mono.just(imageBytes)
                }
            }
            .flatMap { processedBytes ->
                // Subir a Firebase Storage
                storageService.uploadImage(processedBytes, "publicaciones")
                    .subscribeOn(Schedulers.boundedElastic())
            }
            .flatMap { imageUrl ->
                // Guardar en Firestore
                val uuid = UUID.randomUUID().toString()
                val publicacion = Publicacion(
                    uuid = uuid,
                    accountId = accountId,
                    description = descripcion,
                    imageUrl = imageUrl,
                    filterApplied = filterName,
                    likes = 0,
                    comentarios = mutableListOf(),
                    createdAt = System.currentTimeMillis()
                )

                Mono.fromCallable {
                    firestore.collection(collectionName)
                        .document(uuid)
                        .set(publicacion)
                        .get()
                    publicacion
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .map { toResponse(it) }
    }

    fun getPublicacionesByAccount(accountId: String): Flux<PublicacionResponse> {
        return Mono.fromCallable {
            firestore.collection(collectionName)
                .whereEqualTo("accountId", accountId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .documents
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany { Flux.fromIterable(it) }
            .mapNotNull { doc ->
                doc.toObject(Publicacion::class.java)?.let { toResponse(it) }
            }
    }

    fun getFeed(limit: Int = 50): Flux<PublicacionResponse> {
        return Mono.fromCallable {
            firestore.collection(collectionName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get()
                .documents
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany { Flux.fromIterable(it) }
            .mapNotNull { doc ->
                doc.toObject(Publicacion::class.java)?.let { toResponse(it) }
            }
    }

    fun darLike(publicacionId: String): Mono<PublicacionResponse> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef).get()
                val likesActuales = snapshot.getLong("likes") ?: 0
                transaction.update(docRef, "likes", likesActuales + 1)
            }.get()

            docRef.get().get().toObject(Publicacion::class.java)!!
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
    }

    fun quitarLike(publicacionId: String): Mono<PublicacionResponse> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef).get()
                val likesActuales = snapshot.getLong("likes") ?: 0
                if (likesActuales > 0) {
                    transaction.update(docRef, "likes", likesActuales - 1)
                }
            }.get()

            docRef.get().get().toObject(Publicacion::class.java)!!
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
    }

    fun agregarComentario(publicacionId: String, comentario: String): Mono<PublicacionResponse> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef).get()
                val comentarios = snapshot.get("comentarios") as? MutableList<String> ?: mutableListOf()
                comentarios.add(comentario)
                transaction.update(docRef, "comentarios", comentarios)
            }.get()

            docRef.get().get().toObject(Publicacion::class.java)!!
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
    }

    fun getPublicacionById(publicacionId: String): Mono<PublicacionResponse> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            val snapshot = docRef.get().get()

            if (!snapshot.exists()) {
                throw RuntimeException("Publicación no encontrada")
            }

            snapshot.toObject(Publicacion::class.java)!!
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
    }

    fun cambiarDescripcion(publicacionId: String, descripcion: String): Mono<PublicacionResponse> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            docRef.update("description", descripcion).get()
            docRef.get().get().toObject(Publicacion::class.java)!!
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
    }

    fun eliminarPublicacion(publicacionId: String): Mono<Boolean> {
        return Mono.fromCallable {
            val docRef = firestore.collection(collectionName).document(publicacionId)
            val snapshot = docRef.get().get()

            if (!snapshot.exists()) {
                throw RuntimeException("Publicación no encontrada")
            }

            val publicacion = snapshot.toObject(Publicacion::class.java)!!

            // Eliminar imagen de Storage si existe
            publicacion.imageUrl?.let {
                storageService.deleteImage(it)
            }

            // Eliminar documento
            docRef.delete().get()
            true
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun toResponse(publicacion: Publicacion): PublicacionResponse {
        return PublicacionResponse(
            uuid = publicacion.uuid,
            accountId = publicacion.accountId,
            description = publicacion.description ?: "",
            imageUrl = publicacion.imageUrl,
            filterApplied = publicacion.filterApplied,
            likes = publicacion.likes,
            comentarios = publicacion.comentarios,
            createdAt = publicacion.createdAt
        )
    }
}