package co.za.sekgwa.my_interview_code.model.payment;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentRequest {

    private final String customerReference;
    private final String paymentMethod;
    private final BigDecimal amount;
    private final String currency;

    public PaymentRequest(String customerReference, String paymentMethod, BigDecimal amount, String currency) {
        this.customerReference = customerReference;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
    }
}
