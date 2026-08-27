package co.za.sekgwa.my_interview_code.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
public class RecommendationRequest {

    private String customerReference;
    private UsageProfile usageProfile;

    public RecommendationRequest() {}


//    public RecommendationRequest(String customerReference, Map<String, Object> usageAttributes) {
//        this.customerReference = customerReference;
//        this.usageAttributes = usageAttributes;
//    }
}
