package co.za.sekgwa.my_interview_code.model.eligibility;

public class EligibilityResult {

    private final boolean eligible;
    private final String reason;

    public EligibilityResult(boolean eligible, String reason) {
        this.eligible = eligible;
        this.reason = reason;
    }

    public static EligibilityResult eligible() {
        return new EligibilityResult(true, null);
    }

    public static EligibilityResult notEligible(String reason) {
        return new EligibilityResult(false, reason);
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getReason() {
        return reason;
    }
}