package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.recommender_ai.UsageProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Note: ObjectMapper is instantiated directly inside AiBundleRecommendationProvider
 * (not injected), so it can't be mocked - these tests instead supply real JSON strings
 * as the mocked RestTemplate's response body and let the real Jackson 3 ObjectMapper
 * parse them, which exercises the actual parsing logic rather than a stubbed shortcut.
 */
@ExtendWith(MockitoExtension.class)
class AiBundleRecommendationProviderTest {

    private static final String ENDPOINT = "http://localhost:8080/api/v1/bundle-recommendations";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RecommendationRequest recommendationRequest;

    @Mock
    private UsageProfile usageProfile;

    private AiBundleRecommendationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AiBundleRecommendationProvider(restTemplate);
        lenient().when(recommendationRequest.getCustomerReference()).thenReturn("CUST-10291");
        lenient().when(recommendationRequest.getUsageProfile()).thenReturn(usageProfile);
    }

    private ProductCatalogue product(String productCode) {
        ProductCatalogue p = mock(ProductCatalogue.class);
        lenient().when(p.getProductCode()).thenReturn(productCode);
        return p;
    }

    @SuppressWarnings("unchecked")
    private void mockAiResponse(String responseBody) {
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, org.springframework.http.HttpStatus.OK);
        when(restTemplate.postForEntity(eq(ENDPOINT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);
    }

    @Test
    void shouldCallTheExpectedEndpointWithCustomerReferenceAndUsageProfileInBody() {
        mockAiResponse("{ \"recommendedId\": [] }");
        ProductCatalogue p1 = product("PROD-001");

        provider.recommend(recommendationRequest, List.of(p1));

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(ENDPOINT), captor.capture(), eq(String.class));

        Map<String, Object> body = captor.getValue().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("customerReference")).isEqualTo("CUST-10291");
        assertThat(body.get("usageProfile")).isEqualTo(usageProfile);
        assertThat(body).doesNotContainKey("availableProducts"); // this line is currently commented out in the source
    }

    @Test
    void shouldReturnProductsMatchingRecommendedCodes() {
        mockAiResponse("{ \"recommendedId\": [\"PROD-001\", \"PROD-004\"] }");

        ProductCatalogue p1 = product("PROD-001");
        ProductCatalogue p2 = product("PROD-002");
        ProductCatalogue p4 = product("PROD-004");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(p1, p2, p4));

        assertThat(result).containsExactly(p1, p4); // p2 excluded, order follows availableProducts, not the AI's array order
    }

    @Test
    void shouldPreserveAvailableProductsOrderNotAiReturnedOrder() {
        // AI returns PROD-004 first, but PROD-001 appears first in availableProducts -
        // the implementation's final filter preserves availableProducts' order.
        mockAiResponse("{ \"recommendedId\": [\"PROD-004\", \"PROD-001\"] }");

        ProductCatalogue p1 = product("PROD-001");
        ProductCatalogue p4 = product("PROD-004");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(p1, p4));

        assertThat(result).containsExactly(p1, p4);
    }

    @Test
    void shouldLimitResultsToThreeEvenIfMoreCodesRecommended() {
        mockAiResponse("{ \"recommendedId\": [\"P1\", \"P2\", \"P3\", \"P4\"] }");

        List<ProductCatalogue> available = List.of(
                product("P1"), product("P2"), product("P3"), product("P4"));

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, available);

        assertThat(result).hasSize(3);
    }

    @Test
    void shouldReturnEmptyListWhenNoAvailableProductMatchesRecommendedCodes() {
        mockAiResponse("{ \"recommendedId\": [\"PROD-999\"] }");

        ProductCatalogue p1 = product("PROD-001");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(p1));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldWrapRestClientExceptionInAiRecommendationException() {
        when(restTemplate.postForEntity(eq(ENDPOINT), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));

        ProductCatalogue p1 = product("PROD-001");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of(p1)))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessageContaining("AI provider not available")
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void shouldWrapMalformedJsonResponseInAiRecommendationException() {
        mockAiResponse("this is not valid json {{{");

        ProductCatalogue p1 = product("PROD-001");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of(p1)))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessageContaining("Failed to parse AI recommendation");
    }

    @Test
    void shouldWrapMissingRecommendedIdFieldInAiRecommendationException() {
        // "recommendedId" is absent entirely -> rootNode.get("recommendedId") returns null,
        // and the subsequent .forEach(...) on that null throws NPE, which is caught and wrapped.
        mockAiResponse("{ \"someOtherField\": [] }");

        ProductCatalogue p1 = product("PROD-001");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of(p1)))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessageContaining("Failed to parse AI recommendation");
    }

    @Test
    void shouldWrapNullResponseBodyInAiRecommendationException() {
        when(restTemplate.postForEntity(eq(ENDPOINT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>((String) null, org.springframework.http.HttpStatus.OK));

        ProductCatalogue p1 = product("PROD-001");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of(p1)))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessageContaining("Failed to parse AI recommendation");
    }

    @Test
    void shouldReturnEmptyListWhenAvailableProductsIsEmpty() {
        mockAiResponse("{ \"recommendedId\": [\"PROD-001\"] }");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of());

        assertThat(result).isEmpty();
    }
}