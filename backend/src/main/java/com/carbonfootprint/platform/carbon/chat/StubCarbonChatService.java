package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.coach.model.ChatCard;
import com.carbonfootprint.platform.carbon.coach.model.ChatRequest;
import com.carbonfootprint.platform.carbon.coach.model.ChatResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonChatUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("stub")
public class StubCarbonChatService implements CarbonChatUseCase {

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "How can I reduce my transport emissions?",
            "What's my highest emitting category?",
            "Give me a weekly challenge"
    );

    @Override
    public ChatResponse chat(String userId, ChatRequest request) {
        String userQuery = "";
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            userQuery = request.getMessages().get(request.getMessages().size() - 1).getContent().toLowerCase();
        }

        List<ChatCard> cards = new ArrayList<>();
        List<String> suggestions;
        String reply;

        if (userQuery.contains("reduce") || userQuery.contains("help") || userQuery.contains("coach")) {
            reply = "To reduce your footprint, you should focus on **Electricity** and **Transport**. These are currently your highest contributors. Here are a few opportunities:";
            cards.add(ChatCard.builder()
                    .title("Top Opportunity")
                    .value("Standby Power")
                    .description("Turn off standby electronics to save up to 10% on power.")
                    .icon("opportunity")
                    .color("emerald")
                    .build());
            cards.add(ChatCard.builder()
                    .title("Action Plan")
                    .value("Walk < 2km")
                    .description("Substitute short taxi trips with walking or cycling.")
                    .icon("action")
                    .color("blue")
                    .build());
            suggestions = List.of(
                    "What else can I do at home?",
                    "How does my transport compare to last week?",
                    "Show me my monthly trend"
            );
        } else if (userQuery.contains("why") || userQuery.contains("high") || userQuery.contains("footprint") || userQuery.contains("emissions")) {
            reply = "Your carbon footprint is primarily driven by grid electricity usage. Your transport emissions have actually decreased by 8% this week.";
            cards.add(ChatCard.builder()
                    .title("Total Carbon")
                    .value("142.5 kg CO2e")
                    .description("Cumulative carbon footprint over the last 30 days.")
                    .icon("carbon")
                    .color("amber")
                    .build());
            cards.add(ChatCard.builder()
                    .title("Top Category")
                    .value("Electricity (65%)")
                    .description("Your home grid energy represents your largest impact area.")
                    .icon("category")
                    .color("red")
                    .build());
            suggestions = List.of(
                    "How can I lower my electricity usage?",
                    "What's my average daily emission?",
                    "Compare this month to last month"
            );
        } else {
            reply = "Hello! I am your Carbon Coach. I can help you analyze your carbon footprint, explain emissions categories, and suggest actionable ways to reduce your impact. Try asking me: *'How can I reduce my emissions?'*";
            suggestions = DEFAULT_SUGGESTIONS;
        }

        return ChatResponse.builder()
                .reply(reply)
                .cards(cards.isEmpty() ? null : cards)
                .suggestedQuestions(suggestions)
                .build();
    }
}
