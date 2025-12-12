package com.example.demo.controller

import com.example.demo.model.responses.PublicacionResponse
import com.example.demo.service.PublicacionService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/app/publicacion")
class PublicacionController(
    private val publicacionService: PublicacionService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun crearPublicacion(
        @RequestPart("descripcion") descripcion: String,
        @RequestPart("imagen") imagen: FilePart,
        @RequestPart("filter", required = false) filter: String?,
        @RequestHeader accountId: UUID
    ): Mono<PublicacionResponse> {
        // Convertir FilePart a ByteArray reactivamente
        return imagen.content()
            .reduce(ByteArray(0)) { acc, dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer)
                acc + bytes
            }
            .flatMap { imageBytes ->
                publicacionService.crearPublicacion(
                    accountId = accountId.toString(),
                    descripcion = descripcion,
                    imagen = imageBytes,
                    filterName = filter
                )
            }
    }

    @GetMapping("/obtener/all")
    fun getPublicacionesByAccountId(
        @RequestHeader accountId: String
    ): Flux<PublicacionResponse> {
        return publicacionService.getPublicacionesByAccount(accountId)
    }

    @GetMapping("/obtener/{publicacionId}")
    fun getPublicacionById(
        @PathVariable publicacionId: String
    ): Mono<PublicacionResponse> {
        return publicacionService.getPublicacionById(publicacionId)
    }

    @GetMapping("/feed")
    fun getFeed(
        @RequestParam(defaultValue = "50") limit: Int
    ): Flux<PublicacionResponse> {
        return publicacionService.getFeed(limit)
    }

    @PutMapping("/add/like")
    fun getDarLike(
        @RequestParam publicacionId: String
    ): Mono<PublicacionResponse> {
        return publicacionService.darLike(publicacionId)
    }

    @PutMapping("/quit/like")
    fun quitLike(
        @RequestParam publicacionId: String
    ): Mono<PublicacionResponse> {
        return publicacionService.quitarLike(publicacionId)
    }

    @PutMapping("/comment/post/{publicacionId}")
    fun comentarPublicacion(
        @PathVariable publicacionId: String,
        @RequestParam comentario: String
    ): Mono<PublicacionResponse> {
        return publicacionService.agregarComentario(publicacionId, comentario)
    }

    @PutMapping("/change/description/{publicacionId}")
    fun changeDescription(
        @PathVariable publicacionId: String,
        @RequestParam description: String
    ): Mono<PublicacionResponse> {
        return publicacionService.cambiarDescripcion(publicacionId, description)
    }

    @DeleteMapping("/{publicacionId}")
    fun eliminarPublicacion(
        @PathVariable publicacionId: String
    ): Mono<Map<String, Boolean>> {
        return publicacionService.eliminarPublicacion(publicacionId)
            .map { mapOf("success" to it) }
    }
}