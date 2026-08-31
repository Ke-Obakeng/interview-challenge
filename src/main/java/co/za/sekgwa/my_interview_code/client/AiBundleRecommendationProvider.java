package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.model.recommender_ai.UsageProfile;
import co.za.sekgwa.my_interview_code.provider.BundleRecommendationProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component("aiBundleRecommendationProvider")
public class AiBundleRecommendationProvider implements BundleRecommendationProvider {

    private static final int HIGH_USAGE_DATA_MB_THRESHOLD = 200;
    private static final int HIGH_USAGE_VOICE_MINUTES_THRESHOLD = 100;
    private static final String FORCE_AI_FAILURE_TRIGGER = "CUST-AI-DOWN";

    @Override
    public List<ProductCatalogue> recommend(RecommendationRequest recommendationRequest, List<ProductCatalogue> availableProducts) {

        if (FORCE_AI_FAILURE_TRIGGER.equalsIgnoreCase(recommendationRequest.getCustomerReference())) {
            throw new AiRecommendationException(
                    "Simulated AI provider failure (triggered by customerReference=" + FORCE_AI_FAILURE_TRIGGER + ")",
                    new RuntimeException("simulated failure - not a real error"));
        }

        UsageProfile profile = recommendationRequest.getUsageProfile();
        BigDecimal maxBudget = profile != null ? profile.getGetMaximumBudget() : null;

        //Determine product
        String preferredType = isHighUsage(profile);

        List<ProductCatalogue> withinBudget = availableProducts.stream()
                .filter(p -> maxBudget == null || p.getPrice().compareTo(maxBudget) <= 0)
                .toList();
        //filter by preferredType
        List<ProductCatalogue> preferredTypeMatches = (preferredType != null) ? withinBudget.stream()
                .filter(p -> preferredType.equalsIgnoreCase(p.getType()))
                .sorted(Comparator.comparing(ProductCatalogue::getPrice)) // best value first
                .toList()
                : Collections.emptyList();
        //fallback if no preferred matches found
        List<ProductCatalogue> ranked = !preferredTypeMatches.isEmpty()
                ? preferredTypeMatches
                : withinBudget.stream()
                .sorted(Comparator.comparing(ProductCatalogue::getPrice))
                .toList();

        return ranked.stream().limit(3).toList();

//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("customerReference",recommendationRequest.getCustomerReference() );
//        requestBody.put("usageProfile", recommendationRequest.getUsageProfile());
//        requestBody.put("availableProducts", availableProducts);
//
//        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
//
//        try{
//            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, httpEntity, String.class);
//
//            return parseRecommendedproducts(response.getBody(), availableProducts);
//        }catch(RestClientException e){
//            throw new AiRecommendationException("AI provider not available", e);
//
//        }

    }

    private String isHighUsage(UsageProfile profile) {
        if (profile == null) {
            return null;
        }
        boolean highData = profile.getAverageMonthlyDataMb() != null
                && profile.getAverageMonthlyDataMb() > HIGH_USAGE_DATA_MB_THRESHOLD;
        boolean highVoice = profile.getAverageMonthlyVoiceMinutes() != null
                && profile.getAverageMonthlyVoiceMinutes() > HIGH_USAGE_VOICE_MINUTES_THRESHOLD;

        if(highData && highVoice) {
            return "COMBO";
        }else if(highData) {
            return "DATA";
        }else if(highVoice) {
            return "VOICE";
        }
        return null;
    }
}
