package co.za.sekgwa.my_interview_code.model.provisioning;

import co.za.sekgwa.my_interview_code.model.ProvisioningStatus;
import lombok.Getter;

@Getter
public class ProvisioningStatusResult {

    private final ProvisioningStatus provisioningStatus;
    private final String provisioningReference;

    public ProvisioningStatusResult(ProvisioningStatus provisioningStatus, String provisioningReference) {
        this.provisioningStatus = provisioningStatus;
        this.provisioningReference = provisioningReference;
    }
}
