package com.oviro.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises Firebase Admin SDK.
 * Activated only when oviro.fcm.enabled=true and a valid service-account
 * file is present at the configured path.
 *
 * Place the Google service-account JSON in:
 *   src/main/resources/firebase/service-account.json
 * or point oviro.fcm.service-account-path to any classpath/file: resource.
 */
@Configuration
@ConditionalOnProperty(name = "oviro.fcm.enabled", havingValue = "true")
@Slf4j
public class FCMConfig {

    @Value("${oviro.fcm.service-account-path}")
    private Resource serviceAccountResource;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp déjà initialisé – réutilisation de l'instance existante");
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = serviceAccountResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialisé avec succès");
            return app;
        } catch (IOException e) {
            log.error("Impossible de charger le service-account Firebase: {}", e.getMessage());
            throw e;
        }
    }
}
