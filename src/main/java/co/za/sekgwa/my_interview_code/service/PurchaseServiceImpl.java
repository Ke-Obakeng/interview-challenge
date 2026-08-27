package co.za.sekgwa.my_interview_code.service;

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
import co.za.sekgwa.my_interview_code.model.PurchaseStatus;
import co.za.sekgwa.my_interview_code.model.payment.PaymentRequest;
import co.za.sekgwa.my_interview_code.model.payment.PaymentResult;
import co.za.sekgwa.my_interview_code.model.payment.PaymentStatus;
import co.za.sekgwa.my_interview_code.model.payment.ReversalRequest;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningRequest;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningResult;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningStatusResult;
import co.za.sekgwa.my_interview_code.repository.PurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PurchaseServiceImpl implements PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);
    private static final int MAX_PROVISIONING_STATUS_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 200;

    private final ProductCatalogueClient productCatalogueClient;
    private final PaymentClient paymentClient;
    private final ProvisioningClient provisioningClient;
    private final PurchaseRepository purchaseRepository;

    public PurchaseServiceImpl(ProductCatalogueClient productCatalogueClient,
                               PaymentClient paymentClient,
                               ProvisioningClient provisioningClient,
                               PurchaseRepository purchaseRepository) {
        this.productCatalogueClient = productCatalogueClient;
        this.paymentClient = paymentClient;
        this.provisioningClient = provisioningClient;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public PurchaseResponse purchase(PurchaseRequest purchaseRequest, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency Key cannot be null or blank");
        }

        //Idempotency check - If it has the same key, we return the same and don't reprocess
        var existing = purchaseRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            log.info("Duplicate request for the idempotency key {} - returning existing purchase {}", idempotencyKey, existing.get().getPurchaseId());
            return toPurchaseResponse(existing.get());
        }
        double numOrd = (Math.random() * 10000) + 00000;
        String purchaseId = "PUR-" + getTodaysDate(LocalDate.now()) + numOrd;

        PurchaseEntity purchase = new PurchaseEntity(
                purchaseId, idempotencyKey, purchaseRequest.getCustomerReference(),
                purchaseRequest.getProductCode(), purchaseRequest.getPaymentMethod(),
                purchaseRequest.getChannel(), purchaseRequest.getMsisdn(), "ZAR");

        purchase.setPurchaseStatus(PurchaseStatus.RECEIVED.name());
        purchase.setPaymentStatus(PaymentStatus.PENDING.name());
        purchase.setProvisioningStatus(ProvisioningStatus.NOT_STARTED.name());
        purchaseRepository.save(purchase);

        purchase.setPurchaseStatus(PurchaseStatus.VALIDATING.name());
        purchaseRepository.save(purchase);

        ProductCatalogue productCatalogue;

        try {
            productCatalogue = productCatalogueClient.findProduct(purchase.getProductCode());
        } catch (ProductNotFoundException e) {
            purchase.setPurchaseStatus(PurchaseStatus.FAILED.name());
            purchaseRepository.save(purchase);
            throw e;
        }

        purchase.setAmount(productCatalogue.getPrice());
        purchaseRepository.save(purchase);

        //charge payment
        purchase.setPurchaseStatus(PurchaseStatus.PROCESSING.name());
        purchaseRepository.save(purchase);

        PaymentRequest paymentRequest = new PaymentRequest(
                purchaseRequest.getCustomerReference(),
                purchaseRequest.getPaymentMethod(),
                productCatalogue.getPrice(), "ZAR");

        PaymentResult paymentResult = paymentClient.charge(paymentRequest);

        purchase.setPaymentStatus(paymentResult.getStatus().name());

        purchaseRepository.save(purchase);

        if (!paymentResult.isSuccessful()) {
            purchase.setPaymentStatus(PurchaseStatus.FAILED.name());
            purchaseRepository.save(purchase);

            return toPurchaseResponse(purchase);
        }

        //Provision when the payment is successful

        ProvisioningRequest provisioningRequest = new ProvisioningRequest(
                purchaseRequest.getCustomerReference(),
                purchaseRequest.getProductCode(),
                purchaseRequest.getMsisdn(),
                purchaseRequest.getChannel(),
                purchaseRequest.getPaymentMethod()
        );

        ProvisioningResult provisioningResult = provisioningClient.allocate((provisioningRequest));
        ProvisioningStatus provisioningStatus = provisioningResult.getStatus();
        String provisioningReference = provisioningResult.getProvisioningReference();

        //Retry - Resolve the provisioning unknown status by rechecking the status: NB... Payment is not rrecharged

        int attempts = 0;

        while(provisioningStatus == ProvisioningStatus.PROVISIONING_UNKNOWN && attempts < MAX_PROVISIONING_STATUS_RETRIES) {
            attempts++;
            sleepQuietly(RETRY_BACKOFF_MS * attempts);

            ProvisioningStatusResult statusResult = provisioningClient.findStatus(provisioningReference);
            provisioningStatus = statusResult.getProvisioningStatus();
            log.info("Provisioning status retry {} for {} : {}", attempts, purchaseId,  provisioningReference);
        }

        purchase.setProvisioningStatus(provisioningStatus.name());
        purchase.setProvisioningReference(provisioningReference);

        if(provisioningStatus == ProvisioningStatus.SUCCESS) {
            purchase.setPurchaseStatus(PurchaseStatus.SUCCESSFUL.name());
        }else if(provisioningStatus == ProvisioningStatus.PROVISIONING_UNKNOWN) {
            //still unresolved after the retries, payment success but provioning not confirmed
            //let status be in processing
            purchase.setPurchaseStatus(PurchaseStatus.PROCESSING.name());
        }else {
            paymentClient.reverse(new ReversalRequest(paymentResult.getTransactionId()));
            purchase.setPaymentStatus(PurchaseStatus.FAILED.name());
        }

        purchaseRepository.save(purchase);
        return toPurchaseResponse(purchase);
    }

    @Override
    public PurchaseStatusResponse getPurchaseStatus(String purchaseId) {
        PurchaseEntity purchase = purchaseRepository.findById(purchaseId).orElseThrow(() -> new PurchaseNotFoundException(purchaseId));

        return new PurchaseStatusResponse(
                purchase.getPurchaseStatus(),
                purchase.getPaymentStatus(),
                purchase.getProvisioningStatus(),
                purchase.getProvisioningReference(),
                purchase.getUpdatedAt()
        );
    }


    private String getTodaysDate(LocalDate date) {
        //LocalDate today = LocalDate.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        //String formattedDate = date.format(formatter);

        return date.format(formatter);

    }

    private PurchaseResponse toPurchaseResponse(PurchaseEntity purchase) {
        return new PurchaseResponse(
                purchase.getPurchaseId(),
                purchase.getPurchaseStatus(),
                purchase.getProductCode(),
                purchase.getAmount(),
                purchase.getCurrency());
    }

    private void sleepQuietly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

}
