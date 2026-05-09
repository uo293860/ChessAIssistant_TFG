package com.juan.tfg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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

        try (InputStream serviceAccount = openServiceAccount()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
            logger.info("Firebase app initialized successfully.");
            return firebaseApp;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize Firebase Admin SDK.", e);
        }
    }

    private InputStream openServiceAccount() throws IOException {
        Path configuredPath = Path.of(firebaseConfigPath);

        if (Files.exists(configuredPath)) {
            return Files.newInputStream(configuredPath);
        }

        Path backendRelativePath = Path.of("backend").resolve(firebaseConfigPath);

        if (Files.exists(backendRelativePath)) {
            return Files.newInputStream(backendRelativePath);
        }

        Resource classpathResource = new ClassPathResource(firebaseConfigPath);

        if (classpathResource.exists()) {
            return classpathResource.getInputStream();
        }

        throw new IllegalStateException("Firebase config file not found. Checked: "
                + configuredPath.toAbsolutePath() + ", "
                + backendRelativePath.toAbsolutePath() + ", and classpath:"
                + firebaseConfigPath);
    }
}
