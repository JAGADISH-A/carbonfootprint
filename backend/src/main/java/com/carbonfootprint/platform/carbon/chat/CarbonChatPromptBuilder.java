package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.integration.ai.groq.GroqMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the system prompt and message list for the AI Carbon Coach chat.
 *
 * <h3>Architecture</h3>
 * This is the dedicated prompt builder for the multi-turn chat endpoint,
 * replacing the hardcoded SYSTEM_PROMPT in {@code CarbonChatService}.
 * It is separate from {@code CarbonCoachPromptBuilder} which handles
 * one-shot coach summaries.
 *
 * <h3>Prompt Structure</h3>
 * <ol>
 *   <li>System prompt: role definition + behavioral rules + response format</li>
 *   <li>User context block: personalized data from PersonalizedContextBuilder</li>
 *   <li>Conversation history: multi-turn messages from ConversationMemory</li>
 *   <li>Current user question</li>
 * </ol>
 */
@Component
@Profile("!stub")
public class CarbonChatPromptBuilder {

    private static final String SYSTEM_PROMPT = """
        You are **Carbon Wise**, an AI Carbon Reduction Coach embedded in the CarbonWise app.

        ## Your Role
        You are a friendly, encouraging sustainability advisor. You analyze the user's real carbon
        emission data and provide personalized, actionable advice to help them reduce their
        carbon footprint.

        ## Core Rules
        - ONLY use data from the "USER CARBON DATA" block below. Never fabricate statistics.
        - If data is unavailable or incomplete, say "I don't have enough data on that yet"
          and suggest what the user could track.
        - Keep responses concise (2-4 paragraphs max) — this is a mobile chat.
        - Be encouraging, not preachy. Celebrate progress, gently address high emissions.
        - Use specific numbers from the data when relevant.

        ## Response Format
        After your main response, include 3 suggested follow-up questions the user might
        want to ask. Format them as a JSON array after your response text, wrapped in
        ```suggested_questions and ``` markers. Example:

        Here's my response about your emissions...

        ```suggested_questions
        ["How can I reduce my transport emissions?", "What's my highest emitting category?", "Give me a weekly challenge"]
        ```

        ## Personality
        - Warm and supportive, like a knowledgeable friend
        - Use occasional emojis (🌱, 🌍, 💡) but don't overdo it
        - Celebrate small wins — every kg of CO2 saved matters
        - Be honest about trade-offs (e.g., "Electric cars help, but the biggest impact
          is usually reducing car trips altogether")
        """;

    /**
     * Build the full Groq message list for a chat request.
     *
     * @param userContext      the personalized context block from PersonalizedContextBuilder
     * @param conversationHistory prior messages from ConversationMemory
     * @param currentQuestion  the user's current question
     * @return list of GroqMessage for the API call
     */
    public List<GroqMessage> buildMessages(
            String userContext,
            List<ConversationMemory.ChatMessage> conversationHistory,
            String currentQuestion) {

        List<GroqMessage> messages = new ArrayList<>();

        // System prompt with injected context
        String systemWithContext = SYSTEM_PROMPT + "\n\n" + userContext;
        messages.add(GroqMessage.system(systemWithContext));

        // Conversation history
        for (ConversationMemory.ChatMessage msg : conversationHistory) {
            messages.add(new GroqMessage(msg.role(), msg.content()));
        }

        // Current question
        messages.add(GroqMessage.user(currentQuestion));

        return messages;
    }
}
