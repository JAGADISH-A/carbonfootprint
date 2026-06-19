package com.carbonfootprint.platform.integration.ai.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChatCompletionResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<GroqChoice> choices;
    private GroqUsage usage;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    public String getContentText() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        GroqChoice choice = choices.get(0);
        if (choice.getMessage() == null) {
            return null;
        }
        return choice.getMessage().getContent();
    }
}
