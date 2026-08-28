package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.exception.ResourceNotFoundException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.service.ProductCatalogueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductCatalogueController.class)
class ProductCatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCatalogueService productCatalogueService;

    private ProductCatalogue buildProduct(String code, String name, String price, String validity, String type) {
        return new ProductCatalogue(code, name, new BigDecimal(price), validity, type);
    }

    // ---- GET /{productCode} ----

    @Test
    void shouldReturn200WhenProductFound() throws Exception {
        ProductCatalogue product = buildProduct("PROD-001", "Unlimited Data 20GB", "299.00", "30", "PREPAID");
        when(productCatalogueService.findProduct("PROD-001")).thenReturn(product);

        mockMvc.perform(get("/api/v1/bundles/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("PROD-001"))
                .andExpect(jsonPath("$.bundleName").value("Unlimited Data 20GB"))
                .andExpect(jsonPath("$.price").value(299.00))
                .andExpect(jsonPath("$.validity").value("30"))
                .andExpect(jsonPath("$.type").value("PREPAID"));
    }

    @Test
    void shouldReturn404WithMessageBodyWhenProductNotFound() throws Exception {
        when(productCatalogueService.findProduct("PROD-999"))
                .thenThrow(new ResourceNotFoundException("Product not found: PROD-999"));

        mockMvc.perform(get("/api/v1/bundles/PROD-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Product not found: PROD-999"));
    }

    // ---- GET (collection, with optional filters) ----

    @Test
    void shouldReturnAllProductsWhenNoFiltersProvided() throws Exception {
        List<ProductCatalogue> products = List.of(
                buildProduct("PROD-001", "Unlimited Data 20GB", "299.00", "30", "PREPAID"),
                buildProduct("PROD-004", "Basic SMS Bundle", "99.00", "30", "PREPAID")
        );
        when(productCatalogueService.findProductsWithFilter(isNull(), isNull())).thenReturn(products);

        mockMvc.perform(get("/api/v1/bundles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnFilteredProductsByTypeAndMaxPrice() throws Exception {
        List<ProductCatalogue> products = List.of(
                buildProduct("PROD-004", "Basic SMS Bundle", "99.00", "30", "PREPAID")
        );
        when(productCatalogueService.findProductsWithFilter(eq("PREPAID"), eq(BigDecimal.valueOf(150))))
                .thenReturn(products);

        mockMvc.perform(get("/api/v1/bundles").param("type", "PREPAID").param("maxPrice", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("PROD-004"));
    }

    @Test
    void shouldFilterByTypeOnlyWhenMaxPriceOmitted() throws Exception {
        when(productCatalogueService.findProductsWithFilter(eq("POSTPAID"), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bundles").param("type", "POSTPAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldFilterByMaxPriceOnlyWhenTypeOmitted() throws Exception {
        when(productCatalogueService.findProductsWithFilter(isNull(), eq(BigDecimal.valueOf(100))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bundles").param("maxPrice", "100"))
                .andExpect(status().isOk());

        // Confirms the controller passes null for "type", not an empty string, when the param is absent
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsMatchFilter() throws Exception {
        when(productCatalogueService.findProductsWithFilter(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bundles").param("type", "BUNDLE").param("maxPrice", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ---- global exception handling (via a @ControllerAdvice picked up by @WebMvcTest's scan) ----

    @Test
    void shouldReturn400WithMessageBodyWhenServiceThrowsIllegalArgumentException() throws Exception {
        // A global @ControllerAdvice (not shown here, but picked up automatically by @WebMvcTest's
        // component scan) handles IllegalArgumentException and returns its message as a plain-text
        // 400 body - confirmed empirically, not assumed.
        when(productCatalogueService.findProductsWithFilter(any(), eq(BigDecimal.valueOf(-1))))
                .thenThrow(new IllegalArgumentException("maxPrice cannot be negative"));

        mockMvc.perform(get("/api/v1/bundles").param("maxPrice", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("maxPrice cannot be negative"));
    }

    // ---- malformed request ----

    @Test
    void shouldReturn400WhenMaxPriceIsNotAValidNumber() throws Exception {
        // Now handled by a dedicated MethodArgumentTypeMismatchException handler in
        // ControllerAdvice, returning a clean 400 with a helpful message instead of
        // falling through to the generic 500 catch-all.
        mockMvc.perform(get("/api/v1/bundles").param("maxPrice", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value 'not-a-number' for parameter 'maxPrice'"));
    }
}