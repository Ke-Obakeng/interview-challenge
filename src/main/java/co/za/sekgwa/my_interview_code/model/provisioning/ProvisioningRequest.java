package co.za.sekgwa.my_interview_code.model.provisioning;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProvisioningRequest {

    private final String customerReference;
    private final String productCode;
    private final String msisdn;
    private final String channel;
    private final String paymentMethod;

    public ProvisioningRequest(String customerReference, String productCode, String msisdn, String channel, String paymentMethod) {
        this.customerReference = customerReference;
        this.productCode = productCode;
        this.msisdn = msisdn;
        this.channel = channel;
        this.paymentMethod = paymentMethod;
    }
}
