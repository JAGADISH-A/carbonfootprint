package com.carbonfootprint.platform.document.adapter.in.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptUploadResponse {

    private String filename;
    private String mimeType;
    private long fileSize;
    private Instant receivedAt;

}
