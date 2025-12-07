package com.compuinside.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            var serviceAccount = new ClassPathResource("firebase-credentials.json").getInputStream();
            var credentials = GoogleCredentials.fromStream(serviceAccount);

            var options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId("proyecto-cparalela")
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseAuth fireBaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}
