package co.za.sekgwa.my_interview_code.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tbl_purchase", uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
@Getter
@Setter
public class PurchaseEntity {

    @Id
    private String purchaseId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private String customerReference;
    private String productCode;
    private String paymentMethod;
    private String channel;
    private String msisdn;

    private BigDecimal amount;
    private String currency;

    private String purchaseStatus;
    private String paymentStatus;
    private String provisioningStatus;
    private String provisioningReference;

    private Instant createdAt;
    private Instant updatedAt;

    protected PurchaseEntity() {}

    public PurchaseEntity(String purchaseId, String idempotencyKey,
                          String customerReference, String productCode,
                          String paymentMethod, String channel,
                          String msisdn, String currency) {
        this.purchaseId = purchaseId;
        this.idempotencyKey = idempotencyKey;
        this.customerReference = customerReference;
        this.productCode = productCode;
        this.paymentMethod = paymentMethod;
        this.channel = channel;
        this.msisdn = msisdn;
        this.currency = currency;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private void touch() { this.updatedAt = Instant.now(); }
    public void setAmount(BigDecimal amount) { this.amount = amount; touch(); }
}
