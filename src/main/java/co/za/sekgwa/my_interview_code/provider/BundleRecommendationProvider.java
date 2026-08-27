package co.za.sekgwa.my_interview_code.provider;

import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.RecommendationRequest;

import java.util.List;

public interface BundleRecommendationProvider {
    List<ProductCatalogue> recommend(RecommendationRequest recommendationRequest, List<ProductCatalogue> availableProducts);
}
