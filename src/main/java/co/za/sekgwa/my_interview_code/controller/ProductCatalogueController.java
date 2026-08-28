package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.exception.ResourceNotFoundException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.repository.ProductRepository;
import co.za.sekgwa.my_interview_code.service.ProductCatalogueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bundles")
public class ProductCatalogueController {

    private final ProductCatalogueService productCatalogueService;

    public ProductCatalogueController(ProductCatalogueService productCatalogueService ) {
        this.productCatalogueService = productCatalogueService;
    }

    @GetMapping("/{productCode}")
    public ResponseEntity<ProductCatalogue> findProduct(@PathVariable String productCode) {
        ProductCatalogue productCatalogue = productCatalogueService.findProduct(productCode);
        return ResponseEntity.ok(productCatalogue);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @GetMapping()
    public ResponseEntity<List<ProductCatalogue>> filterByTypeAndMaxPrice(
            @RequestParam(required=false) String type,
            @RequestParam(required=false) BigDecimal maxPrice
    ) {
        List <ProductCatalogue> pro = productCatalogueService.findProductsWithFilter(type, maxPrice);
        return ResponseEntity.ok(pro);
    }
}
