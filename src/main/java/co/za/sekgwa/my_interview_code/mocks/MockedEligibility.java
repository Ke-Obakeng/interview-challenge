package co.za.sekgwa.my_interview_code.mocks;

import co.za.sekgwa.my_interview_code.client.EligibilityClient;
import co.za.sekgwa.my_interview_code.model.eligibility.EligibilityResult;
import org.springframework.stereotype.Component;

@Component
public class MockedEligibility implements EligibilityClient {

    private static final String FORCE_INELIGIBLE_TRIGGER = "CUST-INELIGIBLE";

    @Override
    public EligibilityResult checkEligibility(String customerReference, String productCode) {
        if (FORCE_INELIGIBLE_TRIGGER.equalsIgnoreCase(customerReference)) {
            return EligibilityResult.notEligible(
                    "User not eligible");
        }
        return EligibilityResult.eligible();
    }
}
