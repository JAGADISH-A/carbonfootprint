package com.carbonfootprint.platform;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the Spring application context loads successfully.
 *
 * <p>This test is intentionally minimal — it only checks that all beans
 * are wired correctly and there are no configuration errors.
 *
 * <p>External dependencies (Firestore, Groq AI, PaddleOCR) are mocked
 * via test properties to prevent network calls during CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "carbon.firestore.project-id=test-project",
        "carbon.groq.api-key=test-key",
        "carbon.groq.model=openai/gpt-oss-20b",
        "carbon.ocr.paddle.base-url=http://localhost:8001",
        "GCP_PROJECT_ID=test-project"
})
class CarbonPlatformApplicationTest {

    @MockBean
    private Firestore firestore;

    @Test
    void contextLoads() {
        // If the context loads without throwing, this test passes
    }
}
