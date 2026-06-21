package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.coach.model.ChatCard;
import com.carbonfootprint.platform.carbon.coach.model.ChatMessage;
import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;
import com.carbonfootprint.platform.integration.ai.groq.GroqClient;
import com.carbonfootprint.platform.integration.ai.groq.GroqMessage;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Profile("!stub")
public class CarbonChatService implements com.carbonfootprint.platform.carbon.port.in.CarbonChatUseCase {

    private static final Pattern SUGGESTED_QUESTIONS_PATTERN =
            Pattern.compile("```suggested_questions\\s*\\n(\\[.*?])\\s*\\n```", Pattern.DOTALL);

    private final GroqClient groqClient;
    private final PersonalizedContextBuilder contextBuilder;
    private final ConversationMemory conversationMemory;
    private final CarbonChatPromptBuilder promptBuilder;
    private final String coachModel;
    private final ObjectMapper objectMapper;

    public CarbonChatService(
            GroqClient groqClient,
            PersonalizedContextBuilder contextBuilder,
            ConversationMemory conversationMemory,
            CarbonChatPromptBuilder promptBuilder,
            @Value("${carbon.groq.coach-model:llama-3.3-70b-versatile}") String coachModel,
            ObjectMapper objectMapper
    ) {
        this.groqClient = groqClient;
        this.contextBuilder = contextBuilder;
        this.conversationMemory = conversationMemory;
        this.promptBuilder = promptBuilder;
        this.coachModel = coachModel;
        this.objectMapper = objectMapper;
        log.info("CarbonChatService initialised — coachModel={}", coachModel);
    }

    public ChatResponse chat(String userId, ChatRequest request) {
        List<ChatMessage> incomingMessages = request.getMessages();
        if (incomingMessages == null || incomingMessages.isEmpty()) {
            return ChatResponse.builder().reply("Please send a message.").build();
        }

        String currentQuestion = incomingMessages.get(incomingMessages.size() - 1).getContent();

        // Build personalized context from real analytics
        String userContext = contextBuilder.buildContext(userId);

        // Retrieve conversation history
        List<ConversationMemory.ChatMessage> history = conversationMemory.getHistory(userId);

        // Build Groq messages
        List<GroqMessage> groqMessages = promptBuilder.buildMessages(userContext, history, currentQuestion);

        try {
            log.info("CarbonChatService — sending chat request: model={} messageCount={}",
                    coachModel, groqMessages.size());
            String rawResponse = groqClient.generateMessages(coachModel, groqMessages);
            log.info("CarbonChatService — received chat response: responseSize={}",
                    rawResponse != null ? rawResponse.length() : 0);

            ChatResponse response = parseStructuredResponse(rawResponse);

            // Save conversation to memory
            conversationMemory.addMessage(userId, "user", currentQuestion);
            conversationMemory.addMessage(userId, "assistant", response.getReply());

            return response;
        } catch (IngestionException e) {
            log.warn("CarbonChatService — AI call failed (IngestionException): {}", e.getMessage());
            return ChatResponse.builder()
                    .reply("I'm having trouble connecting to my AI brain right now. Please try again in a moment.")
                    .build();
        } catch (Exception e) {
            log.error("CarbonChatService — unexpected error during chat: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("Something went wrong on my end. Please try again in a moment.")
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

            // Extract suggested questions before parsing JSON
            List<String> suggestedQuestions = extractSuggestedQuestions(cleaned);
            // Remove the suggested_questions block from cleaned content before JSON parse
            if (!suggestedQuestions.isEmpty()) {
                cleaned = cleaned.replaceAll("```suggested_questions\\s*\\n\\[.*?]\\s*\\n```", "").trim();
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
                    .suggestedQuestions(suggestedQuestions.isEmpty() ? null : suggestedQuestions)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse structured AI response, falling back to raw text: {}", e.getMessage());
            return ChatResponse.builder().reply(content).build();
        }
    }

    private List<String> extractSuggestedQuestions(String content) {
        try {
            Matcher matcher = SUGGESTED_QUESTIONS_PATTERN.matcher(content);
            if (matcher.find()) {
                String json = matcher.group(1);
                return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.debug("Failed to extract suggested questions: {}", e.getMessage());
        }
        return List.of();
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
}
