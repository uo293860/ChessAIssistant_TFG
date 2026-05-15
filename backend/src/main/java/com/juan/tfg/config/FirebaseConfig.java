package com.juan.tfg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.path:chessaissistant-1ea8f-firebase-adminsdk-fbsvc-40f460c561.json}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(loadCredentials())
                    .build();

            FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
            logger.info("Firebase app initialized successfully.");
            return firebaseApp;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to initialize Firebase Admin SDK. Set firebase.config.path, FIREBASE_CONFIG_PATH, or GOOGLE_APPLICATION_CREDENTIALS.",
                    e
            );
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (firebaseConfigPath == null || firebaseConfigPath.isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }

        Path serviceAccountPath = resolveServiceAccountPath();

        try (InputStream serviceAccount = Files.newInputStream(serviceAccountPath)) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    private Path resolveServiceAccountPath() {
        Path configuredPath = Path.of(firebaseConfigPath.trim());

        if (Files.exists(configuredPath)) {
            return configuredPath;
        }

        Path backendRelativePath = Path.of("backend").resolve(firebaseConfigPath);

        if (Files.exists(backendRelativePath)) {
            return backendRelativePath;
        }

        throw new IllegalStateException("Firebase config file not found. Checked filesystem paths: "
                + configuredPath.toAbsolutePath() + ", "
                + backendRelativePath.toAbsolutePath());
    }
}
