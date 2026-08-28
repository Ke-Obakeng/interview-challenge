package co.za.sekgwa.my_interview_code.model.payment;

public class ReversalResult {

    private final boolean successful;

    public ReversalResult(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {return successful;}

}
