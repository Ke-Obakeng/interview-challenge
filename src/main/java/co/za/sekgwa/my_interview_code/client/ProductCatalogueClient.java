package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.model.ProductCatalogue;

import java.math.BigDecimal;
import java.util.List;

public interface ProductCatalogueClient {
    ProductCatalogue findProduct(String productCode);
    List<ProductCatalogue> findAllProducts();
    List<ProductCatalogue> findProducts(String type, BigDecimal maxPrice);
}
