package com.carbonfootprint.platform.mobile.controller;

import com.carbonfootprint.platform.mobile.dto.CompanionReleaseResponse;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/companion")
@Tag(name = "Companion App", description = "Android Companion APK release information")
public class CompanionReleaseController {

    private final String version;
    private final String downloadUrl;
    private final long fileSizeBytes;
    private final LocalDate releaseDate;
    private final List<String> releaseNotes;

    public CompanionReleaseController(
            @Value("${carbon.companion.version:}") String version,
            @Value("${carbon.companion.download-url:}") String downloadUrl,
            @Value("${carbon.companion.file-size-bytes:0}") long fileSizeBytes,
            @Value("${carbon.companion.release-date:}") String releaseDate,
            @Value("${carbon.companion.release-notes:}") List<String> releaseNotes
    ) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.releaseDate = releaseDate != null && !releaseDate.isBlank()
                ? LocalDate.parse(releaseDate) : null;
        this.releaseNotes = releaseNotes;
        log.info("CompanionReleaseController initialised — version={} available={}",
                version, !version.isBlank());
    }

    @GetMapping("/release")
    @Operation(
            summary = "Get latest Companion APK release info",
            description = "Returns metadata for the latest Android Companion APK including version, download URL, size, and release notes. Public endpoint — no auth required."
    )
    public ResponseEntity<ApiResponse<CompanionReleaseResponse>> getRelease() {
        boolean available = !version.isBlank() && !downloadUrl.isBlank();

        CompanionReleaseResponse response = CompanionReleaseResponse.builder()
                .available(available)
                .version(version)
                .downloadUrl(downloadUrl)
                .fileSizeBytes(fileSizeBytes)
                .releaseDate(releaseDate)
                .releaseNotes(releaseNotes != null ? releaseNotes : List.of())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Companion release info fetched"));
    }
}
