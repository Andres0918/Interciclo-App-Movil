package com.example.demo.controller

import com.example.demo.model.request.PublicacionRequest
import com.example.demo.model.responses.PublicacionResponse
import com.example.demo.service.PublicacionService
import org.apache.coyote.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/publicacion")
class PublicacionController(
    private val publicacionService: PublicacionService
){

    @PostMapping
    fun crearPublicacion(@RequestBody publicacionRequest: PublicacionRequest, @RequestHeader accountId: UUID): ResponseEntity<PublicacionResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(publicacionService.crearPublicacion(accountId, publicacionRequest))
    }

    @GetMapping("/obtener/all")
    fun getPublicacionesByAccountId(@RequestHeader accountId: String): ResponseEntity<List<PublicacionResponse>> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.getPublicacionesByAccountId(accountId))
    }

    @GetMapping("/obtener/{publicacionId}")
    fun getPublicacionById(@PathVariable publicacionId: UUID): ResponseEntity<PublicacionResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.getPublicacionById(publicacionId))
    }

    @PutMapping("/add/like")
    fun getDarLike(@RequestParam publicacionId: UUID): ResponseEntity<PublicacionResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.darLike(publicacionId))
    }

    @PutMapping("/quit/like")
    fun quitLike(@RequestParam publicacionId: UUID): ResponseEntity<PublicacionResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.quitLike(publicacionId))
    }

    @PutMapping("/comment/post/{publicacionId}")
    fun comentarPublicacion(@PathVariable publicacionId: UUID, @RequestParam comentario: String): ResponseEntity<PublicacionResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.comentarPublicacion(publicacionId, comentario))
    }

    @PutMapping("/change/description/{publicacionId}")
    fun changeDescription(@PathVariable publicacionId: UUID, @RequestParam description: String): ResponseEntity<PublicacionResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(publicacionService.cambiarDescripcionDePublicacion(publicacionId, description))
    }
}