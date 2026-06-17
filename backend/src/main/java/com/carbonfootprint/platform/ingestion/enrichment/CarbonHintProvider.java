package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.ingestion.model.CarbonHints;

/**
 * Strategy interface for a single carbon-domain inference provider.
 *
 * <h3>Strategy Pattern</h3>
 * Each implementation encapsulates the inference logic for one carbon domain
 * (fuel, electricity, transport, flight, shopping, merchant industry).
 * Providers are discovered by Spring and injected as {@code List<CarbonHintProvider>}
 * into {@link CarbonHintEngine}, which orchestrates them.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Implementations receive a pre-built {@link CarbonHintContext} — they must
 *       NOT build their own search corpus or call {@code toLowerCase()} on raw fields.</li>
 *   <li>Implementations must NOT modify the original {@link com.carbonfootprint.platform.ingestion.model.ExtractionResult}.</li>
 *   <li>Implementations must NOT modify metadata directly.</li>
 *   <li>Return a <em>partial</em> {@link CarbonHints} — only populate fields this
 *       provider can infer. Leave all other fields {@code null}.</li>
 *   <li>Return {@link CarbonHints#empty()} (not {@code null}) when no hints can be inferred.</li>
 *   <li>Set {@link CarbonHints#getConfidence()} using the constants defined in this interface.</li>
 * </ul>
 *
 * <h3>Confidence constants</h3>
 * Use these constants to express the strength of an inference:
 * <ul>
 *   <li>{@link #CONFIDENCE_MERCHANT_MATCH} (0.9) — direct merchant/brand keyword match</li>
 *   <li>{@link #CONFIDENCE_UNIT_SIGNAL} (0.8)    — unit field explicitly indicates domain</li>
 *   <li>{@link #CONFIDENCE_KEYWORD_MATCH} (0.7)  — description / item keyword match</li>
 *   <li>{@link #CONFIDENCE_CATEGORY_ONLY} (0.5)  — category-only inference (weakest signal)</li>
 * </ul>
 *
 * <h3>Extensibility</h3>
 * To add a new domain, create a new {@code @Component} implementing this interface.
 * The engine picks it up automatically — no existing code needs to change.
 *
 * @see CarbonHintEngine
 * @see CarbonHintContext
 * @see com.carbonfootprint.platform.ingestion.model.CarbonHints
 */
public interface CarbonHintProvider {

    // ── Confidence constants ────────────────────────────────────────────────

    /** Confidence for a direct merchant or brand keyword match. */
    double CONFIDENCE_MERCHANT_MATCH = 0.9;

    /** Confidence for an explicit unit field signal (e.g. kWh on the result). */
    double CONFIDENCE_UNIT_SIGNAL = 0.8;

    /** Confidence for a description or item keyword match. */
    double CONFIDENCE_KEYWORD_MATCH = 0.7;

    /** Confidence for category-only inference (weakest signal). */
    double CONFIDENCE_CATEGORY_ONLY = 0.5;

    // ── Strategy method ─────────────────────────────────────────────────────

    /**
     * Infers carbon hints for a single domain from the pre-built context.
     *
     * @param context the pre-built context containing the normalized extraction result,
     *                pre-lowercased corpus, and lookup methods — must not be mutated
     * @return a partial {@link CarbonHints} with only this provider's fields set;
     *         never {@code null} — return {@link CarbonHints#empty()} when nothing can be inferred
     */
    CarbonHints provide(CarbonHintContext context);

    /**
     * Determines the execution order of this provider within the engine.
     *
     * <p>Lower values run first. When two providers set the same field, the one with the
     * lower order wins (first-non-null-wins merge strategy in
     * {@link CarbonHintMerger}).
     *
     * <p>Recommended ordering:
     * <ol>
     *   <li>10 — FuelHintProvider (high specificity)</li>
     *   <li>20 — ElectricityHintProvider</li>
     *   <li>30 — FlightHintProvider</li>
     *   <li>40 — TransportHintProvider</li>
     *   <li>50 — ShoppingHintProvider</li>
     *   <li>60 — MerchantIndustryHintProvider (lowest specificity)</li>
     * </ol>
     *
     * @return execution order (lower = higher priority)
     */
    int getOrder();
}
