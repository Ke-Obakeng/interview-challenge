package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.client.EligibilityClient;
import co.za.sekgwa.my_interview_code.client.PaymentClient;
import co.za.sekgwa.my_interview_code.client.ProductCatalogueClient;
import co.za.sekgwa.my_interview_code.client.ProvisioningClient;
import co.za.sekgwa.my_interview_code.dto.PurchaseRequest;
import co.za.sekgwa.my_interview_code.dto.PurchaseResponse;
import co.za.sekgwa.my_interview_code.dto.PurchaseStatusResponse;
import co.za.sekgwa.my_interview_code.entity.PurchaseEntity;
import co.za.sekgwa.my_interview_code.exception.ProductNotFoundException;
import co.za.sekgwa.my_interview_code.exception.PurchaseNotFoundException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.model.ProvisioningStatus;
import co.za.sekgwa.my_interview_code.model.eligibility.EligibilityResult;
import co.za.sekgwa.my_interview_code.model.payment.PaymentResult;
import co.za.sekgwa.my_interview_code.model.payment.PaymentStatus;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningResult;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningStatusResult;
import co.za.sekgwa.my_interview_code.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private ProductCatalogueClient productCatalogueClient;

    @Mock
    private EligibilityClient eligibilityClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ProvisioningClient provisioningClient;

    @Mock
    private PurchaseRepository purchaseRepository;

    private PurchaseServiceImpl service;
    private ProductCatalogue product;

    @BeforeEach
    void setUp() {
        service = new PurchaseServiceImpl(productCatalogueClient, eligibilityClient,paymentClient, provisioningClient, purchaseRepository);
        lenient().when(purchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        product = new ProductCatalogue("PROD-004", "Basic SMS Bundle", BigDecimal.valueOf(99), "30", "PREPAID");
        lenient().when(eligibilityClient.checkEligibility(any(), any())).thenReturn(EligibilityResult.eligible());
    }

    private PurchaseRequest buildRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setCustomerReference("CUST-10291");
        request.setProductCode("PROD-004");
        request.setPaymentMethod("CARD");
        request.setChannel("APP");
        request.setMsisdn("0821234567");
        return request;
    }

    private PurchaseRequest buildRequest(String msisdn) {
        PurchaseRequest request = buildRequest();
        request.setMsisdn(msisdn);
        return request;
    }

    private PaymentResult successfulPayment(String txnId) {
        return new PaymentResult(PaymentStatus.SUCCESS, txnId);
    }

    private PaymentResult failedPayment() {
        return new PaymentResult(PaymentStatus.FAILED, null);
    }

    private ProvisioningResult provisioningResult(ProvisioningStatus status, String reference) {
        return new ProvisioningResult(status, reference);
    }

    private ProvisioningStatusResult statusResult(ProvisioningStatus status) {
        return new ProvisioningStatusResult(status, null);
    }

    //Msisdn Validation

    @Test
    void shouldAcceptTenDigitMsisdn() {
        when(purchaseRepository.findByIdempotencyKey("idem-msisdn-1")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-M1"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-M1"));

        PurchaseResponse response = service.purchase(buildRequest("0821234567"), "idem-msisdn-1");

        assertThat(response.getStatus()).isEqualTo("SUCCESSFUL");
    }


    @Test
    void shouldAcceptElevenDigitMsisdn() {
        when(purchaseRepository.findByIdempotencyKey("idem-msisdn-2")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-M2"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-M2"));

        PurchaseResponse response = service.purchase(buildRequest("27821234567"), "idem-msisdn-2");

        assertThat(response.getStatus()).isEqualTo("SUCCESSFUL");
    }

    @Test
    void shouldAcceptElevenDigitMsisdnWithLeadingPlusNotCountedTowardsDigitTotal() {
        when(purchaseRepository.findByIdempotencyKey("idem-msisdn-3")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-M3"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-M3"));

        PurchaseResponse response = service.purchase(buildRequest("+27821234567"), "idem-msisdn-3");

        assertThat(response.getStatus()).isEqualTo("SUCCESSFUL");
    }

    @Test
    void shouldRejectTwelveDigitMsisdn() {
        assertThatThrownBy(() -> service.purchase(buildRequest("278212345678"), "idem-msisdn-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("msisdn must be exactly 10 or 11 digits");
    }

    @Test
    void shouldRejectNullMsisdn() {
        assertThatThrownBy(() -> service.purchase(buildRequest(null), "idem-msisdn-5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("msisdn is required");
    }

    @Test
    void shouldRejectBlankMsisdn() {
        assertThatThrownBy(() -> service.purchase(buildRequest("   "), "idem-msisdn-6"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("msisdn is required");
    }

    @Test
    void shouldRejectMsisdnWithNonDigitCharacters() {
        assertThatThrownBy(() -> service.purchase(buildRequest("082-123-4567"), "idem-msisdn-7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("msisdn must be exactly 10 or 11 digits");
    }

        // ---- idempotency key validation ----

    @Test
    void shouldRejectNullIdempotencyKey() {
        assertThatThrownBy(() -> service.purchase(buildRequest(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency Key cannot be null or blank");
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThatThrownBy(() -> service.purchase(buildRequest(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- idempotency dedup ----

    @Test
    void shouldReturnExistingPurchaseOnDuplicateIdempotencyKeyWithoutRecharging() {
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-1"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-1"));
        when(purchaseRepository.findByIdempotencyKey("idem-dup-001")).thenReturn(Optional.empty());

        PurchaseResponse first = service.purchase(buildRequest(), "idem-dup-001");

        // Capture the entity actually persisted, then feed it back for the second call
        // to simulate the repository now genuinely holding that record.
        ArgumentCaptor<PurchaseEntity> captor = ArgumentCaptor.forClass(PurchaseEntity.class);
        verify(purchaseRepository, atLeastOnce()).save(captor.capture());
        PurchaseEntity savedEntity = captor.getValue();
        when(purchaseRepository.findByIdempotencyKey("idem-dup-001")).thenReturn(Optional.of(savedEntity));

        PurchaseResponse second = service.purchase(buildRequest(), "idem-dup-001");

        assertThat(second.getPurchaseId()).isEqualTo(first.getPurchaseId());
        verify(paymentClient, times(1)).charge(any()); // charged exactly once across both calls
    }

    // ---- product validation ----

    @Test
    void shouldFailValidationAndMarkFailedWhenProductNotFound() {
        when(purchaseRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenThrow(new ProductNotFoundException("PROD-004"));

        assertThatThrownBy(() -> service.purchase(buildRequest(), "idem-1"))
                .isInstanceOf(ProductNotFoundException.class);

        verify(paymentClient, never()).charge(any());

        ArgumentCaptor<PurchaseEntity> captor = ArgumentCaptor.forClass(PurchaseEntity.class);
        verify(purchaseRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getPurchaseStatus()).isEqualTo("FAILED");
    }

    // ---- payment failure ----

    @Test
    void shouldNotProvisionWhenPaymentFails() {
        when(purchaseRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(failedPayment());

        service.purchase(buildRequest(), "idem-2");

        verify(provisioningClient, never()).allocate(any());
    }

    @Test
    void bugCheck_purchaseStatusStaysProcessingAfterPaymentFailure() {
        // KNOWN BUG: the payment-failure branch only calls setPaymentStatus(...), never
        // setPurchaseStatus(FAILED). This test documents that current (likely unintended)
        // behaviour: the overall purchase status is left at "PROCESSING", not "FAILED".
        when(purchaseRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(failedPayment());

        PurchaseResponse response = service.purchase(buildRequest(), "idem-3");

        assertThat(response.getStatus()).isEqualTo("PROCESSING"); // NOT "FAILED", despite payment having failed
    }

    // ---- provisioning success (happy path) ----

    @Test
    void shouldSucceedWhenPaymentAndProvisioningSucceed() {
        when(purchaseRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-4"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-4"));

        PurchaseResponse response = service.purchase(buildRequest(), "idem-4");

        assertThat(response.getStatus()).isEqualTo("SUCCESSFUL");
        assertThat(response.getProductCode()).isEqualTo("PROD-004");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99));
        assertThat(response.getCurrency()).isEqualTo("ZAR");
        verify(paymentClient, never()).reverse(any());
    }

    // ---- provisioning genuine failure -> reversal ----

    @Test
    void shouldReverseSuccessfulPaymentWhenProvisioningFails() {
        when(purchaseRepository.findByIdempotencyKey("idem-5")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-5"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.FAILED, "PROV-5"));

        service.purchase(buildRequest(), "idem-5");

        verify(paymentClient).reverse(argThat(r -> r.getTransactionId().equals("TXN-5")));
    }

    @Test
    void bugCheck_purchaseStatusStaysProcessingAfterProvisioningFailureAndReversal() {
        // KNOWN BUG: the genuine-provisioning-failure branch reverses payment and calls
        // setPaymentStatus(...), but never calls setPurchaseStatus(FAILED) either.
        // Overall status is left at "PROCESSING" even though the purchase was rolled back.
        when(purchaseRepository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-6"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.FAILED, "PROV-6"));

        PurchaseResponse response = service.purchase(buildRequest(), "idem-6");

        assertThat(response.getStatus()).isEqualTo("PROCESSING"); // NOT "FAILED", despite the reversal
    }

    // ---- provisioning retry resolution ----

    @Test
    void shouldResolveProvisioningUnknownViaRetryAndSucceed() {
        when(purchaseRepository.findByIdempotencyKey("idem-7")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-7"));
        when(provisioningClient.allocate(any()))
                .thenReturn(provisioningResult(ProvisioningStatus.PROVISIONING_UNKNOWN, "PROV-7"));
        when(provisioningClient.findStatus("PROV-7"))
                .thenReturn(statusResult(ProvisioningStatus.PROVISIONING_UNKNOWN))
                .thenReturn(statusResult(ProvisioningStatus.SUCCESS));

        PurchaseResponse response = service.purchase(buildRequest(), "idem-7");

        assertThat(response.getStatus()).isEqualTo("SUCCESSFUL");
        verify(provisioningClient, times(2)).findStatus("PROV-7");
        verify(paymentClient, never()).reverse(any());
    }

    @Test
    void shouldStayProcessingWhenProvisioningRemainsUnknownAfterMaxRetries() {
        when(purchaseRepository.findByIdempotencyKey("idem-8")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-8"));
        when(provisioningClient.allocate(any()))
                .thenReturn(provisioningResult(ProvisioningStatus.PROVISIONING_UNKNOWN, "PROV-8"));
        when(provisioningClient.findStatus("PROV-8"))
                .thenReturn(statusResult(ProvisioningStatus.PROVISIONING_UNKNOWN));

        PurchaseResponse response = service.purchase(buildRequest(), "idem-8");

        assertThat(response.getStatus()).isEqualTo("PROCESSING"); // intentional design, not a bug here
        verify(provisioningClient, times(3)).findStatus("PROV-8"); // MAX_PROVISIONING_STATUS_RETRIES
        verify(paymentClient, never()).reverse(any());
        verify(paymentClient, times(1)).charge(any()); // never re-charged during retries
    }

    // ---- purchase ID format ----

    @Test
    void shouldGeneratePurchaseIdInExpectedFormat() {
        when(purchaseRepository.findByIdempotencyKey("idem-9")).thenReturn(Optional.empty());
        when(productCatalogueClient.findProduct("PROD-004")).thenReturn(product);
        when(paymentClient.charge(any())).thenReturn(successfulPayment("TXN-9"));
        when(provisioningClient.allocate(any())).thenReturn(provisioningResult(ProvisioningStatus.SUCCESS, "PROV-9"));

        PurchaseResponse response = service.purchase(buildRequest(), "idem-9");

        assertThat(response.getPurchaseId()).matches("^PUR-\\d{8}-\\d{5}$");
    }

    // ---- status lookup ----

    @Test
    void shouldRetrievePurchaseStatusByPurchaseId() {
        PurchaseEntity entity = new PurchaseEntity(
                "PUR-20260827-00001", "idem-x", "CUST-1", "PROD-004",
                "CARD", "APP", "+27821234567", "ZAR");
        entity.setPurchaseStatus("SUCCESSFUL");
        entity.setPaymentStatus("SUCCESS");
        entity.setProvisioningStatus("SUCCESS");
        entity.setProvisioningReference("PROV-10");

        when(purchaseRepository.findById("PUR-20260827-00001")).thenReturn(Optional.of(entity));

        PurchaseStatusResponse status = service.getPurchaseStatus("PUR-20260827-00001");

        assertThat(status.getPurchaseStatus()).isEqualTo("SUCCESSFUL");
        assertThat(status.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(status.getProvisioningStatus()).isEqualTo("SUCCESS");
        assertThat(status.getReference()).isEqualTo("PROV-10");
        assertThat(status.getTimestamp()).isNotNull();
    }

    @Test
    void shouldThrowWhenPurchaseIdDoesNotExist() {
        when(purchaseRepository.findById("PUR-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPurchaseStatus("PUR-missing"))
                .isInstanceOf(PurchaseNotFoundException.class);
    }
}