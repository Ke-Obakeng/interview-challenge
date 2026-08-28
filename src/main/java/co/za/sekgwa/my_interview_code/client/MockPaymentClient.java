package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.payment.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Component
public class MockPaymentClient implements PaymentClient {

    @Override
    public PaymentResult charge(PaymentRequest paymentRequest) {

        if(paymentRequest.getAmount().compareTo(new BigDecimal("500.00")) == 0) {
            return new PaymentResult(PaymentStatus.FAILED, null);
        }
        return new PaymentResult(PaymentStatus.SUCCESS, "TRX-" + randomNumberGen());
    }

    @Override
    public ReversalResult reverse(ReversalRequest reverseRequest) {

        return new ReversalResult(true);
    }

    private String randomNumberGen() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return String.format("%05d", randomNumber);
    }
}
