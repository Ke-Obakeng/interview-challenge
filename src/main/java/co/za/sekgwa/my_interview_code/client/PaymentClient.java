package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.payment.PaymentRequest;
import co.za.sekgwa.my_interview_code.model.payment.PaymentResult;
import co.za.sekgwa.my_interview_code.model.payment.ReversalRequest;
import co.za.sekgwa.my_interview_code.model.payment.ReversalResult;

public interface PaymentClient {
    PaymentResult charge(PaymentRequest paymentRequest);
    ReversalResult reverse(ReversalRequest reversalRequest);
}
