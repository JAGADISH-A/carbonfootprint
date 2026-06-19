package com.carbonfootprint.platform.integration.ai.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChoice {

    private Integer index;
    private GroqMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
