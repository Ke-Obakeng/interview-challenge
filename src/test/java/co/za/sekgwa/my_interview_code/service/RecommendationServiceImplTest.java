package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.client.ProductCatalogueClient;
import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.recommender_ai.UsageProfile;
import co.za.sekgwa.my_interview_code.provider.BundleRecommendationProvider;
import co.za.sekgwa.my_interview_code.provider.DeterministicBundleRecomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NOTE ON ASSUMED GETTERS: RecommendationItem / RecommendationResponse were not provided,
 * so this test assumes conventional getters matching the constructor call sites in
 * RecommendationServiceImpl: getRecommendationId(), getRecommendations(), getSource(),
 * getPromptVersion() on RecommendationResponse, and getProductCode(), getConfidence(),
 * getReason() on RecommendationItem. Adjust these calls if your actual DTOs differ.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private BundleRecommendationProvider aiRecommendationProvider;

    @Mock
    private DeterministicBundleRecomProvider fallbackProvider;

    @Mock
    private ProductCatalogueClient productCatalogueClient;

    @Mock
    private RecommendationRequest recommendationRequest;

    @Mock
    private UsageProfile usageProfile;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(aiRecommendationProvider, fallbackProvider, productCatalogueClient);
        lenient().when(recommendationRequest.getUsageProfile()).thenReturn(usageProfile);
        lenient().when(recommendationRequest.getCustomerReference()).thenReturn("CUST-10291");
    }

    private ProductCatalogue product(String code, String price, String validity) {
        ProductCatalogue p = mock(ProductCatalogue.class);
        lenient().when(p.getProductCode()).thenReturn(code);
        lenient().when(p.getPrice()).thenReturn(price == null ? null : new BigDecimal(price));
        lenient().when(p.getValidity()).thenReturn(validity);
        return p;
    }

    // ---- validateRequest ----

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> service.recommendProducts(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UsageProfile is required");
    }

    @Test
    void shouldThrowWhenUsageProfileIsNull() {
        when(recommendationRequest.getUsageProfile()).thenReturn(null);

        assertThatThrownBy(() -> service.recommendProducts(recommendationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UsageProfile is required");

        verifyNoInteractions(productCatalogueClient, aiRecommendationProvider, fallbackProvider);
    }

    // ---- AI success / fallback routing ----

    @Test
    void shouldUseAiRecommendationsAndTagSourceAsAiOnSuccess() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        assertThat(response.getSource()).isEqualTo("AI");
        assertThat(response.getRecommendations()).hasSize(1);
        verifyNoInteractions(fallbackProvider);
    }

    @Test
    void shouldFallBackToDeterministicProviderWhenAiThrows() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any()))
                .thenThrow(new AiRecommendationException("AI down", new RuntimeException()));
        when(fallbackProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        assertThat(response.getSource()).isEqualTo("fallback");
        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    void shouldNotSuppressNonAiRecommendationExceptionsFromAiProvider() {
        // Only AiRecommendationException triggers the fallback; any other RuntimeException
        // from the AI provider propagates straight out uncaught.
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of());
        when(aiRecommendationProvider.recommend(any(), any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.recommendProducts(recommendationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verifyNoInteractions(fallbackProvider);
    }

    @Test
    void shouldCapRecommendationsAtThreeEvenIfProviderReturnsMore() {
        List<ProductCatalogue> four = List.of(
                product("P1", "10", "30"),
                product("P2", "20", "30"),
                product("P3", "30", "30"),
                product("P4", "40", "30")
        );
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(productCatalogueClient.findAllProducts()).thenReturn(four);
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(four);

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        assertThat(response.getRecommendations()).hasSize(3);
    }

    // ---- response shape ----

    @Test
    void shouldGenerateRecommendationIdStartingWithRecPrefix() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        // recommendationId is "REC" + a raw double (e.g. "REC54213.778..."), not a UUID -
        // this test locks in that current (unformatted) shape rather than assuming a cleaner one.
        assertThat(response.getRecommendationId()).matches("REC-\\d{5}");
    }

    @Test
    void shouldIncludePromptVersionInResponse() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        assertThat(response.getPromptVersion()).isEqualTo("bundle-recommendations-v1");
    }

    @Test
    void shouldUseSameReasonTextForEveryRecommendedItem() {
        // NOTE: RecommendationItem has no "reason" field - its third field is named "source".
        // The service passes the reason string into that constructor slot (see next test for
        // why this is a real bug), so getSource() on an item actually returns the reason text.
        List<ProductCatalogue> products = List.of(
                product("P1", "10", "30"),
                product("P2", "20", "30")
        );
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(productCatalogueClient.findAllProducts()).thenReturn(products);
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(products);

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        assertThat(response.getRecommendations())
                .allSatisfy(item -> assertThat(item.getSource())
                        .isEqualTo("Closest products to the supplied monthly usage"));
    }

    @Test
    void shouldRevealBugWherePerItemSourceHoldsReasonTextInsteadOfAiOrFallback() {
        // KNOWN BUG: RecommendationItem's "source" field is populated with the reason string
        // ("Closest products to the supplied monthly usage") in mapToResponse(), not with
        // "AI" or "fallback". Only RecommendationResponse.getSource() (the top-level field)
        // correctly reflects which provider actually served the recommendation.
        // This test locks in the current (likely unintended) behaviour so it's visible rather
        // than silently relied upon.
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        String topLevelSource = response.getSource();
        String itemLevelSource = response.getRecommendations().get(0).getSource();

        assertThat(topLevelSource).isEqualTo("AI");
        assertThat(itemLevelSource).isNotEqualTo("AI"); // it's the reason text instead, not the source
        assertThat(itemLevelSource).isEqualTo("Closest products to the supplied monthly usage");
    }

    // ---- confidence scoring ----

    @Test
    void confidenceShouldBeWithinValidRangeForReasonableInputs() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        double confidence = response.getRecommendations().get(0).getConfidence();
        assertThat(confidence).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
    }

    @Test
    void confidenceShouldDropToZeroBudgetComponentWhenPriceExceedsBudget() {
        ProductCatalogue overBudget = product("PROD-EXP", "999", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(overBudget));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(overBudget));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        // validity is a perfect match (0.6 weight) but budgetScore=0 -> combined caps at 0.6
        double confidence = response.getRecommendations().get(0).getConfidence();
        assertThat(confidence).isEqualTo(0.6);
    }

    @Test
    void confidenceShouldDefaultValidityScoreToFullWhenPreferredValidityDaysIsNull() {
        ProductCatalogue p = product("PROD-004", "99", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(null);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(160));
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(p));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(p));

        RecommendationResponse response = service.recommendProducts(recommendationRequest);

        // validityScore stays at its default of 1.0, budgetScore = 99/160 = 0.6188 -> rounds per formula
        double confidence = response.getRecommendations().get(0).getConfidence();
        assertThat(confidence).isGreaterThan(0.0);
    }

    // ---- BUG: NPE when validity string is non-null but unparsable ----

    @Test
    void shouldThrowNullPointerExceptionWhenValidityStringHasNoDigits() {
        // KNOWN BUG: getValidity() returning a non-null, no-digit string like "unlimited"
        // causes parseValidity() to return null, which is then unboxed in
        // Math.abs(parseValidity(...) - preferredValidityDays) -> NullPointerException.
        // This test documents the CURRENT behaviour so a future fix has a red test to turn green.
        ProductCatalogue badValidity = product("PROD-BAD", "99", "unlimited");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(badValidity));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(badValidity));

        assertThatThrownBy(() -> service.recommendProducts(recommendationRequest))
                .isInstanceOf(NullPointerException.class);
    }

    // ---- BUG: ArithmeticException when budget and price are both exactly zero ----

    @Test
    void shouldThrowArithmeticExceptionWhenBudgetAndPriceAreBothZero() {
        // KNOWN BUG: when maximumBudget and price are both BigDecimal.ZERO, price.compareTo(budget)
        // is 0 (not > 0), so execution falls into price.divide(budget, ...) -> divide by zero.
        ProductCatalogue freeProduct = product("PROD-FREE", "0", "30");
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.ZERO);
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of(freeProduct));
        when(aiRecommendationProvider.recommend(any(), any())).thenReturn(List.of(freeProduct));

        assertThatThrownBy(() -> service.recommendProducts(recommendationRequest))
                .isInstanceOf(ArithmeticException.class);
    }
}