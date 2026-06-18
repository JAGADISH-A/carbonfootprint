package com.carbonfootprint.platform.carbon.adapter.in.web;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarbonInsightController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarbonInsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarbonInsightUseCase carbonInsightUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/carbon/insights";

    // ── Success cases ─────────────────────────────────────────────────────

    @Test
    void getInsights_noParams_returnsInsights() throws Exception {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Your total footprint is 45.00 kg CO₂e across 5 activities.")
                .achievements(List.of("Emissions are below 100 kg threshold."))
                .warnings(List.of())
                .recommendations(List.of("Consider using public transport."))
                .insights(List.of("Highest category: FUEL at 30.00 kg CO₂e."))
                .build();

        when(carbonInsightUseCase.generateInsights("anonymous"))
                .thenReturn(Optional.of(insight));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", is("Your total footprint is 45.00 kg CO₂e across 5 activities.")))
                .andExpect(jsonPath("$.data.achievements", hasSize(1)))
                .andExpect(jsonPath("$.data.warnings", hasSize(0)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.data.insights", hasSize(1)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(carbonInsightUseCase).generateInsights("anonymous");
    }

    @Test
    void getInsights_withDateRange_delegatesWithDates() throws Exception {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Insights for Jan 2026.")
                .achievements(List.of())
                .warnings(List.of())
                .recommendations(List.of())
                .insights(List.of("10 activities in period."))
                .build();

        // from=2026-01-01, to=2026-01-31
        // Controller converts: fromInstant = 2026-01-01T00:00:00Z
        //                     toInstant = 2026-02-01T00:00:00Z - 1ns = 2026-01-31T23:59:59.999999999Z
        Instant expectedFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant expectedTo = Instant.parse("2026-02-01T00:00:00Z").minusNanos(1);

        when(carbonInsightUseCase.generateInsights("anonymous", expectedFrom, expectedTo))
                .thenReturn(Optional.of(insight));

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", is("Insights for Jan 2026.")));

        verify(carbonInsightUseCase).generateInsights("anonymous", expectedFrom, expectedTo);
    }

    @Test
    void getInsights_noData_returnsEmptyInsightWithMessage() throws Exception {
        when(carbonInsightUseCase.generateInsights("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("No carbon data found")))
                .andExpect(jsonPath("$.data.summary", is("No carbon data found")))
                .andExpect(jsonPath("$.data.achievements", hasSize(0)))
                .andExpect(jsonPath("$.data.warnings", hasSize(0)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(0)))
                .andExpect(jsonPath("$.data.insights", hasSize(0)));
    }

    @Test
    void getInsights_dateRangeNoData_returnsEmptyInsightWithMessage() throws Exception {
        Instant expectedFrom = Instant.parse("2026-03-01T00:00:00Z");
        Instant expectedTo = Instant.parse("2026-04-01T00:00:00Z").minusNanos(1);

        when(carbonInsightUseCase.generateInsights("anonymous", expectedFrom, expectedTo))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("No carbon data found")));

        verify(carbonInsightUseCase).generateInsights("anonymous", expectedFrom, expectedTo);
    }

    // ── Date parameter handling ───────────────────────────────────────────

    @Test
    void getInsights_onlyFromParam_noDateRangeCalled() throws Exception {
        // When only 'from' is provided (no 'to'), should call the no-dates overload
        when(carbonInsightUseCase.generateInsights("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("from", "2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(carbonInsightUseCase).generateInsights("anonymous");
        verify(carbonInsightUseCase, never()).generateInsights(any(), any(Instant.class), any(Instant.class));
    }

    @Test
    void getInsights_onlyToParam_noDateRangeCalled() throws Exception {
        // When only 'to' is provided (no 'from'), should call the no-dates overload
        when(carbonInsightUseCase.generateInsights("anonymous"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL)
                        .param("to", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(carbonInsightUseCase).generateInsights("anonymous");
        verify(carbonInsightUseCase, never()).generateInsights(any(), any(Instant.class), any(Instant.class));
    }

    // ── Full insight data ─────────────────────────────────────────────────

    @Test
    void getInsights_fullInsight所有情节Returned() throws Exception {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Complete insight summary.")
                .achievements(List.of("Achievement 1", "Achievement 2"))
                .warnings(List.of("Warning 1"))
                .recommendations(List.of("Rec 1", "Rec 2", "Rec 3"))
                .insights(List.of("Insight 1", "Insight 2"))
                .build();

        when(carbonInsightUseCase.generateInsights("anonymous"))
                .thenReturn(Optional.of(insight));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.achievements", hasSize(2)))
                .andExpect(jsonPath("$.data.warnings", hasSize(1)))
                .andExpect(jsonPath("$.data.recommendations", hasSize(3)))
                .andExpect(jsonPath("$.data.insights", hasSize(2)));
    }
}
