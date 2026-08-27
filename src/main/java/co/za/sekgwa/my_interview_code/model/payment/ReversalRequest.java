package co.za.sekgwa.my_interview_code.model.payment;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReversalRequest {

    private final String transactionId;

    public ReversalRequest(String transactionId) {
        this.transactionId = transactionId;
    }
}
