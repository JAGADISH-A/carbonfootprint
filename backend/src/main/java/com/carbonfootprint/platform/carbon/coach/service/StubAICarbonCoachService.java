package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.port.in.AICarbonCoachUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Profile("stub")
public class StubAICarbonCoachService implements AICarbonCoachUseCase {

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId) {
        return Optional.of(buildMockResponse());
    }

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId, LocalDate from, LocalDate to) {
        return Optional.of(buildMockResponse());
    }

    private AICarbonCoachResponse buildMockResponse() {
        return AICarbonCoachResponse.builder()
                .summary("Your carbon footprint has decreased by 8% compared to last week, primarily driven by a reduction in transport emissions. Electricity emissions remain your largest source of impact.")
                .strengths(List.of(
                        "Consistently choosing public transport and metro over taxi services.",
                        "Maintaining low grocery and shopping-related spend emissions."
                ))
                .concerns(List.of(
                        "Electricity emissions (grid dependency) are 65% of your total footprint.",
                        "High usage of heating/air conditioning during peak hours."
                ))
                .recommendations(List.of(
                        "Consider switching off appliances at the wall to reduce standby power usage.",
                        "Opt for energy-efficient LED lighting throughout your home.",
                        "Try walking or cycling for short trips under 2 kilometres."
                ))
                .weeklyChallenge("Standby Zero: Turn off all non-essential electronics at the power outlet before going to bed every night this week.")
                .motivation("Every small change adds up. Keep tracking your daily activities and aiming for a lower carbon lifestyle!")
                .confidence(95)
                .aiGenerated(true)
                .build();
    }
}
