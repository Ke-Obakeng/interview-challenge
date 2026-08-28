package co.za.sekgwa.my_interview_code.model.provisioning;

import co.za.sekgwa.my_interview_code.model.ProvisioningStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProvisioningResult {

    private final ProvisioningStatus status;
    private final String provisioningReference;

    public ProvisioningResult(ProvisioningStatus status, String provisioningReference) {
        this.status = status;
        this.provisioningReference = provisioningReference;
    }


}
