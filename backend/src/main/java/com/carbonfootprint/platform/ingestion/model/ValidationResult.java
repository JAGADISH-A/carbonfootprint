package com.carbonfootprint.platform.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object representing the result of a validation step.
 *
 * <p>Carries pass/fail status and a list of human-readable violation messages.
 * Used by:
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator}</li>
 * </ul>
 *
 * <p>Results from multiple validators are combined via {@link #merge(ValidationResult)}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<String> violations = new ArrayList<>();

    // ── Convenience factory methods ──────────────────────────────────────────

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).build();
    }

    public static ValidationResult fail(String violation) {
        return ValidationResult.builder()
                .valid(false)
                .violations(List.of(violation))
                .build();
    }

    public static ValidationResult fail(List<String> violations) {
        return ValidationResult.builder()
                .valid(false)
                .violations(new ArrayList<>(violations))
                .build();
    }

    /**
     * Alias for {@link #fail(String)} — preferred in new code for readability.
     */
    public static ValidationResult failure(String violation) {
        return fail(violation);
    }

    /**
     * Merges another {@code ValidationResult} into this one.
     * Used to combine results from multiple validators in a chain.
     */
    public ValidationResult merge(ValidationResult other) {
        List<String> combined = new ArrayList<>(this.violations);
        combined.addAll(other.getViolations());
        return ValidationResult.builder()
                .valid(this.valid && other.isValid())
                .violations(combined)
                .build();
    }
}
