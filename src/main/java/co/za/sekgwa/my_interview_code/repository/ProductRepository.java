package co.za.sekgwa.my_interview_code.repository;


import co.za.sekgwa.my_interview_code.entity.ProductCatalogueDB;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductCatalogueDB, String> {
    Optional<ProductCatalogueDB> findByProductCode(String productCode);
    List<ProductCatalogueDB> findByType(String type);
    List<ProductCatalogueDB> findByPriceLessThanEqual(BigDecimal price);
    List<ProductCatalogueDB> findByTypeAndPriceLessThanEqual(String type, BigDecimal maxPrice);
}
