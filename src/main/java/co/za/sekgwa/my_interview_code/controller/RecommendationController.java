package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.PurchaseStatusResponse;
import co.za.sekgwa.my_interview_code.dto.RecommendationResponse;
import co.za.sekgwa.my_interview_code.model.recommender_ai.RecommendationRequest;
import co.za.sekgwa.my_interview_code.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bundles")
@Tag(name = "AI Recommender", description = "Recommend bundles")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

//    @Operation
//    @GetMapping("/{recommendationId}")
//    public ResponseEntity<RecommendationResponse> getStatus(@PathVariable("recommendationId") String recommendationId) {
//        return ResponseEntity.ok(recommendationService.);
//    }

    @Operation(summary = "AI Recommendation",
            description = "Mocked AI recommender for suggesting bundles that can be bought. Top 3")

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
