package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;

public interface RecommendationService {
    //List<ProductCatalogue> recommendProducts(RecommendationRequest recommendationRequest);
    RecommendationResponse recommendProducts(RecommendationRequest recommendationRequest);
}
