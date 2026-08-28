package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.dto.PurchaseRequest;
import co.za.sekgwa.my_interview_code.dto.PurchaseResponse;
import co.za.sekgwa.my_interview_code.dto.PurchaseStatusResponse;
import co.za.sekgwa.my_interview_code.model.PurchaseStatus;

public interface PurchaseService {
    PurchaseResponse purchase(PurchaseRequest purchaseRequest, String idempotencyKey);
    PurchaseStatusResponse getPurchaseStatus(String purchaseId);
}
