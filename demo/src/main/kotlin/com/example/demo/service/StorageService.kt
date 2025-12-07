package com.example.demo.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class StorageService(
    private val storage: Storage,
    @Value("\${firebase.storage-bucket}")
    private val bucketName: String
) {

    fun uploadImage(imageBytes: ByteArray, folder: String): Mono<String> {
        return Mono.fromCallable {
            val fileName = "${UUID.randomUUID()}.jpg"
            val blobId = BlobId.of(bucketName, "$folder/$fileName")

            val blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("image/jpeg")
                .build()

            storage.create(blobInfo, imageBytes)

            // Hacer público
            val blob = storage.get(blobId)
            blob.createAcl(
                com.google.cloud.storage.Acl.of(
                    com.google.cloud.storage.Acl.User.ofAllUsers(),
                    com.google.cloud.storage.Acl.Role.READER
                )
            )

            "https://storage.googleapis.com/$bucketName/$folder/$fileName"
        }
    }

    fun deleteImage(imageUrl: String): Boolean {
        return try {
            // Extraer el path de la URL
            val path = imageUrl.substringAfter("$bucketName/")
            val blobId = BlobId.of(bucketName, path)
            storage.delete(blobId)
        } catch (e: Exception) {
            false
        }
    }
}