package co.za.sekgwa.my_interview_code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseRequest {

    private String customerReference;
    private String productCode;
    private String paymentMethod;
    private String channel;
    private String msisdn;

}
