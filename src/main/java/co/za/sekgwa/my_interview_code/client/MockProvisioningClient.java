package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.ProvisioningStatus;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningRequest;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningResult;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningStatusResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockProvisioningClient implements ProvisioningClient{

    private final Map<String, Integer> statusCheckAttempts = new ConcurrentHashMap<>();

    @Override
    public ProvisioningResult allocate(ProvisioningRequest provisioningRequest) {
        String reference = "PROVI-"+ randomNumberGen();
        statusCheckAttempts.put(reference, 0);
        return new ProvisioningResult(ProvisioningStatus.PROVISIONING_UNKNOWN, reference);
    }

    @Override
    public ProvisioningStatusResult findStatus(String provisioningReference) {
        int attempts = statusCheckAttempts.merge(provisioningReference, 1, Integer::sum);
        ProvisioningStatus status = attempts >=1 ? ProvisioningStatus.SUCCESS : ProvisioningStatus.PROVISIONING_UNKNOWN;

        return new ProvisioningStatusResult(status, provisioningReference);
    }

    private String randomNumberGen() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return String.format("%05d", randomNumber);
    }
}
