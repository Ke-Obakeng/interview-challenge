package co.za.sekgwa.my_interview_code.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class PurchaseResponse {

    private final String purchaseId;
    private final String status;
    private final String productCode;
    private final BigDecimal amount;
    private final String currency;

    public PurchaseResponse(String purchaseId, String status, String productCode, BigDecimal amount, String currency) {
        this.purchaseId = purchaseId;
        this.status = status;
        this.productCode = productCode;
        this.amount = amount;
        this.currency = currency;
    }
}
