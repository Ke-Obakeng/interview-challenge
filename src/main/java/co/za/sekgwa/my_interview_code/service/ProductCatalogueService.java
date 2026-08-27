package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.model.ProductCatalogue;

import java.math.BigDecimal;
import java.util.List;

public interface ProductCatalogueService {
    ProductCatalogue findProduct(String productCode);
    List<ProductCatalogue> findAllProducts();
    List<ProductCatalogue> findProductsWithFilter(String type, BigDecimal price);
}
