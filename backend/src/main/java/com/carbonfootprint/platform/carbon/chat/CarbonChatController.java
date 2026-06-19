package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import com.carbonfootprint.platform.shared.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiConstants.CARBON_PATH)
@Profile("!stub")
@RequiredArgsConstructor
@Tag(name = "Carbon Chat", description = "Interactive AI chat about carbon emissions")
public class CarbonChatController {

    private final CarbonChatService carbonChatService;

    @PostMapping("/chat")
    @Operation(summary = "Send a chat message and receive an AI response")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request) {

        String userId = getCurrentUserId();
        log.debug("Chat request: userId={} messageCount={}", userId,
                request.getMessages() != null ? request.getMessages().size() : 0);

        ChatResponse response = carbonChatService.chat(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getCurrentUserId() {
        return "anonymous";
    }
}
