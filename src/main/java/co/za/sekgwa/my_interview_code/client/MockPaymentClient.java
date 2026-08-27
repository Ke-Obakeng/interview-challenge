package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.payment.*;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class MockPaymentClient implements PaymentClient {

    @Override
    public PaymentResult charge(PaymentRequest paymentRequest) {

        return new PaymentResult(PaymentStatus.SUCCESS, "TRX-" + UUID.randomUUID());
    }

    @Override
    public ReversalResult reverse(ReversalRequest reverseRequest) {

        return new ReversalResult(true);
    }
}
