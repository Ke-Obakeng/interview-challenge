package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.client.ProductCatalogueClient;
import co.za.sekgwa.my_interview_code.dto.RecommendationItem;
import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.UsageProfile;
import co.za.sekgwa.my_interview_code.provider.BundleRecommendationProvider;
import co.za.sekgwa.my_interview_code.provider.DeterministicBundleRecomProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final BundleRecommendationProvider aiRecommendationProvider;
    private final DeterministicBundleRecomProvider fallbackProvider;
    private final ProductCatalogueClient productCatalogueClient;

    public RecommendationServiceImpl(
            @Qualifier("aiBundleRecommendationProvider") BundleRecommendationProvider  aiRecommendationProvider,
            DeterministicBundleRecomProvider fallbackProvider,
            ProductCatalogueClient productCatalogueClient
            ) {
        this.aiRecommendationProvider = aiRecommendationProvider;
        this.fallbackProvider = fallbackProvider;
        this.productCatalogueClient = productCatalogueClient;
    }

    @Override
    public RecommendationResponse recommendProducts(RecommendationRequest recommendationRequest) {

        validateRequest(recommendationRequest);

        List<ProductCatalogue> availableProducts = productCatalogueClient.findAllProducts();

        List<ProductCatalogue> recommendations;
        String source;
        String promptVersion = "bundle-recommendations-v1";
        String reason = "Closest products to the supplied monthly usage";

        try {
            recommendations = aiRecommendationProvider.recommend(recommendationRequest, availableProducts);
            source = "AI";
            log.info("AI Recommendation succeeded for customer {}", recommendationRequest.getCustomerReference());
        }catch(AiRecommendationException e){
            log.warn("AI recommendation unavailable, fallingback", recommendationRequest.getCustomerReference(), e.getMessage());
            recommendations = fallbackProvider.recommend(recommendationRequest, availableProducts);
            source = "fallback";
        }

        recommendations = recommendations.stream().limit(3).toList();

        return mapToResponse(recommendations, recommendationRequest.getUsageProfile(), source, promptVersion, reason);
    }

    private RecommendationResponse mapToResponse(List<ProductCatalogue> products, UsageProfile usageProfile, String source, String prompt, String reason) {

        double num = (Math.random() * 90000) + 10000;
        String recommendationId = "REC" + num;

        List<RecommendationItem> items = products.stream()
                .map(product -> new RecommendationItem(
                        product.getProductCode(),
                        calculateConfidence(product, usageProfile),
                        reason))
                .toList();

        return new RecommendationResponse(recommendationId, items, source, prompt);
    }

    private double calculateConfidence(ProductCatalogue products, UsageProfile usageProfile) {

        double validityScore = 1.0;

        if(usageProfile.getPreferredValidityDays() != null && products.getValidity() != null) {
            int distance = Math.abs(parseValidity(products.getValidity()) - usageProfile.getPreferredValidityDays());
            validityScore = Math.max(0.0, 1.0 - (distance / 10.0) * 0.1);
        }

            double budgetScore = 1.0;

            if(usageProfile.getGetMaximumBudget() != null && products.getPrice() != null) {
                BigDecimal budget = usageProfile.getGetMaximumBudget();
                BigDecimal price = products.getPrice();

                if(price.compareTo(budget) > 0) {
                    budgetScore = 0.0;
                }else {
                    double ratio = price.divide(budget, 4, RoundingMode.HALF_UP).doubleValue();
                    budgetScore = ratio;
                }
            }
        double combined = (validityScore * 0.6) + (budgetScore * 0.4);
            return Math.round(combined * 100.0) / 100.0;
    }

    private void validateRequest(RecommendationRequest recommendationRequest) {
        if (recommendationRequest == null || recommendationRequest.getUsageProfile() == null) {
            throw new IllegalArgumentException("UsageProfile is required");
        }
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
