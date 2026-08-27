package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.client.ProductCatalogueClient;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductCatalogueServiceImpl implements ProductCatalogueService {

    private final ProductCatalogueClient productCatalogueClient;

    public ProductCatalogueServiceImpl(ProductCatalogueClient productCatalogueClient) {
        this.productCatalogueClient = productCatalogueClient;
    }

    //Do more business logic
    @Override
    public ProductCatalogue findProduct(String productCode) {

        return productCatalogueClient.findProduct(productCode);
    }

    @Override
    public List<ProductCatalogue> findAllProducts(){

        return productCatalogueClient.findAllProducts();
    }

    @Override
    public List<ProductCatalogue> findProductsWithFilter(String type, BigDecimal maxPrice) {
        if(maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("maxPrice cannot be negative");
        }

        if(type != null && type.isBlank()){
            type = null; //Just treat it as same as having no filter
        }else if(type != null){
            type = type.trim().toUpperCase(); // Simon might ask if you don't uppercase it, why it is uppercase on h2
        }

    return productCatalogueClient.findProducts(type, maxPrice);
    }
}
