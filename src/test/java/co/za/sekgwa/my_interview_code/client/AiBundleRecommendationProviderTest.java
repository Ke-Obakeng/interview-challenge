package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.recommender_ai.UsageProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiBundleRecommendationProviderTest {

    private final AiBundleRecommendationProvider provider = new AiBundleRecommendationProvider();

    @Mock
    private RecommendationRequest recommendationRequest;

    @Mock
    private UsageProfile usageProfile;

    @BeforeEach
    void setUp() {
        lenient().when(recommendationRequest.getUsageProfile()).thenReturn(usageProfile);
        lenient().when(recommendationRequest.getCustomerReference()).thenReturn("CUST-10291");
    }

    private ProductCatalogue product(String code, String price, String type) {
        ProductCatalogue p = org.mockito.Mockito.mock(ProductCatalogue.class);
        lenient().when(p.getProductCode()).thenReturn(code);
        lenient().when(p.getPrice()).thenReturn(new BigDecimal(price));
        lenient().when(p.getType()).thenReturn(type);
        return p;
    }

    // ---- demo failure trigger ----

    @Test
    void shouldThrowAiRecommendationExceptionWhenCustomerReferenceIsForceFailureTrigger() {
        when(recommendationRequest.getCustomerReference()).thenReturn("CUST-AI-DOWN");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of()))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessageContaining("Simulated AI provider failure");
    }

    @Test
    void shouldTreatTriggerAsCaseInsensitive() {
        when(recommendationRequest.getCustomerReference()).thenReturn("cust-ai-down");

        assertThatThrownBy(() -> provider.recommend(recommendationRequest, List.of()))
                .isInstanceOf(AiRecommendationException.class);
    }

    @Test
    void shouldNotThrowForAnyOtherCustomerReference() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(product("PROD-001", "50", "PREPAID")));

        assertThat(result).hasSize(1);
    }

    // ---- budget filtering ----

    @Test
    void shouldExcludeProductsOverBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));

        ProductCatalogue cheap = product("PROD-CHEAP", "50", "PREPAID");
        ProductCatalogue expensive = product("PROD-EXP", "150", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(cheap, expensive));

        assertThat(result).containsExactly(cheap);
    }

    @Test
    void shouldIncludeProductAtExactlyTheBudget() {
        // Uses compareTo(maxBudget) <= 0, so a product priced exactly at budget IS included
        // here (unlike DeterministicBundleRecomProvider's strict < 0 check).
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));

        ProductCatalogue atBudget = product("PROD-AT", "100", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(atBudget));

        assertThat(result).containsExactly(atBudget);
    }

    @Test
    void shouldIncludeAllProductsWhenBudgetIsNull() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(null);

        ProductCatalogue expensive = product("PROD-EXP", "999999", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(expensive));

        assertThat(result).containsExactly(expensive);
    }

    @Test
    void shouldReturnEmptyListWhenNothingFitsBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(10));

        ProductCatalogue expensive = product("PROD-EXP", "500", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(expensive));

        assertThat(result).isEmpty();
    }

    // ---- usage-based type preference ----

    @Test
    void shouldPreferBundleTypeWhenDataUsageIsHigh() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(3000); // above the 2000 threshold
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        ProductCatalogue bundleProduct = product("PROD-BUNDLE", "50", "BUNDLE");
        ProductCatalogue prepaidProduct = product("PROD-PREPAID", "50", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(prepaidProduct, bundleProduct));

        assertThat(result).containsExactly(bundleProduct); // only the BUNDLE-type match returned
    }

    @Test
    void shouldPreferBundleTypeWhenVoiceUsageIsHigh() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(100);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(400); // above the 300 threshold

        ProductCatalogue bundleProduct = product("PROD-BUNDLE", "50", "BUNDLE");
        ProductCatalogue prepaidProduct = product("PROD-PREPAID", "50", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(prepaidProduct, bundleProduct));

        assertThat(result).containsExactly(bundleProduct);
    }

    @Test
    void shouldPreferPrepaidTypeWhenUsageIsLow() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(500); // below threshold
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(50); // below threshold

        ProductCatalogue bundleProduct = product("PROD-BUNDLE", "50", "BUNDLE");
        ProductCatalogue prepaidProduct = product("PROD-PREPAID", "50", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(bundleProduct, prepaidProduct));

        assertThat(result).containsExactly(prepaidProduct);
    }

    @Test
    void shouldFallBackToAllInBudgetProductsWhenNoPreferredTypeMatches() {
        // High usage -> prefers "BUNDLE", but none exist - should still return the in-budget
        // POSTPAID product rather than an empty list.
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(3000);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        ProductCatalogue postpaidProduct = product("PROD-POST", "50", "POSTPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(postpaidProduct));

        assertThat(result).containsExactly(postpaidProduct);
    }

    // ---- price-ascending ranking within the preferred type ----

    @Test
    void shouldRankCheapestFirstWithinPreferredType() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(100);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        ProductCatalogue pricier = product("PROD-PRICIER", "90", "PREPAID");
        ProductCatalogue cheaper = product("PROD-CHEAPER", "50", "PREPAID");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(pricier, cheaper));

        assertThat(result.get(0)).isEqualTo(cheaper); // ascending, opposite of the deterministic fallback
    }

    // ---- cap at 3 ----

    @Test
    void shouldLimitResultsToThreeEvenWithMoreEligibleProducts() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(100);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        List<ProductCatalogue> fourProducts = List.of(
                product("P1", "10", "PREPAID"),
                product("P2", "20", "PREPAID"),
                product("P3", "30", "PREPAID"),
                product("P4", "40", "PREPAID")
        );

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, fourProducts);

        assertThat(result).hasSize(3);
    }

    // ---- null usage profile defensiveness ----

    @Test
    void shouldTreatNullUsageProfileAsLowUsageAndNotThrow() {
        when(recommendationRequest.getUsageProfile()).thenReturn(null);

        ProductCatalogue prepaidProduct = product("PROD-PREPAID", "50", "PREPAID");

        // No budget/usage info at all - should not throw, and with a null profile the budget
        // filter is skipped entirely (maxBudget stays null -> everything passes).
        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(prepaidProduct));

        assertThat(result).containsExactly(prepaidProduct);
    }
}