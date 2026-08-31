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
                List.of(product("PROD-001", "50", "DATA")));

        assertThat(result).hasSize(1);
    }

    // ---- budget filtering ----

    @Test
    void shouldExcludeProductsOverBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));

        ProductCatalogue cheap = product("PROD-CHEAP", "50", "DATA");
        ProductCatalogue expensive = product("PROD-EXP", "150", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(cheap, expensive));

        assertThat(result).containsExactly(cheap);
    }

    @Test
    void shouldIncludeProductAtExactlyTheBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));

        ProductCatalogue atBudget = product("PROD-AT", "100", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(atBudget));

        assertThat(result).containsExactly(atBudget);
    }

    @Test
    void shouldIncludeAllProductsWhenBudgetIsNull() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(null);

        ProductCatalogue expensive = product("PROD-EXP", "999999", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(expensive));

        assertThat(result).containsExactly(expensive);
    }

    @Test
    void shouldReturnEmptyListWhenNothingFitsBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(10));

        ProductCatalogue expensive = product("PROD-EXP", "500", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(expensive));

        assertThat(result).isEmpty();
    }

    // ---- usage-based type preference ----

    @Test
    void shouldPreferDataTypeWhenDataUsageIsHighOnly() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(300); // > 200 MB threshold
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10); // <= 100 min threshold

        ProductCatalogue dataProduct = product("PROD-DATA", "50", "DATA");
        ProductCatalogue voiceProduct = product("PROD-VOICE", "50", "VOICE");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(voiceProduct, dataProduct));

        assertThat(result).containsExactly(dataProduct);
    }

    @Test
    void shouldPreferVoiceTypeWhenVoiceUsageIsHighOnly() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(100); // <= 200 MB threshold
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(400); // > 100 min threshold

        ProductCatalogue voiceProduct = product("PROD-VOICE", "50", "VOICE");
        ProductCatalogue dataProduct = product("PROD-DATA", "50", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(dataProduct, voiceProduct));

        assertThat(result).containsExactly(voiceProduct);
    }

    @Test
    void shouldPreferComboTypeWhenBothDataAndVoiceUsageAreHigh() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(300); // > 200 MB threshold
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(400); // > 100 min threshold

        ProductCatalogue comboProduct = product("PROD-COMBO", "50", "COMBO");
        ProductCatalogue dataProduct = product("PROD-DATA", "50", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(dataProduct, comboProduct));

        assertThat(result).containsExactly(comboProduct);
    }

    @Test
    void shouldFallBackToAllInBudgetProductsWhenNoPreferredTypeMatches() {
        // High data -> prefers "DATA", but only "VOICE" exists - falls back to voice product within budget
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(300);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        ProductCatalogue voiceProduct = product("PROD-VOICE", "50", "VOICE");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(voiceProduct));

        assertThat(result).containsExactly(voiceProduct);
    }

    // ---- price-ascending ranking within the preferred type ----

    @Test
    void shouldRankCheapestFirstWithinPreferredType() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(300);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        ProductCatalogue pricier = product("PROD-PRICIER", "90", "DATA");
        ProductCatalogue cheaper = product("PROD-CHEAPER", "50", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest,
                List.of(pricier, cheaper));

        assertThat(result.get(0)).isEqualTo(cheaper);
    }

    // ---- cap at 3 ----

    @Test
    void shouldLimitResultsToThreeEvenWithMoreEligibleProducts() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getAverageMonthlyDataMb()).thenReturn(300);
        when(usageProfile.getAverageMonthlyVoiceMinutes()).thenReturn(10);

        List<ProductCatalogue> fourProducts = List.of(
                product("P1", "10", "DATA"),
                product("P2", "20", "DATA"),
                product("P3", "30", "DATA"),
                product("P4", "40", "DATA")
        );

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, fourProducts);

        assertThat(result).hasSize(3);
    }

    // ---- null usage profile defensiveness ----

    @Test
    void shouldTreatNullUsageProfileAsLowUsageAndNotThrow() {
        when(recommendationRequest.getUsageProfile()).thenReturn(null);

        ProductCatalogue dataProduct = product("PROD-DATA", "50", "DATA");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(dataProduct));

        assertThat(result).containsExactly(dataProduct);
    }
}