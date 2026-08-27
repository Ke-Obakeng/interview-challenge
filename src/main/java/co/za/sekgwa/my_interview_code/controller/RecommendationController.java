package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.RecommendationRequest;
import co.za.sekgwa.my_interview_code.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/bundles")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/bundle-recommendation")
    public ResponseEntity<RecommendationResponse> recommendBundles(@RequestBody RecommendationRequest recommendationRequest ) {

        RecommendationResponse recommendations = recommendationService.recommendProducts(recommendationRequest);

        return ResponseEntity.ok(recommendations);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
