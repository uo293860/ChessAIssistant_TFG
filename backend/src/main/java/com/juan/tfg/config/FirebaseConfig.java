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

    /**
     * Creates or returns the singleton Firebase application used by the backend.
     *
     * @return the initialized Firebase application.
     */
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

    /**
     * Loads Firebase credentials from the configured service-account path.
     *
     * @return Google credentials for the Firebase Admin SDK.
     * @throws IOException if the configured credentials file cannot be read.
     */
    private GoogleCredentials loadCredentials() throws IOException {
        if (firebaseConfigPath == null || firebaseConfigPath.isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }

        Path serviceAccountPath = resolveServiceAccountPath();

        try (InputStream serviceAccount = Files.newInputStream(serviceAccountPath)) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    /**
     * Resolves the configured Firebase service-account file from the current directory or backend directory.
     *
     * @return the existing service-account file path.
     * @throws IllegalStateException if the configured file cannot be found.
     */
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
