package com.carbonfootprint.platform.carbon.adapter.in.web;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.port.in.AICarbonCoachUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AICarbonCoachController.class)
@AutoConfigureMockMvc(addFilters = false)
class AICarbonCoachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AICarbonCoachUseCase aiCarbonCoachUseCase;

    @MockBean
    private com.carbonfootprint.platform.mobile.service.DeviceTokenService deviceTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/carbon/coach";

    // ── Success cases ─────────────────────────────────────────────────────

    @Test
    void getCoach_noParams_returnsCoachResponse() throws Exception {
        AICarbonCoachResponse coach = AICarbonCoachResponse.builder()
                .summary("Your footprint is 45.0 kg CO₂e. You're doing well overall.")
                .strengths(List.of("Low flight emissions this month."))
                .concerns(List.of("Fuel usage is above average."))
                .recommendations(List.of("Try carpooling to reduce fuel emissions."))
                .weeklyChallenge("Walk or cycle for one trip this week.")
                .motivation("Every small change adds up!")
                .aiGenerated(true)
                .build();

        when(aiCarbonCoachUseCase.generateCoach("anonymous"))
                .thenReturn(Optional.of(coach));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", is("Your footprint is 45.0 kg CO₂e. You're doing well overall.")))
                .andExpect(jsonPath("$.data.strengths", hasSize(1)))
                .andExpect(jsonPath("$.data.concerns", hasSize(1)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.data.weeklyChallenge", is("Walk or cycle for one trip this week.")))
                .andExpect(jsonPath("$.data.motivation", is("Every small change adds up!")))
                .andExpect(jsonPath("$.data.aiGenerated", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(aiCarbonCoachUseCase).generateCoach("anonymous");
    }

    @Test
    void getCoach_withDateRange_delegatesWithDates() throws Exception {
        AICarbonCoachResponse coach = AICarbonCoachResponse.builder()
                .summary("Coaching for March 2026.")
                .strengths(List.of())
                .concerns(List.of())
                .recommendations(List.of("Reduce electricity use."))
                .weeklyChallenge("Unplug unused devices.")
                .motivation("You're on the right track!")
                .aiGenerated(false)
                .build();

        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(aiCarbonCoachUseCase.generateCoach("anonymous", from, to))
                .thenReturn(Optional.of(coach));

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", is("Coaching for March 2026.")))
                .andExpect(jsonPath("$.data.aiGenerated", is(false)));

        verify(aiCarbonCoachUseCase).generateCoach("anonymous", from, to);
    }

    // ── No data cases ─────────────────────────────────────────────────────

    @Test
    void getCoach_noData_returnsEmptyCoachWithMessage() throws Exception {
        when(aiCarbonCoachUseCase.generateCoach("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("No carbon data found")))
                .andExpect(jsonPath("$.data.summary", is("No carbon data found for coaching.")))
                .andExpect(jsonPath("$.data.strengths", hasSize(0)))
                .andExpect(jsonPath("$.data.concerns", hasSize(0)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(0)))
                .andExpect(jsonPath("$.data.weeklyChallenge", is("No challenge available.")))
                .andExpect(jsonPath("$.data.motivation", is("Start tracking your activities to receive coaching.")))
                .andExpect(jsonPath("$.data.aiGenerated", is(false)));
    }

    @Test
    void getCoach_dateRangeNoData_returnsEmptyCoachWithMessage() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(aiCarbonCoachUseCase.generateCoach("anonymous", from, to))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("No carbon data found")));

        verify(aiCarbonCoachUseCase).generateCoach("anonymous", from, to);
    }

    // ── Date parameter handling ───────────────────────────────────────────

    @Test
    void getCoach_onlyFromParam_noDateRangeCalled() throws Exception {
        when(aiCarbonCoachUseCase.generateCoach("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(aiCarbonCoachUseCase).generateCoach("anonymous");
        verify(aiCarbonCoachUseCase, never()).generateCoach(any(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getCoach_onlyToParam_noDateRangeCalled() throws Exception {
        when(aiCarbonCoachUseCase.generateCoach("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("to", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(aiCarbonCoachUseCase).generateCoach("anonymous");
        verify(aiCarbonCoachUseCase, never()).generateCoach(any(), any(LocalDate.class), any(LocalDate.class));
    }

    // ── Full response data ────────────────────────────────────────────────

    @Test
    void getCoach_fullResponse_allFieldsReturned() throws Exception {
        AICarbonCoachResponse coach = AICarbonCoachResponse.builder()
                .summary("Complete coaching summary.")
                .strengths(List.of("Strength 1", "Strength 2"))
                .concerns(List.of("Concern 1"))
                .recommendations(List.of("Rec 1", "Rec 2", "Rec 3"))
                .weeklyChallenge("Challenge: bike to work twice this week.")
                .motivation("Keep going, you're making a difference!")
                .aiGenerated(true)
                .build();

        when(aiCarbonCoachUseCase.generateCoach("anonymous"))
                .thenReturn(Optional.of(coach));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.strengths", hasSize(2)))
                .andExpect(jsonPath("$.data.concerns", hasSize(1)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(3)))
                .andExpect(jsonPath("$.data.weeklyChallenge", is("Challenge: bike to work twice this week.")))
                .andExpect(jsonPath("$.data.motivation", is("Keep going, you're making a difference!")))
                .andExpect(jsonPath("$.data.aiGenerated", is(true)));
    }
}
