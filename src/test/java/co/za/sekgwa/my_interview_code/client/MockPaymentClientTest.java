package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.payment.PaymentRequest;
import co.za.sekgwa.my_interview_code.model.payment.PaymentResult;
import co.za.sekgwa.my_interview_code.model.payment.PaymentStatus;
import co.za.sekgwa.my_interview_code.model.payment.ReversalRequest;
import co.za.sekgwa.my_interview_code.model.payment.ReversalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentClientTest {

    private MockPaymentClient paymentClient;
    private PaymentRequest samplePaymentRequest;

    @BeforeEach
    void setUp() {
        paymentClient = new MockPaymentClient();
        samplePaymentRequest = new PaymentRequest(
                "CUST-001",
                "CREDIT_CARD",
                new BigDecimal("250.00"),
                "ZAR"
        );
    }

    @Test
    void charge_ShouldReturnSuccessStatusAndValidTransactionId() {
        PaymentResult result = paymentClient.charge(samplePaymentRequest);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getTransactionId()).isNotNull();
        assertThat(result.getTransactionId()).startsWith("TRX-");
    }

    @Test
    void charge_ShouldGenerateUniqueTransactionIdsOnSubsequentCalls() {
        PaymentResult result1 = paymentClient.charge(samplePaymentRequest);
        PaymentResult result2 = paymentClient.charge(samplePaymentRequest);

        assertThat(result1.getTransactionId()).isNotEqualTo(result2.getTransactionId());
    }

    @Test
    void reverse_ShouldReturnSuccessfulReversalResult() {
        ReversalRequest reversalRequest = new ReversalRequest("TRX-12345678-abcd");

        ReversalResult result = paymentClient.reverse(reversalRequest);

        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
    }
}