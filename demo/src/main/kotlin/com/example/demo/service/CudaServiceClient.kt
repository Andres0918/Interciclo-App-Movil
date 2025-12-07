package com.example.demo.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CudaServiceClient(
    @Value("\${cuda.service.url}")
    private val cudaServiceUrl: String
) {
    private val webClient = WebClient.builder()
        .baseUrl(cudaServiceUrl)
        .build()

    fun getAvailableFilters(): Mono<Map<String, Any>> {
        return webClient.get()
            .uri("/filters")
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { it as Map<String, Any> }
    }

    fun processImage(imageBytes: ByteArray, filterName: String): Mono<ByteArray> {
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("image", imageBytes).filename("image.jpg")
        bodyBuilder.part("filter", filterName)

        return webClient.post()
            .uri("/process/gpu")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToFlux(org.springframework.core.io.buffer.DataBuffer::class.java)
            .let { DataBufferUtils.join(it) }
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
    }

    fun healthCheck(): Mono<Map<String, Any>> {
        return webClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { it as Map<String, Any> }
            .onErrorResume {
                Mono.just(mapOf("status" to "unhealthy", "error" to it.message)) as Mono<out Map<String, Any>?>
            }
    }
}