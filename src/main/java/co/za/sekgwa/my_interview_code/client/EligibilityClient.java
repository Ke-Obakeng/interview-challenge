package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.eligibility.EligibilityResult;

public interface EligibilityClient {
   EligibilityResult checkEligibility(String customerReference, String productCode);
}
