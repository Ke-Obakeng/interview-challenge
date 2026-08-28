package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningRequest;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningResult;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningStatusResult;

public interface ProvisioningClient {
    ProvisioningResult allocate(ProvisioningRequest provisioningRequest);
    ProvisioningStatusResult findStatus(String provisioningReference);
}

