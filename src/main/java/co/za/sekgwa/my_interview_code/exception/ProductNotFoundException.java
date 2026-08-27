package co.za.sekgwa.my_interview_code.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String ProductId) {
        super("Product not found" + ProductId);
    }
}
