package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.ProvisioningStatus;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningRequest;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningResult;
import co.za.sekgwa.my_interview_code.model.provisioning.ProvisioningStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockProvisioningClientTest {

    private MockProvisioningClient provisioningClient;

    @BeforeEach
    void setUp() {
        provisioningClient = new MockProvisioningClient();
    }

    @Test
    void allocate_ShouldReturnUnknownStatusAndValidReference() {
        ProvisioningRequest request = mock(ProvisioningRequest.class);

        ProvisioningResult result = provisioningClient.allocate(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ProvisioningStatus.PROVISIONING_UNKNOWN);
        assertThat(result.getProvisioningReference()).isNotNull();
        assertThat(result.getProvisioningReference()).startsWith("PROVI-");
    }

    @Test
    void allocate_ShouldGenerateUniqueReferencesOnSubsequentCalls() {
        ProvisioningRequest request = mock(ProvisioningRequest.class);

        ProvisioningResult result1 = provisioningClient.allocate(request);
        ProvisioningResult result2 = provisioningClient.allocate(request);

        assertThat(result1.getProvisioningReference())
                .isNotEqualTo(result2.getProvisioningReference());
    }

    @Test
    void findStatus_WhenCalledForAllocatedReference_ShouldReturnSuccessStatus() {
        ProvisioningRequest request = mock(ProvisioningRequest.class);
        ProvisioningResult allocationResult = provisioningClient.allocate(request);
        String reference = allocationResult.getProvisioningReference();

        ProvisioningStatusResult statusResult = provisioningClient.findStatus(reference);

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.getProvisioningReference()).isEqualTo(reference);
        assertThat(statusResult.getProvisioningStatus()).isEqualTo(ProvisioningStatus.SUCCESS);
    }

    @Test
    void findStatus_WhenCalledForUnknownReference_ShouldIncrementAndReturnSuccessStatus() {
        String unknownReference = "PROVI-UNKNOWN-123";

        ProvisioningStatusResult statusResult = provisioningClient.findStatus(unknownReference);

        assertThat(statusResult).isNotNull();
        assertThat(statusResult.getProvisioningReference()).isEqualTo(unknownReference);
        assertThat(statusResult.getProvisioningStatus()).isEqualTo(ProvisioningStatus.SUCCESS);
    }
}