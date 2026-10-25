package br.com.bnuuy.jwar.server.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bootstraps the Firebase Admin SDK. When the configured service-account path is empty or
 * unreadable, a stub FirebaseAuth bean is provided so dev/test profiles run without real credentials.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("Firebase service-account path is empty — using stub authentication. " +
                "Set app.firebase.service-account-path (env FB_SERVICE_ACCOUNT_PATH) for real verification.");
            return;
        }
        Path path = Path.of(serviceAccountPath);
        if (!Files.isReadable(path)) {
            log.error("Firebase service-account file '{}' is not readable. Falling back to stub auth.", serviceAccountPath);
            return;
        }
        try (FileInputStream credentialsStream = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialised from '{}'", serviceAccountPath);
            }
        } catch (IOException ex) {
            log.error("Failed to initialise Firebase Admin SDK from '{}'", serviceAccountPath, ex);
        }
    }

    @Bean
    public FirebaseAuthService firebaseAuthService() {
        boolean realFirebase = serviceAccountPath != null && !serviceAccountPath.isBlank()
            && !FirebaseApp.getApps().isEmpty();
        if (realFirebase) {
            return new RealFirebaseAuthService(FirebaseAuth.getInstance());
        }
        log.warn("Using stub Firebase auth service. Accepts tokens of the form 'dev:<uid>:<email>'.");
        return new StubFirebaseAuthService();
    }
}
