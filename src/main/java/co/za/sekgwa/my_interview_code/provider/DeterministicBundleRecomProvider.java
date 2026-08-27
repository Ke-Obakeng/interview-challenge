package co.za.sekgwa.my_interview_code.provider;

import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.UsageProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class DeterministicBundleRecomProvider implements BundleRecommendationProvider{

    @Override
    public List<ProductCatalogue> recommend(RecommendationRequest recommendationRequest, List<ProductCatalogue> availableProducts) {

        UsageProfile profile = recommendationRequest.getUsageProfile();
        BigDecimal maxBudget = profile.getGetMaximumBudget();
        Integer preferredValidity = profile.getPreferredValidityDays();

        return availableProducts.stream()
                .filter(product -> maxBudget == null || product.getPrice().compareTo(maxBudget) < 0)
                .sorted(Comparator
                        .comparingInt((ProductCatalogue product) -> validityDistance(parseValidity(product.getValidity()), preferredValidity))
                        .thenComparing(Comparator.comparing(ProductCatalogue::getPrice).reversed()))
                .limit(3)
                .toList();
    }

    private int validityDistance(Integer validity, Integer preferredValidity) {
        if(validity == null || preferredValidity == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(validity - preferredValidity);
    }

    private Integer parseValidity(String validity) {

        if(validity == null || validity.isBlank()) {
            return null;
        }

        try {
            String digitsOnly = validity.replaceAll("[^0-9]", "");
            return digitsOnly.isBlank() ? null : Integer.parseInt(digitsOnly);
        }catch (NumberFormatException e) {
            return null;
        }
    }
}
