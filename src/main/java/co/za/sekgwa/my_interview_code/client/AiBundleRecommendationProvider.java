package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.exception.AiRecommendationException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
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

import java.util.*;
import java.util.stream.Collectors;

@Component("aiBundleRecommendationProvider")
public class AiBundleRecommendationProvider implements BundleRecommendationProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiBundleRecommendationProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<ProductCatalogue> recommend(RecommendationRequest recommendationRequest, List<ProductCatalogue> availableProducts) {

        String endpoint = "http://localhost:8080/api/v1/bundle-recommendations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerReference",recommendationRequest.getCustomerReference() );
        requestBody.put("usageProfile", recommendationRequest.getUsageProfile());
        //requestBody.put("availableProducts", availableProducts);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try{
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, httpEntity, String.class);

            return parseRecommendedproducts(response.getBody(), availableProducts);
        }catch(RestClientException e){
            throw new AiRecommendationException("AI provider not available", e);

        }

    }

    private List<ProductCatalogue> parseRecommendedproducts(String responseBody, List<ProductCatalogue> availableProducts) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode recommendedProductCodes = rootNode.get("recommendedId");

            List<String> productCodes = new ArrayList<>();

            recommendedProductCodes.forEach(n -> productCodes.add(n.asString()));

            return availableProducts.stream()
                    .filter( product -> productCodes.contains(product.getProductCode()))
                    .limit(3)
                    .collect(Collectors.toList());

        }catch (Exception e) {
            throw new AiRecommendationException("Failed to parse AI recommendation", e);
        }
    }
}
