package com.carbonfootprint.platform.document.adapter.in.web;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.port.in.IngestionUseCase;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Endpoints for document ingestion")
public class ReceiptUploadController {

    @Value("${carbon.upload.supported-mime-types:image/jpeg,image/png,image/webp,application/pdf}")
    private List<String> supportedMimeTypes;

    private final IngestionUseCase ingestionUseCase;


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a receipt for carbon processing")
    public ResponseEntity<ApiResponse<ReceiptUploadResponse>> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryHint", required = false) String categoryHint) {

        if (file.isEmpty()) {
            log.warn("Rejected empty file upload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("File cannot be empty", "EMPTY_FILE"));
        }

        // TODO: Move MIME type validation into the core validation layer (RawDocumentValidator)
        String mimeType = file.getContentType();
        if (mimeType == null || !supportedMimeTypes.contains(mimeType.toLowerCase())) {
            log.warn("Rejected unsupported MIME type: {}", mimeType);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ApiResponse.error("Unsupported MIME type: " + mimeType + ". Supported types are: " + String.join(", ", supportedMimeTypes), "UNSUPPORTED_MEDIA_TYPE"));
        }

        ActivityCategory parsedCategoryHint = null;
        if (categoryHint != null && !categoryHint.isBlank()) {
            try {
                parsedCategoryHint = ActivityCategory.valueOf(categoryHint.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category hint provided: {}", categoryHint);
                // Continue processing; the parser will infer the category.
            }
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process uploaded file", "INTERNAL_SERVER_ERROR"));
        }

        // TODO: Extract userId from authenticated SecurityContext after Google Authentication is integrated.
        // Using a clearly temporary placeholder for now.
        String currentUserId = "anonymous";

        IngestionRequest request = IngestionRequest.builder()
                .userId(currentUserId)
                .source(ActivitySource.RECEIPT)
                .categoryHint(parsedCategoryHint)
                .fileBytes(fileBytes)
                .mimeType(mimeType)
                .originalFilename(file.getOriginalFilename())
                .build();

        // Let any exceptions bubble up to the GlobalExceptionHandler
        ingestionUseCase.ingest(request);

        ReceiptUploadResponse response = ReceiptUploadResponse.builder()
                .filename(file.getOriginalFilename())
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .receivedAt(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }
}
