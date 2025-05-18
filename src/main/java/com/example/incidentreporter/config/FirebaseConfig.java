package com.example.incidentreporter.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credential.path}")
    private Resource firebaseCredentialResource;

    @PostConstruct
    public void initialize() {
        try {
            // Si FirebaseApp ya está inicializado, no hacemos nada
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(firebaseCredentialResource.getInputStream()))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar Firebase: " + e.getMessage(), e);
        }
    }
}