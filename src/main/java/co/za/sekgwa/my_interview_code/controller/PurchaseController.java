package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.PurchaseRequest;
import co.za.sekgwa.my_interview_code.dto.PurchaseResponse;
import co.za.sekgwa.my_interview_code.dto.PurchaseStatusResponse;
import co.za.sekgwa.my_interview_code.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bundle-purchases")
@Tag(name = "Purchases", description = "Purchase a product and check the status")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Operation(summary = "Purchase a bundle",
            description = "Idempotent - retrying with same idempotency-key returns the original isntead of charging again")
    @PostMapping
    public ResponseEntity<PurchaseResponse> purchase(
            @RequestBody PurchaseRequest purchaseRequest,
            @Parameter(description = "Unique key preventing duplicate purchases", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        PurchaseResponse response = purchaseService.purchase(purchaseRequest, idempotencyKey);
        HttpStatus status = "FAILED".equals(response.getStatus()) ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @Operation
    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseStatusResponse> getStatus(@PathVariable("purchaseId") String purchaseId) {
        return ResponseEntity.ok(purchaseService.getPurchaseStatus(purchaseId));
    }

}
