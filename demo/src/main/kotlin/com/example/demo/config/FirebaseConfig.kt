package com.example.demo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
open class FirebaseConfig {

    @Value("\${firebase.project-id}")
    private lateinit var projectId: String

    @Value("\${firebase.storage-bucket}")
    private lateinit var storageBucket: String

    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isEmpty()) {
            val serviceAccount = ClassPathResource("firebase-credentials.json").inputStream

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(projectId)
                .setStorageBucket(storageBucket)
                .build()

            return FirebaseApp.initializeApp(options)
        }
        return FirebaseApp.getInstance()
    }

    @Bean
    fun firestore(): Firestore {
        return FirestoreClient.getFirestore(firebaseApp())
    }

    @Bean
    fun storage(): Storage {
        val credentials = GoogleCredentials.fromStream(
            ClassPathResource("firebase-credentials.json").inputStream
        )
        return StorageOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build()
            .service
    }
}