package com.juan.tfg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.config.path:chessaissistant-1ea8f-firebase-adminsdk-fbsvc-40f460c561.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = null;
            File file = new File(firebaseConfigPath);
            if (file.exists()) {
                serviceAccount = new FileInputStream(file);
            } else {
                Resource resource = new ClassPathResource(firebaseConfigPath);
                if (resource.exists()) {
                    serviceAccount = resource.getInputStream();
                }
            }

            if (serviceAccount == null) {
                logger.warn("Firebase configuration file not found at {} or in classpath. Firebase will not be initialized.", firebaseConfigPath);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully.");
            }
        } catch (Exception e) {
            logger.error("Error initializing Firebase", e);
        }
    }
}
