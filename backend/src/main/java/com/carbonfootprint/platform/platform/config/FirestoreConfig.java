package com.carbonfootprint.platform.platform.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * Configures the Google Cloud Firestore client bean.
 *
 * <h3>Authentication</h3>
 * Uses Application Default Credentials (ADC):
 * <ul>
 *   <li><strong>Cloud Run</strong>: the service account attached to the Cloud Run revision.</li>
 *   <li><strong>Local development</strong>: {@code gcloud auth application-default login}.</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * Project ID is read from {@code carbon.firestore.project-id} (env: {@code GCP_PROJECT_ID}).
 */
@Slf4j
@Configuration
@Profile("!stub")
public class FirestoreConfig {

    @Value("${carbon.firestore.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initialising Firestore client for project='{}'", projectId);
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        return options.getService();
    }
}
