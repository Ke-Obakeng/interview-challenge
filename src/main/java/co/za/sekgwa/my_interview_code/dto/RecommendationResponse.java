package co.za.sekgwa.my_interview_code.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class RecommendationResponse {

    private String recommendationId;
    private List<RecommendationItem> recommendations;
    private String source;
    private String promptVersion;

    public RecommendationResponse() {}

    public RecommendationResponse(String recommendationId, List<RecommendationItem> recommendations, String source, String promptVersion){
        this.recommendationId = recommendationId;
        this.recommendations = recommendations;
        this.promptVersion = promptVersion;
        this.source = source;
    }
}
