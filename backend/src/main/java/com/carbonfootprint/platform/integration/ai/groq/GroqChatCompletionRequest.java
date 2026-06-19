package com.carbonfootprint.platform.integration.ai.groq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroqChatCompletionRequest {

    private String model;
    private List<GroqMessage> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type;
    }

    public static GroqChatCompletionRequest of(String model, List<GroqMessage> messages,
                                                Double temperature, Integer maxTokens) {
        return GroqChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(new ResponseFormat("json_object"))
                .build();
    }
}
