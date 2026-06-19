package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.coach.model.ChatCard;
import com.carbonfootprint.platform.carbon.coach.model.ChatMessage;
import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.integration.ai.groq.GroqClient;
import com.carbonfootprint.platform.integration.ai.groq.GroqMessage;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            RESPONSE FORMAT:
            You MUST respond with a JSON object. Do NOT output any text outside the JSON.
            The JSON has two fields:
            - "reply": A short conversational message (1-3 sentences max). Use markdown for emphasis.
            - "cards": An array of analysis cards. Include cards ONLY when the user's question
              calls for data analysis. For general chat (greetings, simple questions), use an empty array.

            Each card has:
            - "title": Short label (e.g. "Total Emissions", "Top Category", "Confidence")
            - "value": The main number or short text (e.g. "45.2 kg CO₂e")
            - "description": One sentence explaining the card
            - "icon": One of: "carbon", "category", "opportunity", "trend", "confidence", "action"
            - "color": One of: "emerald", "amber", "blue", "red", "purple"

            Available card icons:
            - "carbon": Total emissions summary
            - "category": Highest emission category
            - "opportunity": Best improvement area
            - "trend": Trend or comparison
            - "confidence": Data confidence level
            - "action": Action plan item

            CARD GUIDELINES:
            - For "why is my footprint high" → include carbon + category + opportunity cards
            - For "which purchase" → include category card + opportunity card
            - For "reduce emissions" → include opportunity + action cards
            - For "action plan" → include action card
            - For "compare" → include trend card
            - For greetings / simple questions → empty cards array

            Rules:
            - Reference the user's specific data (total kg, categories, merchants)
            - Never invent numbers — use ONLY the provided context data
            - Keep the reply short and warm
            - Output ONLY valid JSON, no markdown fences, no extra text
            """;

    private final GroqClient groqClient;
    private final CarbonInsightUseCase carbonInsightUseCase;
    private final String coachModel;
    private final ObjectMapper objectMapper;

    public CarbonChatService(
            GroqClient groqClient,
            CarbonInsightUseCase carbonInsightUseCase,
            @Value("${carbon.groq.coach-model:llama-3.3-70b-versatile}") String coachModel,
            ObjectMapper objectMapper
    ) {
        this.groqClient = groqClient;
        this.carbonInsightUseCase = carbonInsightUseCase;
        this.coachModel = coachModel;
        this.objectMapper = objectMapper;
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
            return parseStructuredResponse(rawResponse);
        } catch (IngestionException e) {
            log.warn("CarbonChatService — AI call failed: {}", e.getMessage());
            return ChatResponse.builder()
                    .reply("I'm having trouble connecting to my AI brain right now. Please try again in a moment.")
                    .build();
        }
    }

    private ChatResponse parseStructuredResponse(String rawResponse) {
        String content = extractContent(rawResponse);
        if (content == null || content.isBlank()) {
            return ChatResponse.builder().reply("I couldn't generate a response. Please try again.").build();
        }

        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "");
            }

            JsonNode root = objectMapper.readTree(cleaned);

            String reply = root.has("reply") ? root.get("reply").asText("") : "";
            List<ChatCard> cards = new ArrayList<>();

            if (root.has("cards") && root.get("cards").isArray()) {
                for (JsonNode cardNode : root.get("cards")) {
                    cards.add(ChatCard.builder()
                            .title(getText(cardNode, "title"))
                            .value(getText(cardNode, "value"))
                            .description(getText(cardNode, "description"))
                            .icon(getText(cardNode, "icon"))
                            .color(getText(cardNode, "color"))
                            .build());
                }
            }

            if (reply.isBlank()) {
                reply = "Here's what I found based on your data.";
            }

            return ChatResponse.builder()
                    .reply(reply)
                    .cards(cards.isEmpty() ? null : cards)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse structured AI response, falling back to raw text: {}", e.getMessage());
            return ChatResponse.builder().reply(content).build();
        }
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText("") : null;
    }

    private String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
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
            log.warn("Failed to extract content from Groq response: {}", e.getMessage());
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

    private String buildAnalyticsContext(String userId) {
        return carbonInsightUseCase.generateInsights(userId)
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
                })
                .orElse("No carbon data available yet. The user hasn't uploaded any receipts.");
    }

    private String fmt(BigDecimal val) {
        return val == null ? "0" : val.setScale(1, RoundingMode.HALF_UP).toString();
    }

    private String fmtPct(BigDecimal val) {
        return val == null ? "0" : val.setScale(0, RoundingMode.HALF_UP).toString();
    }
}
