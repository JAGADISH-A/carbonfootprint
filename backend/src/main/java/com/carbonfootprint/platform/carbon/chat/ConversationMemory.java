package com.carbonfootprint.platform.carbon.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory conversation history for multi-turn chat sessions.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Stores up to {@code MAX_HISTORY} messages per user (configurable)</li>
 *   <li>Thread-safe via ConcurrentHashMap</li>
 *   <li>No DB persistence — ephemeral session storage for demo purposes</li>
 *   <li>Old messages are trimmed from the front when limit is reached</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * All operations are atomic per-user. The list copy on get is safe for concurrent reads.
 */
@Slf4j
@Component
public class ConversationMemory {

    private static final int DEFAULT_MAX_HISTORY = 20;

    private final ConcurrentHashMap<String, List<ChatMessage>> histories = new ConcurrentHashMap<>();
    private final int maxHistory;

    public ConversationMemory() {
        this(DEFAULT_MAX_HISTORY);
    }

    public ConversationMemory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    /**
     * Append a message to the user's conversation history.
     */
    public void addMessage(String userId, String role, String content) {
        histories.computeIfAbsent(userId, k -> new ArrayList<>()).add(new ChatMessage(role, content));
        List<ChatMessage> history = histories.get(userId);
        if (history != null && history.size() > maxHistory) {
            synchronized (history) {
                while (history.size() > maxHistory) {
                    history.remove(0);
                }
            }
        }
    }

    /**
     * Get a copy of the user's conversation history.
     */
    public List<ChatMessage> getHistory(String userId) {
        List<ChatMessage> history = histories.get(userId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /**
     * Clear the user's conversation history.
     */
    public void clearHistory(String userId) {
        histories.remove(userId);
    }

    /**
     * Immutable message record for storage.
     */
    public record ChatMessage(String role, String content) {}
}
