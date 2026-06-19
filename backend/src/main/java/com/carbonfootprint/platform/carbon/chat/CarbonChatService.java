package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.coach.model.ChatMessage;
import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.integration.ai.groq.GroqClient;
import com.carbonfootprint.platform.integration.ai.groq.GroqMessage;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Profile("!stub")
public class CarbonChatService {

    private static final String SYSTEM_PROMPT = """
            You are EcoBuddy, a friendly and knowledgeable sustainability coach.
            You help users understand their carbon footprint based on their receipt data.

            Your personality:
            - Warm, encouraging, and conversational
            - Specific and data-driven — always reference the user's actual numbers
            - Practical — suggest realistic, achievable changes
            - Never preachy or judgmental

            Rules:
            - Reference the user's specific data (total kg, categories, merchants) whenever relevant
            - Keep responses concise (2-4 paragraphs max) unless the user asks for detail
            - Use markdown formatting: **bold** for emphasis, bullet lists for suggestions
            - If asked about something outside your expertise, say so honestly
            - Never invent numbers or data — only use what's provided in the context
            - If no carbon data is available, encourage the user to upload receipts first
            """;

    private final GroqClient groqClient;
    private final CarbonInsightUseCase carbonInsightUseCase;
    private final String coachModel;

    public CarbonChatService(
            GroqClient groqClient,
            CarbonInsightUseCase carbonInsightUseCase,
            @Value("${carbon.groq.coach-model:llama-3.3-70b-versatile}") String coachModel
    ) {
        this.groqClient = groqClient;
        this.carbonInsightUseCase = carbonInsightUseCase;
        this.coachModel = coachModel;
        log.info("CarbonChatService initialised — coachModel={}", coachModel);
    }

    public ChatResponse chat(String userId, ChatRequest request) {
        List<ChatMessage> incomingMessages = request.getMessages();
        if (incomingMessages == null || incomingMessages.isEmpty()) {
            return ChatResponse.builder().reply("Please send a message.").build();
        }

        String contextBlock = buildAnalyticsContext(userId);

        List<GroqMessage> groqMessages = new ArrayList<>();
        groqMessages.add(GroqMessage.system(SYSTEM_PROMPT + "\n\n" + contextBlock));

        for (ChatMessage msg : incomingMessages) {
            String role = msg.getRole() != null ? msg.getRole().toLowerCase() : "user";
            if ("system".equals(role)) continue;
            if ("assistant".equals(role)) {
                groqMessages.add(GroqMessage.assistant(msg.getContent()));
            } else {
                groqMessages.add(GroqMessage.user(msg.getContent()));
            }
        }

        try {
            String rawResponse = groqClient.generateMessages(coachModel, groqMessages);
            String reply = extractContent(rawResponse);
            return ChatResponse.builder().reply(reply).build();
        } catch (IngestionException e) {
            log.warn("CarbonChatService — AI call failed: {}", e.getMessage());
            return ChatResponse.builder()
                    .reply("I'm having trouble connecting to my AI brain right now. Please try again in a moment.")
                    .build();
        }
    }

    private String buildAnalyticsContext(String userId) {
        Optional<com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse> coachOpt =
                carbonInsightUseCase.generateInsights(userId)
                        .map(insight -> {
                            CarbonAnalyticsResponse a = insight.getAnalytics();
                            StringBuilder sb = new StringBuilder();
                            sb.append("\n--- USER CARBON DATA ---\n");
                            sb.append("Total emissions: ").append(fmt(a.getTotalCarbonKg())).append(" kg CO₂e\n");
                            sb.append("Activity count: ").append(a.getActivityCount()).append("\n");
                            sb.append("Average daily: ").append(fmt(a.getAverageDailyKg())).append(" kg\n");

                            if (a.getCategoryTotals() != null && !a.getCategoryTotals().isEmpty()) {
                                sb.append("Categories:\n");
                                for (CategoryEmissionSummary cat : a.getCategoryTotals()) {
                                    sb.append("  - ").append(cat.getCategory())
                                            .append(": ").append(fmt(cat.getCarbonKg())).append(" kg (")
                                            .append(fmtPct(cat.getPercentageOfTotal())).append("%)\n");
                                }
                            }

                            if (a.getTopActivities() != null && !a.getTopActivities().isEmpty()) {
                                sb.append("Top activities:\n");
                                for (TopEmissionActivity act : a.getTopActivities().stream().limit(5).toList()) {
                                    sb.append("  - ").append(act.getMerchant() != null ? act.getMerchant() : act.getCategory())
                                            .append(": ").append(fmt(act.getCarbonKg())).append(" kg\n");
                                }
                            }

                            sb.append("--- END DATA ---\n");
                            return sb.toString();
                        });

        return coachOpt.orElse("No carbon data available yet. The user hasn't uploaded any receipts.");
    }

    private String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "I couldn't generate a response. Please try again.";
        }
        try {
            int choicesIdx = rawResponse.indexOf("\"choices\"");
            if (choicesIdx == -1) return rawResponse;

            int messageIdx = rawResponse.indexOf("\"content\"", choicesIdx);
            if (messageIdx == -1) return rawResponse;

            int colonIdx = rawResponse.indexOf(':', messageIdx);
            int quoteStart = rawResponse.indexOf('"', colonIdx + 1);
            if (quoteStart == -1) return rawResponse;

            int quoteEnd = findClosingQuote(rawResponse, quoteStart + 1);
            if (quoteEnd == -1) return rawResponse;

            String content = rawResponse.substring(quoteStart + 1, quoteEnd);
            return content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception e) {
            log.warn("Failed to parse Groq response, returning raw: {}", e.getMessage());
            return rawResponse.length() > 2000 ? rawResponse.substring(0, 2000) + "..." : rawResponse;
        }
    }

    private int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    private String fmt(BigDecimal val) {
        return val == null ? "0" : val.setScale(1, RoundingMode.HALF_UP).toString();
    }

    private String fmtPct(BigDecimal val) {
        return val == null ? "0" : val.setScale(0, RoundingMode.HALF_UP).toString();
    }
}
