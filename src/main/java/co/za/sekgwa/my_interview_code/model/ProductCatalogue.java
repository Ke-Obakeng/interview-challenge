package co.za.sekgwa.my_interview_code.model;


import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;

@Getter()
@Setter()
@NoArgsConstructor()
public class ProductCatalogue {

    private String productCode;
    private String bundleName;
    private BigDecimal price;
    private String validity;
    private String type;

    public ProductCatalogue(String productCode, String bundleName, BigDecimal price, String validity, String type) {
        this.productCode = productCode;
        this.bundleName = bundleName;
        this.price = price;
        this.validity = validity;
        this.type = type;
    }
}
