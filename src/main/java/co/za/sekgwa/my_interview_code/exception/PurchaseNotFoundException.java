package co.za.sekgwa.my_interview_code.exception;

public class PurchaseNotFoundException extends RuntimeException {

    public PurchaseNotFoundException(String purchaseId) {
        super("Purchase not found" + purchaseId);
    }
}
