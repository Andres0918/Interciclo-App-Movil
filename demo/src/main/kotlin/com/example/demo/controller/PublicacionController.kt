package com.example.demo.controller

import com.example.demo.model.responses.PublicacionResponse
import com.example.demo.service.PublicacionService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping("/app/publicacion")
class PublicacionController(
    private val publicacionService: PublicacionService
) {
    private val log = LoggerFactory.getLogger(PublicacionController::class.java)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun crearPublicacion(
        @RequestPart("descripcion") descripcion: String,
        @RequestPart("filter") filter: String,
        @RequestPart("imagen") imagen: FilePart,
        @RequestHeader("X-Account-Id") accountId: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("📤 Creando publicación para cuenta: $accountId")
        log.info("📝 Descripción: $descripcion")
        log.info("🎨 Filtro: $filter")
        log.info("🖼️ Imagen: ${imagen.filename()}")

        return imagen.content()
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                bytes
            }
            .reduce { acc, bytes -> acc + bytes }
            .flatMap { imageBytes ->
                log.info("📊 Tamaño imagen: ${imageBytes.size} bytes")
                publicacionService.crearPublicacion(
                    accountId = accountId,
                    descripcion = descripcion,
                    imagen = imageBytes,
                    filterName = filter
                )
            }
            .map { ResponseEntity.ok(it) }
            .doOnError { error ->
                log.error("❌ Error creando publicación: ${error.message}", error)
            }
    }

    @GetMapping("/feed")
    fun getFeed(
        @RequestParam(defaultValue = "50") limit: Int
    ): Flux<PublicacionResponse> {
        log.info("📰 Obteniendo feed (limit: $limit)")
        return publicacionService.getFeed(limit)
    }

    @GetMapping("/add/like")
    fun addLike(
        @RequestParam publicacionId: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("❤️ Dando like a publicación: $publicacionId")
        return publicacionService.darLike(publicacionId)
            .map { ResponseEntity.ok(it) }
    }

    @PutMapping("/quit/like")
    fun quitLike(
        @RequestParam publicacionId: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("💔 Quitando like de publicación: $publicacionId")
        return publicacionService.quitarLike(publicacionId)
            .map { ResponseEntity.ok(it) }
    }

    @PutMapping("/comment/post/{postId}")
    fun comentarPost(
        @PathVariable postId: String,
        @RequestParam comentario: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("💬 Agregando comentario a publicación: $postId")
        log.info("📝 Comentario: $comentario")
        return publicacionService.agregarComentario(postId, comentario)
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/obtener/{publicacionId}")
    fun getPublicacionById(
        @PathVariable publicacionId: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("🔍 Obteniendo publicación: $publicacionId")
        return publicacionService.getPublicacionById(publicacionId)
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/obtener/all")
    fun getAllByAccount(
        @RequestHeader("X-Account-Id") accountId: String
    ): Flux<PublicacionResponse> {
        log.info("📋 Obteniendo publicaciones de cuenta: $accountId")
        return publicacionService.getPublicacionesByAccount(accountId)
    }

    @PutMapping("/change/description/{publicacionId}")
    fun cambiarDescripcion(
        @PathVariable publicacionId: String,
        @RequestParam description: String
    ): Mono<ResponseEntity<PublicacionResponse>> {
        log.info("✏️ Cambiando descripción de publicación: $publicacionId")
        return publicacionService.cambiarDescripcion(publicacionId, description)
            .map { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/{publicacionId}")
    fun eliminarPublicacion(
        @PathVariable publicacionId: String
    ): Mono<ResponseEntity<Boolean>> {
        log.info("🗑️ Eliminando publicación: $publicacionId")
        return publicacionService.eliminarPublicacion(publicacionId)
            .map { ResponseEntity.ok(it) }
    }
}