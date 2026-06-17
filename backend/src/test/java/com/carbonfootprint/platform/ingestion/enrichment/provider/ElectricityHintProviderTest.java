package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ElectricityHintProviderTest {

    private ElectricityHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ElectricityHintProvider();
    }

    @Test
    void order_is20() {
        assertThat(provider.getOrder()).isEqualTo(20);
    }

    // ── Brand detection ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"BESCOM", "Tata Power", "Adani Electricity", "TNEB", "KSEB"})
    void provide_detectsElectricityBrands(String merchant) {
        CarbonHints hints = provider.provide(contextWith(merchant, null, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.ELECTRICITY);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    // ── Unit signal ──────────────────────────────────────────────────────

    @Test
    void provide_unitKwh_infersElectricity() {
        CarbonHints hints = provider.provide(contextWith(null, null, "kwh", null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.ELECTRICITY);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_UNIT_SIGNAL);
    }

    @Test
    void provide_unitUnits_infersElectricity() {
        CarbonHints hints = provider.provide(contextWith(null, null, "units", null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.ELECTRICITY);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_UNIT_SIGNAL);
    }

    // ── Keyword signal ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"electricity bill", "power bill", "kwh", "meter reading"})
    void provide_detectsElectricityKeywords(String keyword) {
        CarbonHints hints = provider.provide(contextWith(null, keyword, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.ELECTRICITY);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_KEYWORD_MATCH);
    }

    // ── Category signal ──────────────────────────────────────────────────

    @Test
    void provide_categoryElectricity_infersElectricity() {
        CarbonHints hints = provider.provide(contextWith(null, null, null, ActivityCategory.ELECTRICITY));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.ELECTRICITY);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_CATEGORY_ONLY);
    }

    // ── Energy source ────────────────────────────────────────────────────

    @Test
    void provide_solarKeyword_setsEnergySourceToSolar() {
        // Need an electricity signal (brand/keyword/unit/category) PLUS solar keyword
        CarbonHints hints = provider.provide(contextWith("Tata Power", "solar power installation", null, null));

        assertThat(hints.getEnergySource()).isEqualTo(EnergySource.SOLAR);
    }

    @Test
    void provide_noSolarKeyword_setsEnergySourceToGrid() {
        CarbonHints hints = provider.provide(contextWith("Tata Power", null, null, null));

        assertThat(hints.getEnergySource()).isEqualTo(EnergySource.GRID);
    }

    // ── Electricity unit ─────────────────────────────────────────────────

    @Test
    void provide_alwaysSetsElectricityUnitToKwh() {
        CarbonHints hints = provider.provide(contextWith("BESCOM", null, null, null));

        assertThat(hints.getElectricityUnit()).isEqualTo("KWH");
    }

    // ── No signal ────────────────────────────────────────────────────────

    @Test
    void provide_noSignal_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", "random item", null, null));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Immutability ─────────────────────────────────────────────────────

    @Test
    void provide_doesNotModifyExtractionResult() {
        ExtractionResult original = ExtractionResult.builder()
                .merchant("BESCOM")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("BESCOM");
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private CarbonHintContext contextWith(String merchant, String description,
                                           String unit, ActivityCategory category) {
        return CarbonHintContext.from(ExtractionResult.builder()
                .merchant(merchant)
                .description(description)
                .unit(unit)
                .category(category)
                .build());
    }
}
