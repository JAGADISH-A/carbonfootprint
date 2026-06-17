package com.carbonfootprint.platform.ingestion.port.in;

import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.model.IngestionResult;

/**
 * Inbound port: the primary use-case interface for the ingestion pipeline.
 *
 * <p>Driving adapters (REST controllers, message consumers) must depend on this
 * interface — never on the service implementation directly. This enforces the
 * Dependency Inversion Principle and keeps the application core independent
 * of the delivery mechanism.
 *
 * <p>Implementations: {@link com.carbonfootprint.platform.ingestion.service.IngestionPipelineService}
 */
public interface IngestionUseCase {

    /**
     * Executes the full ingestion pipeline for the given request.
     *
     * <p>Pipeline steps:
     * <ol>
     *   <li>Locate the appropriate {@link com.carbonfootprint.platform.ingestion.port.out.IngestionSource}.</li>
     *   <li>Convert the request to a {@link com.carbonfootprint.platform.document.model.RawDocument}.</li>
     *   <li>Validate the {@code RawDocument}.</li>
     *   <li>Parse the {@code RawDocument} into an {@link com.carbonfootprint.platform.activity.model.Activity}.</li>
     *   <li>Validate and normalise the {@code Activity}.</li>
     *   <li>Persist both {@code RawDocument} and {@code Activity}.</li>
     * </ol>
     *
     * @param request the ingestion request carrying source, user, and raw data
     * @return the result containing the persisted {@code RawDocument} and {@code Activity}
     */
    IngestionResult ingest(IngestionRequest request);
}
