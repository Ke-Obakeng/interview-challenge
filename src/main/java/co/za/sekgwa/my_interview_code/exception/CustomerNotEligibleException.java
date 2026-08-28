package co.za.sekgwa.my_interview_code.exception;

public class CustomerNotEligibleException extends RuntimeException {

    public CustomerNotEligibleException(String customerReference, String productCode, String reason) {
        super(buildMessage(customerReference, productCode, reason));
    }

    private static String buildMessage(String customerReference, String productCode, String reason) {
        String base = String.format("Customer %s is not eligible to purchase product %s",
                customerReference, productCode);
        return (reason != null && !reason.isBlank()) ? base + ": " + reason : base;
    }
}