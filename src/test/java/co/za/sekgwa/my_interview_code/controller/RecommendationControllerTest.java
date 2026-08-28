package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.RecommendationItem;
import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    private static final String REQUEST_BODY = """
            {
              "customerReference": "CUST-10291",
              "usageProfile": {
                "averageMonthlyDataMb": 3200,
                "averageMonthlyVoiceMinutes": 40,
                "preferredValidityDays": 30,
                "maximumBudget": 160
              }
            }
            """;

    @Test
    void shouldReturn200WithRecommendationsForValidRequest() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                "REC54213.778",
                List.of(new RecommendationItem("PROD-004", 0.78, "AI")),
                "AI",
                "bundle-recommendations-v1");
        when(recommendationService.recommendProducts(any(RecommendationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bundles/bundle-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationId").value("REC54213.778"))
                .andExpect(jsonPath("$.recommendations[0].productCode").value("PROD-004"))
                .andExpect(jsonPath("$.recommendations[0].confidence").value(0.78))
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.promptVersion").value("bundle-recommendations-v1"));
    }

    @Test
    void shouldReturnUpToThreeRecommendationsAsProvidedByService() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                "REC12345.6",
                List.of(
                        new RecommendationItem("P1", 0.9, "AI"),
                        new RecommendationItem("P2", 0.7, "AI"),
                        new RecommendationItem("P3", 0.5, "AI")
                ),
                "AI",
                "bundle-recommendations-v1");
        when(recommendationService.recommendProducts(any(RecommendationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bundles/bundle-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3));
    }

    @Test
    void shouldPassRequestBodyThroughToService() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                "REC1.1", List.of(), "AI", "bundle-recommendations-v1");
        when(recommendationService.recommendProducts(any(RecommendationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bundles/bundle-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<RecommendationRequest> captor =
                org.mockito.ArgumentCaptor.forClass(RecommendationRequest.class);
        verify(recommendationService).recommendProducts(captor.capture());

        RecommendationRequest captured = captor.getValue();
        assertThat(captured.getCustomerReference()).isEqualTo("CUST-10291");
        assertThat(captured.getUsageProfile()).isNotNull();
        assertThat(captured.getUsageProfile().getPreferredValidityDays()).isEqualTo(30);
    }

    // ---- local IllegalArgumentException handler (takes precedence over the global advice) ----

    @Test
    void shouldReturn400WithMessageBodyWhenServiceThrowsIllegalArgumentException() throws Exception {
        when(recommendationService.recommendProducts(any(RecommendationRequest.class)))
                .thenThrow(new IllegalArgumentException("UsageProfile is required"));

        mockMvc.perform(post("/api/v1/bundles/bundle-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("UsageProfile is required")); // plain text, from the LOCAL handler
    }

    // ---- anything other than IllegalArgumentException falls through to the global advice ----

    @Test
    void shouldFallThroughToGlobalHandlerForNonIllegalArgumentExceptions() throws Exception {
        // This controller's local @ExceptionHandler only covers IllegalArgumentException.
        // Any other exception type is NOT caught locally and falls through to the global
        // ControllerAdvice's catch-all Exception handler instead, which returns a structured
        // JSON body (not the local handler's plain text) with a 500.
        when(recommendationService.recommendProducts(any(RecommendationRequest.class)))
                .thenThrow(new RuntimeException("AI provider exploded"));

        mockMvc.perform(post("/api/v2/bundles/bundle-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred")); // global advice's shape
    }
}