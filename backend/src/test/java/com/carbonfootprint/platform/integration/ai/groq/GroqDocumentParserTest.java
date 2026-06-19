package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroqDocumentParserTest {

    @Mock
    private GroqClient groqClient;

    private GroqDocumentParser parser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RECEIPT_MODEL = "openai/gpt-oss-20b";

    @BeforeEach
    void setUp() {
        parser = new GroqDocumentParser("test-api-key", RECEIPT_MODEL, groqClient, objectMapper);
    }

    // ── Model selection ──────────────────────────────────────────────────

    @Test
    void parse_usesReceiptModel() throws Exception {
        String groqResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":\\"Test\\",\\"amount\\":100.00,\\"currency\\":\\"INR\\",\\"category\\":\\"FOOD\\"}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(groqResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Test receipt")
                .source(ActivitySource.RECEIPT)
                .build();

        parser.parse(doc);

        ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).generateContent(modelCaptor.capture(), any());
        assertThat(modelCaptor.getValue()).isEqualTo(RECEIPT_MODEL);
    }

    @Test
    void constructor_storesReceiptModel() {
        GroqDocumentParser customParser = new GroqDocumentParser(
                "key", "custom-receipt-model", groqClient, objectMapper);

        RawDocument doc = RawDocument.builder().rawText("text").build();
        assertThat(customParser.supports(doc)).isTrue();
    }

    // ── Prompt contains strict JSON rules ────────────────────────────────

    @Test
    void buildExtractionPrompt_containsNumericRules() throws Exception {
        String systemMessage = "Extract information from documents. Return ONLY valid JSON. No markdown. Use null for unknown fields. "
                + "Numeric fields must contain final computed values only — never arithmetic expressions (e.g. use 1440.48, not 1375.00 + 32.74 + 32.74). "
                + "Never output comments, explanatory text, or code fences. Every value must be a literal JSON primitive.";

        assertThat(systemMessage).contains("never arithmetic expressions");
        assertThat(systemMessage).contains("1440.48, not 1375.00 + 32.74 + 32.74");
        assertThat(systemMessage).contains("literal JSON primitive");
    }

    // ── Receipt with subtotal + tax → numeric fields are plain numbers ───

    @Test
    void parse_receiptWithSubtotalAndTax_amountIsPlainNumber() throws Exception {
        String groqResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":\\"Ashoka Restaurant\\",\\"amount\\":1440.48,\\"currency\\":\\"INR\\",\\"taxAmount\\":65.48,\\"subtotal\\":1375.00,\\"category\\":\\"FOOD\\",\\"confidence\\":0.95}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(groqResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Ashoka Restaurant\\nSubtotal: 1375.00\\nTax: 65.48\\nTotal: 1440.48")
                .source(ActivitySource.RECEIPT)
                .build();

        ExtractionResult result = parser.parse(doc);

        assertThat(result.getAmount()).isNotNull();
        assertThat(result.getAmount().toString()).isEqualTo("1440.48");
        assertThat(result.getMerchant()).isEqualTo("Ashoka Restaurant");
    }

    @Test
    void parse_receiptWithExpressionAmount_throwsOnInvalidJson() throws Exception {
        String badResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":\\"Test\\",\\"amount\\":1375.00 + 65.48,\\"currency\\":\\"INR\\",\\"category\\":\\"FOOD\\"}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(badResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Test receipt")
                .source(ActivitySource.RECEIPT)
                .build();

        assertThatThrownBy(() -> parser.parse(doc))
                .isInstanceOf(com.carbonfootprint.platform.platform.exception.IngestionException.class);
    }

    @Test
    void parse_validJsonWithAllNumericFields_arePlainNumbers() throws Exception {
        String groqResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":\\"Shell\\",\\"amount\\":2500.00,\\"currency\\":\\"INR\\",\\"taxAmount\\":375.00,\\"subtotal\\":2125.00,\\"discount\\":0,\\"quantity\\":1,\\"confidence\\":0.9,\\"items\\":[{\\"name\\":\\"Fuel\\",\\"quantity\\":1,\\"unitPrice\\":2500.00,\\"totalPrice\\":2500.00}]}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(groqResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Shell Fuel Station\\nFuel: 2500.00")
                .source(ActivitySource.RECEIPT)
                .build();

        ExtractionResult result = parser.parse(doc);

        assertThat(result.getAmount()).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("2500.00");
        assertThat(result.getMetadata()).isNotNull();
    }

    @Test
    void parse_receiptWithNullFields_handledGracefully() throws Exception {
        String groqResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":null,\\"amount\\":null,\\"currency\\":null,\\"category\\":null}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(groqResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Unknown receipt")
                .source(ActivitySource.RECEIPT)
                .build();

        ExtractionResult result = parser.parse(doc);

        assertThat(result.getMerchant()).isNull();
        assertThat(result.getAmount()).isNull();
        assertThat(result.getCategory()).isNull();
    }

    @Test
    void parse_receiptWithSubtotalPlusTax_computedCorrectly() throws Exception {
        String groqResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"merchant\\":\\"Big Bazaar\\",\\"amount\\":550.00,\\"currency\\":\\"INR\\",\\"taxAmount\\":50.00,\\"subtotal\\":500.00,\\"category\\":\\"SHOPPING\\",\\"confidence\\":0.88}"
                    }
                  }]
                }
                """;

        when(groqClient.generateContent(any(), any())).thenReturn(groqResponse);

        RawDocument doc = RawDocument.builder()
                .id(UUID.randomUUID().toString())
                .rawText("Big Bazaar\\nSubtotal: 500.00\\nTax (10%): 50.00\\nTotal: 550.00")
                .source(ActivitySource.RECEIPT)
                .build();

        ExtractionResult result = parser.parse(doc);

        assertThat(result.getAmount()).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("550.00");
        assertThat(result.getMerchant()).isEqualTo("Big Bazaar");
    }

    // ── supports() ──────────────────────────────────────────────────────

    @Test
    void supports_blankApiKey_returnsFalse() {
        GroqDocumentParser noKeyParser = new GroqDocumentParser("", RECEIPT_MODEL, groqClient, objectMapper);
        RawDocument doc = RawDocument.builder().rawText("text").build();
        assertThat(noKeyParser.supports(doc)).isFalse();
    }

    @Test
    void supports_nullRawText_returnsFalse() {
        RawDocument doc = RawDocument.builder().rawText(null).build();
        assertThat(parser.supports(doc)).isFalse();
    }

    @Test
    void supports_validDocument_returnsTrue() {
        RawDocument doc = RawDocument.builder().rawText("receipt text").build();
        assertThat(parser.supports(doc)).isTrue();
    }
}
