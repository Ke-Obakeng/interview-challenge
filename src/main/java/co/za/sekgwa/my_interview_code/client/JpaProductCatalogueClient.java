package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.entity.ProductCatalogueDB;
import co.za.sekgwa.my_interview_code.exception.ResourceNotFoundException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JpaProductCatalogueClient implements ProductCatalogueClient {

    private final ProductRepository productRepository;

    public JpaProductCatalogueClient(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductCatalogue findProduct(String productCode) {
        ProductCatalogueDB db = productRepository.findById(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return new ProductCatalogue(
                db.getProductCode(),
                db.getBundleName(),
                db.getPrice(),
                db.getValidity(),
                db.getType()
        );
    }

    @Override
    public List<ProductCatalogue> findAllProducts() {
        return productRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ProductCatalogue> findProducts(String type, BigDecimal maxPrice) {
        List<ProductCatalogueDB> dba;

        boolean hasType = type != null && !type.isBlank();
        boolean hasMaxPrice = maxPrice != null;

        if(hasType && hasMaxPrice) {
            dba = productRepository.findByTypeAndPriceLessThanEqual(type, maxPrice);
        }else if(hasType){
            dba = productRepository.findByType(type);
        }else if(hasMaxPrice){
            dba = productRepository.findByPriceLessThanEqual(maxPrice);
        }else {
            dba = productRepository.findAll();
        }

        return dba.stream().map(this::toDomain).toList();
    }


    private ProductCatalogue toDomain(ProductCatalogueDB db) {
        return new ProductCatalogue(
                db.getProductCode(),
                db.getBundleName(),
                db.getPrice(),
                db.getValidity(),
                db.getType()
        );
    }
}
