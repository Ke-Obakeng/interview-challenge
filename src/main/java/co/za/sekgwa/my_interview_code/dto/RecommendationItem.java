package co.za.sekgwa.my_interview_code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationItem {
    private String productCode;
    private double confidence;
    private String source;

    public RecommendationItem(String productCode, double confidence, String source) {
        this.productCode = productCode;
        this.confidence = confidence;
        this.source = source;
    }
}
