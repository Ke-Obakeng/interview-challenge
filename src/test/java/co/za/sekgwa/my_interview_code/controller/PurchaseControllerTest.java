package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.PurchaseRequest;
import co.za.sekgwa.my_interview_code.dto.PurchaseResponse;
import co.za.sekgwa.my_interview_code.dto.PurchaseStatusResponse;
import co.za.sekgwa.my_interview_code.exception.PurchaseNotFoundException;
import co.za.sekgwa.my_interview_code.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseService purchaseService;

    private static final String REQUEST_BODY = """
            {
              "customerReference": "CUST-10291",
              "productCode": "PROD-004",
              "paymentMethod": "CARD",
              "channel": "APP",
              "msisdn": "+27821234567"
            }
            """;

    // ---- POST /api/bundle-purchases ----

    @Test
    void shouldReturn200WithSuccessfulPurchase() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
                "PUR-20260828-00001", "SUCCESSFUL", "PROD-004", BigDecimal.valueOf(99.00), "ZAR");
        when(purchaseService.purchase(any(PurchaseRequest.class), eq("idem-8841-001"))).thenReturn(response);

        mockMvc.perform(post("/api/bundle-purchases")
                        .header("Idempotency-Key", "idem-8841-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value("PUR-20260828-00001"))
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.productCode").value("PROD-004"))
                .andExpect(jsonPath("$.currency").value("ZAR"));
    }

    @Test
    void shouldReturn422WhenPurchaseStatusIsFailed() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
                "PUR-20260828-00002", "FAILED", "PROD-004", BigDecimal.valueOf(99.00), "ZAR");
        when(purchaseService.purchase(any(PurchaseRequest.class), eq("idem-fail-001"))).thenReturn(response);

        mockMvc.perform(post("/api/bundle-purchases")
                        .header("Idempotency-Key", "idem-fail-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isUnprocessableEntity()) // asserts code 422, independent of the enum name used
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void shouldPassRequestBodyAndIdempotencyKeyThroughToService() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
                "PUR-20260828-00003", "SUCCESSFUL", "PROD-004", BigDecimal.valueOf(99.00), "ZAR");
        when(purchaseService.purchase(any(PurchaseRequest.class), eq("idem-8841-002"))).thenReturn(response);

        mockMvc.perform(post("/api/bundle-purchases")
                        .header("Idempotency-Key", "idem-8841-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<PurchaseRequest> captor = org.mockito.ArgumentCaptor.forClass(PurchaseRequest.class);
        org.mockito.Mockito.verify(purchaseService).purchase(captor.capture(), eq("idem-8841-002"));

        PurchaseRequest captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.getCustomerReference()).isEqualTo("CUST-10291");
        org.assertj.core.api.Assertions.assertThat(captured.getProductCode()).isEqualTo("PROD-004");
        org.assertj.core.api.Assertions.assertThat(captured.getMsisdn()).isEqualTo("+27821234567");
    }

    @Test
    void shouldReturn500WhenIdempotencyKeyHeaderIsCompletelyMissing() throws Exception {
        // Spring throws MissingRequestHeaderException before the controller body runs. The global
        // ControllerAdvice has no specific handler for it, so it falls to the generic Exception
        // catch-all -> 500, not a clean 400. Documents current behaviour; update if a handler
        // for MissingRequestHeaderException is added later.
        mockMvc.perform(post("/api/bundle-purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn400WhenIdempotencyKeyHeaderIsPresentButBlank() throws Exception {
        // Different from the missing-header case: an empty header value satisfies Spring's
        // @RequestHeader presence check and reaches the service, whose own validation throws
        // IllegalArgumentException - which IS handled globally -> a clean 400.
        when(purchaseService.purchase(any(PurchaseRequest.class), eq("")))
                .thenThrow(new IllegalArgumentException("Idempotency Key cannot be null or blank"));

        mockMvc.perform(post("/api/bundle-purchases")
                        .header("Idempotency-Key", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Idempotency Key cannot be null or blank"));
    }

    // ---- GET /api/bundle-purchases/{purchaseId} ----

    @Test
    void shouldReturnPurchaseStatusByPurchaseId() throws Exception {
        PurchaseStatusResponse statusResponse = new PurchaseStatusResponse(
                "SUCCESSFUL", "SUCCESS", "SUCCESS", "PROV-1234",
                Instant.parse("2026-08-27T10:15:30.123Z"));
        when(purchaseService.getPurchaseStatus("PUR-20260828-00001")).thenReturn(statusResponse);

        mockMvc.perform(get("/api/bundle-purchases/PUR-20260828-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseStatus").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.provisioningStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.reference").value("PROV-1234"));
    }

    @Test
    void shouldReturn404WhenPurchaseIdNotFound() throws Exception {
        when(purchaseService.getPurchaseStatus("PUR-missing"))
                .thenThrow(new PurchaseNotFoundException("PUR-missing"));

        mockMvc.perform(get("/api/bundle-purchases/PUR-missing"))
                .andExpect(status().isNotFound());
    }
}