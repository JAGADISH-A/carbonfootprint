package com.carbonfootprint.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionReleaseResponse {

    private boolean available;
    private String version;
    private String downloadUrl;
    private long fileSizeBytes;
    private LocalDate releaseDate;
    private List<String> releaseNotes;
}
