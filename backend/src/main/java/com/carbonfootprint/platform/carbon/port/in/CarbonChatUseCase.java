package com.carbonfootprint.platform.carbon.port.in;

import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;

public interface CarbonChatUseCase {
    ChatResponse chat(String userId, ChatRequest request);
}
