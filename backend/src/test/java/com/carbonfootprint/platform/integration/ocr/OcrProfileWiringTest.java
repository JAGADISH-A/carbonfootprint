package com.carbonfootprint.platform.integration.ocr;

import com.carbonfootprint.platform.integration.ocr.paddle.PaddleOcrProvider;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class OcrProfileWiringTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("stub")
    class StubProfileTest {

        @MockBean
        private Firestore firestore;

        @Autowired
        private ApplicationContext context;

        @Autowired
        private OcrProvider ocrProvider;

        @Test
        void testStubProfileWiring() {
            assertThat(ocrProvider).isInstanceOf(StubOcrProvider.class);
            assertThat(context.containsBean("paddleOcrProvider")).isFalse();
            assertThat(context.containsBean("stubOcrProvider")).isTrue();
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("default")
    @TestPropertySource(properties = {
            "carbon.ocr.paddle.base-url=http://localhost:8001",
            "carbon.firestore.project-id=test-project",
            "carbon.groq.api-key=test-key",
            "carbon.groq.model=openai/gpt-oss-20b",
            "GCP_PROJECT_ID=test-project"
    })
    class DefaultProfileTest {

        @MockBean
        private Firestore firestore;

        @Autowired
        private ApplicationContext context;

        @Autowired
        private OcrProvider ocrProvider;

        @Autowired
        private RestClient ocrRestClient;

        @Test
        void testDefaultProfileWiring() {
            assertThat(ocrProvider).isInstanceOf(PaddleOcrProvider.class);
            assertThat(context.containsBean("paddleOcrProvider")).isTrue();
            assertThat(context.containsBean("stubOcrProvider")).isFalse();
        }

        @Test
        void testOcrRestClientHttpVersion() throws Exception {
            Class<?> current = ocrRestClient.getClass();
            java.lang.reflect.Field factoryField = null;
            while (current != null) {
                try {
                    factoryField = current.getDeclaredField("clientRequestFactory");
                    break;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }

            if (factoryField == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Class: ").append(ocrRestClient.getClass().getName()).append("\n");
                current = ocrRestClient.getClass();
                while (current != null) {
                    sb.append("  Fields in ").append(current.getName()).append(":\n");
                    for (java.lang.reflect.Field f : current.getDeclaredFields()) {
                        sb.append("    - ").append(f.getName()).append(" (type: ").append(f.getType().getName()).append(")\n");
                    }
                    current = current.getSuperclass();
                }
                throw new IllegalStateException("Could not find field 'clientRequestFactory'. Dump:\n" + sb.toString());
            }

            factoryField.setAccessible(true);
            Object requestFactory = factoryField.get(ocrRestClient);

            // If it's a wrapper request factory, get the target factory
            if (requestFactory instanceof org.springframework.http.client.support.HttpRequestWrapper) {
                // Just in case it is wrapped
            }

            assertThat(requestFactory).isInstanceOf(JdkClientHttpRequestFactory.class);

            // Get the httpClient from JdkClientHttpRequestFactory
            java.lang.reflect.Field clientField = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
            clientField.setAccessible(true);
            java.net.http.HttpClient httpClient = (java.net.http.HttpClient) clientField.get(requestFactory);

            // Verify that HTTP/1.1 is configured to ensure FastAPI/Uvicorn compatibility
            assertThat(httpClient.version()).isEqualTo(java.net.http.HttpClient.Version.HTTP_1_1);
        }
    }
}
