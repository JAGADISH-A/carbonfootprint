package com.carbonfootprint.platform.ingestion.model;

/**
 * Represents the processing stage of a document within the ingestion pipeline.
 */
public enum IngestionLifecycleStage {
    /**
     * The document has just been received and initial validation is performed.
     * Content extraction (like OCR) has not yet occurred.
     */
    RECEIVED,

    /**
     * Content extraction (like OCR) has completed.
     */
    OCR_COMPLETED,
    
    /**
     * The document has been parsed into an ExtractionResult.
     */
    PARSED,

    /**
     * The document has been normalised into a final Activity.
     */
    NORMALISED
}
