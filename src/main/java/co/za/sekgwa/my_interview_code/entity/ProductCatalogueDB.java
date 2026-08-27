package co.za.sekgwa.my_interview_code.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity()
@Table(name = "tbl_product_offers")
@Getter()
@Setter()
public class ProductCatalogueDB {

    @Id
    private String productCode;

    private String bundleName;
    private BigDecimal price;
    private String validity;
    private String type;

    protected ProductCatalogueDB() {}

    public ProductCatalogueDB(String productCode, String bundleName, BigDecimal price, String validity, String type) {
        this.productCode = productCode;
        this.bundleName = bundleName;
        this.price = price;
        this.validity = validity;
        this.type = type;
    }
}