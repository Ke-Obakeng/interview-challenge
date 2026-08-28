package co.za.sekgwa.my_interview_code.model.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class PaymentResult {

    private final PaymentStatus status;
    private final String transactionId;

    public PaymentResult(PaymentStatus status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }
}
