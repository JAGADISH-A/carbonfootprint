package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonHintContextTest {

    // ── Factory: from() ───────────────────────────────────────────────────

    @Test
    void from_buildsCorpusFromMerchantAndDescription() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell Petrol Pump")
                .description("40 litres diesel")
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCorpus()).contains("shell petrol pump");
        assertThat(ctx.getCorpus()).contains("40 litres diesel");
        assertThat(ctx.getMerchant()).isEqualTo("shell petrol pump");
        assertThat(ctx.getDescription()).isEqualTo("40 litres diesel");
    }

    @Test
    void from_includesMetadataStringsInCorpus() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("HP")
                .metadata(Map.of("fuelType", "petrol", "litres", 40))
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCorpus()).contains("hp");
        assertThat(ctx.getCorpus()).contains("petrol");
    }

    @Test
    void from_includesNestedListAndMapMetadata() {
        Map<String, Object> nested = Map.of("key", "nestedValue");
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Test")
                .metadata(Map.of("items", List.of("item1", nested)))
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCorpus()).contains("item1");
        assertThat(ctx.getCorpus()).contains("nestedvalue");
    }

    @Test
    void from_lowercasesAllFields() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("SHELL")
                .description("DIESEL")
                .unit("LITRE")
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCorpus()).isLowerCase();
        assertThat(ctx.getMerchant()).isLowerCase();
        assertThat(ctx.getDescription()).isLowerCase();
        assertThat(ctx.getUnit()).isLowerCase();
    }

    @Test
    void from_handlesNullMetadataGracefully() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(null)
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCorpus()).contains("shell");
    }

    @Test
    void from_handlesBlankMerchantAndDescription() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("  ")
                .description("")
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getMerchant()).isEmpty();
        assertThat(ctx.getDescription()).isEmpty();
    }

    @Test
    void from_preservesCategory() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Test")
                .category(ActivityCategory.FUEL)
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCategory()).isEqualTo(ActivityCategory.FUEL);
    }

    @Test
    void from_nullCategoryResultsInNull() {
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Test")
                .build();

        CarbonHintContext ctx = CarbonHintContext.from(result);

        assertThat(ctx.getCategory()).isNull();
    }

    // ── Lookup: contains() ────────────────────────────────────────────────

    @Test
    void contains_returnsTrueForMatchingKeyword() {
        CarbonHintContext ctx = buildContext("Shell Petrol Pump", null, null, null);

        assertThat(ctx.contains("shell")).isTrue();
        assertThat(ctx.contains("petrol")).isTrue();
    }

    @Test
    void contains_returnsFalseForNonMatchingKeyword() {
        CarbonHintContext ctx = buildContext("Shell", null, null, null);

        assertThat(ctx.contains("electricity")).isFalse();
    }

    // ── Lookup: merchantContains() ────────────────────────────────────────

    @Test
    void merchantContains_returnsTrueOnlyInMerchant() {
        CarbonHintContext ctx = buildContext("Shell Station", "diesel fuel", null, null);

        assertThat(ctx.merchantContains("shell")).isTrue();
        assertThat(ctx.merchantContains("diesel")).isFalse();
    }

    @Test
    void merchantContains_returnsFalseForEmptyMerchant() {
        CarbonHintContext ctx = buildContext(null, "description", null, null);

        assertThat(ctx.merchantContains("anything")).isFalse();
    }

    // ── Lookup: hasCategory() ────────────────────────────────────────────

    @Test
    void hasCategory_returnsTrueForMatchingCategory() {
        CarbonHintContext ctx = buildContext("Test", null, null, ActivityCategory.FUEL);

        assertThat(ctx.hasCategory(ActivityCategory.FUEL)).isTrue();
    }

    @Test
    void hasCategory_returnsFalseForNonMatchingCategory() {
        CarbonHintContext ctx = buildContext("Test", null, null, ActivityCategory.FUEL);

        assertThat(ctx.hasCategory(ActivityCategory.ELECTRICITY)).isFalse();
    }

    @Test
    void hasCategory_returnsFalseForNullCategory() {
        CarbonHintContext ctx = buildContext("Test", null, null, null);

        assertThat(ctx.hasCategory(ActivityCategory.FUEL)).isFalse();
    }

    // ── Lookup: hasCategoryOrNull() ──────────────────────────────────────

    @Test
    void hasCategoryOrNull_returnsTrueWhenCategoryIsNull() {
        CarbonHintContext ctx = buildContext("Test", null, null, null);

        assertThat(ctx.hasCategoryOrNull(ActivityCategory.FUEL)).isTrue();
    }

    @Test
    void hasCategoryOrNull_returnsTrueForMatchingCategory() {
        CarbonHintContext ctx = buildContext("Test", null, null, ActivityCategory.SHOPPING);

        assertThat(ctx.hasCategoryOrNull(ActivityCategory.SHOPPING)).isTrue();
    }

    @Test
    void hasCategoryOrNull_returnsFalseForNonMatchingCategory() {
        CarbonHintContext ctx = buildContext("Test", null, null, ActivityCategory.FUEL);

        assertThat(ctx.hasCategoryOrNull(ActivityCategory.SHOPPING)).isFalse();
    }

    // ── Lookup: anyMatch() ───────────────────────────────────────────────

    @Test
    void anyMatch_returnsTrueWhenAnyKeywordFound() {
        CarbonHintContext ctx = buildContext("Shell Petrol Pump", null, null, null);

        assertThat(ctx.anyMatch(List.of("shell", "electricity"))).isTrue();
    }

    @Test
    void anyMatch_returnsFalseWhenNoKeywordsFound() {
        CarbonHintContext ctx = buildContext("Unknown Merchant", null, null, null);

        assertThat(ctx.anyMatch(List.of("shell", "electricity"))).isFalse();
    }

    @Test
    void anyMatch_worksOnEmptyCollection() {
        CarbonHintContext ctx = buildContext("Shell", null, null, null);

        assertThat(ctx.anyMatch(List.of())).isFalse();
    }

    // ── Lookup: findFirstKeyword() ───────────────────────────────────────

    @Test
    void findFirstKeyword_returnsFirstMatch() {
        CarbonHintContext ctx = buildContext("Shell Petrol Pump", null, null, null);

        Optional<String> result = ctx.findFirstKeyword(List.of("petrol", "shell"));

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("petrol");
    }

    @Test
    void findFirstKeyword_returnsEmptyWhenNoneMatch() {
        CarbonHintContext ctx = buildContext("Unknown", null, null, null);

        assertThat(ctx.findFirstKeyword(List.of("shell", "petrol"))).isEmpty();
    }

    // ── Lookup: findFirstMatch() ─────────────────────────────────────────

    @Test
    void findFirstMatch_returnsFirstMatchingEntry() {
        CarbonHintContext ctx = buildContext("Shell Petrol Pump", null, null, null);

        Map<String, String> keywordMap = new LinkedHashMap<>();
        keywordMap.put("petrol", "PETROL");
        keywordMap.put("shell", "SHELL");

        Optional<Map.Entry<String, String>> result = ctx.findFirstMatch(keywordMap);

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo("PETROL");
    }

    @Test
    void findFirstMatch_returnsEmptyWhenNoneMatch() {
        CarbonHintContext ctx = buildContext("Unknown", null, null, null);

        Map<String, String> keywordMap = new LinkedHashMap<>();
        keywordMap.put("shell", "SHELL");

        assertThat(ctx.findFirstMatch(keywordMap)).isEmpty();
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private CarbonHintContext buildContext(String merchant, String description,
                                           String unit, ActivityCategory category) {
        ExtractionResult result = ExtractionResult.builder()
                .merchant(merchant)
                .description(description)
                .unit(unit)
                .category(category)
                .build();
        return CarbonHintContext.from(result);
    }
}
