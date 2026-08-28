package co.za.sekgwa.my_interview_code.provider;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeterministicBundleRecomProviderTest {

    private final DeterministicBundleRecomProvider provider = new DeterministicBundleRecomProvider();

    @Mock
    private RecommendationRequest recommendationRequest;

    @Mock
    private UsageProfile usageProfile;

    @BeforeEach
    void setUp() {
        lenient().when(recommendationRequest.getUsageProfile()).thenReturn(usageProfile);
    }

    private ProductCatalogue product(String code, String price, String validity) {
        ProductCatalogue p = org.mockito.Mockito.mock(ProductCatalogue.class);
        lenient().when(p.getPrice()).thenReturn(price == null ? null : new BigDecimal(price));
        lenient().when(p.getValidity()).thenReturn(validity);
        return p;
    }

    @Test
    void shouldExcludeProductWithPriceExactlyEqualToBudget() {
        // The filter uses compareTo(maxBudget) < 0, which is strictly-less-than,
        // so a product priced exactly at the budget is deliberately excluded here.
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue atBudget = product("PROD-AT-BUDGET", "100", "30");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(atBudget));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExcludeProductOverBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue overBudget = product("PROD-OVER", "150", "30");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(overBudget));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIncludeProductStrictlyUnderBudget() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(100));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue underBudget = product("PROD-UNDER", "99", "30");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(underBudget));

        assertThat(result).extracting(ProductCatalogue::getPrice).containsExactly(BigDecimal.valueOf(99));
    }

    @Test
    void shouldIncludeAllProductsWhenBudgetIsNull() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(null);
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue expensive = product("PROD-EXP", "999999", "30");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(expensive));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRankClosestValidityMatchFirst() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue duration30 = product("PROD-30", "50", "30");
        ProductCatalogue duration7 = product("PROD-7", "50", "7");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(duration7, duration30));

        assertThat(result.get(0)).isEqualTo(duration30);
    }

    @Test
    void shouldParseValidityStringsContainingNonNumericCharacters() {
        // Exercises parseValidity() indirectly: "30 days" -> 30, matching preferred=30 exactly
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue withUnits = product("PROD-UNITS", "50", "30 days");
        ProductCatalogue farOff = product("PROD-FAR", "50", "1 day");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(farOff, withUnits));

        assertThat(result.get(0)).isEqualTo(withUnits);
    }

    @Test
    void shouldTreatNullValidityAsUnknownAndRankItLast() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue knownValidity = product("PROD-KNOWN", "50", "30");
        ProductCatalogue nullValidity = product("PROD-NULL", "50", null);

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(nullValidity, knownValidity));

        assertThat(result.get(0)).isEqualTo(knownValidity);
        assertThat(result.get(1)).isEqualTo(nullValidity);
    }

    @Test
    void shouldTreatUnparsableValidityAsUnknownRatherThanThrow() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue garbageValidity = product("PROD-GARBAGE", "50", "unlimited");

        // Should not throw NumberFormatException - garbage validity is swallowed and treated as unknown
        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(garbageValidity));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldTreatBlankValidityAsUnknown() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue blankValidity = product("PROD-BLANK", "50", "   ");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(blankValidity));

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldPreferHigherPriceWhenValidityDistanceIsTied() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        ProductCatalogue cheaper = product("PROD-CHEAP", "50", "30");
        ProductCatalogue pricier = product("PROD-PRICIER", "90", "30");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(cheaper, pricier));

        assertThat(result.get(0)).isEqualTo(pricier);
    }

    @Test
    void shouldLimitResultsToThreeEvenWithMoreEligibleProducts() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        List<ProductCatalogue> fourProducts = List.of(
                product("P1", "10", "30"),
                product("P2", "20", "30"),
                product("P3", "30", "30"),
                product("P4", "40", "30")
        );

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, fourProducts);

        assertThat(result).hasSize(3);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsAvailable() {
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(30);

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFallBackToPriceOrderingWhenPreferredValidityIsNull() {
        // preferredValidity == null -> validityDistance() always returns MAX_VALUE for every product,
        // so the comparator effectively falls through to price descending for all of them.
        when(usageProfile.getGetMaximumBudget()).thenReturn(BigDecimal.valueOf(1000));
        when(usageProfile.getPreferredValidityDays()).thenReturn(null);

        ProductCatalogue cheaper = product("PROD-CHEAP", "50", "30");
        ProductCatalogue pricier = product("PROD-PRICIER", "90", "7");

        List<ProductCatalogue> result = provider.recommend(recommendationRequest, List.of(cheaper, pricier));

        assertThat(result.get(0)).isEqualTo(pricier);
    }
}