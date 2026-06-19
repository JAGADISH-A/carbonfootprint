package com.carbonfootprint.platform.integration.ai.groq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroqMessage {

    private String role;
    private String content;

    public static GroqMessage system(String content) {
        return new GroqMessage("system", content);
    }

    public static GroqMessage user(String content) {
        return new GroqMessage("user", content);
    }

    public static GroqMessage assistant(String content) {
        return new GroqMessage("assistant", content);
    }
}
