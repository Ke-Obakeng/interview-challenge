package co.za.sekgwa.my_interview_code.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class PurchaseStatusResponse {

    private final String purchaseStatus;
    private final String paymentStatus;
    private final String provisioningStatus;
    private final String reference;
    private final Instant timestamp;

    public PurchaseStatusResponse(String purchaseStatus, String paymentStatus, String provisioningStatus, String reference, Instant timestamp) {
        this.purchaseStatus = purchaseStatus;
        this.paymentStatus = paymentStatus;
        this.provisioningStatus = provisioningStatus;
        this.reference = reference;
        this.timestamp = timestamp;
    }
}
