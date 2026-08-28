package co.za.sekgwa.my_interview_code.service;

import co.za.sekgwa.my_interview_code.client.ProductCatalogueClient;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogueServiceImplTest {

    @Mock
    private ProductCatalogueClient productCatalogueClient;

    private ProductCatalogueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogueServiceImpl(productCatalogueClient);
    }

    // ---- findProduct: pure delegation, no business logic yet ----

    @Test
    void findProductShouldDelegateDirectlyToClient() {
        ProductCatalogue expected = mock(ProductCatalogue.class);
        when(productCatalogueClient.findProduct("PROD-001")).thenReturn(expected);

        ProductCatalogue result = service.findProduct("PROD-001");

        assertThat(result).isSameAs(expected);
        verify(productCatalogueClient).findProduct("PROD-001");
    }

    @Test
    void findProductShouldPassThroughWhateverCodeItIsGivenWithoutNormalization() {
        // Unlike findProductsWithFilter's "type", findProduct performs no trim/uppercase -
        // this test locks in that current (lack of) behaviour.
        ProductCatalogue expected = mock(ProductCatalogue.class);
        when(productCatalogueClient.findProduct("  prod-001  ")).thenReturn(expected);

        service.findProduct("  prod-001  ");

        verify(productCatalogueClient).findProduct("  prod-001  ");
    }

    @Test
    void findProductShouldPropagateNullReturnFromClient() {
        when(productCatalogueClient.findProduct("PROD-MISSING")).thenReturn(null);

        ProductCatalogue result = service.findProduct("PROD-MISSING");

        assertThat(result).isNull();
    }

    // ---- findAllProducts: pure delegation ----

    @Test
    void findAllProductsShouldDelegateDirectlyToClient() {
        List<ProductCatalogue> expected = List.of(mock(ProductCatalogue.class), mock(ProductCatalogue.class));
        when(productCatalogueClient.findAllProducts()).thenReturn(expected);

        List<ProductCatalogue> result = service.findAllProducts();

        assertThat(result).isSameAs(expected);
        verify(productCatalogueClient).findAllProducts();
    }

    @Test
    void findAllProductsShouldReturnEmptyListWhenClientReturnsEmpty() {
        when(productCatalogueClient.findAllProducts()).thenReturn(List.of());

        List<ProductCatalogue> result = service.findAllProducts();

        assertThat(result).isEmpty();
    }

    // ---- findProductsWithFilter: maxPrice validation ----

    @Test
    void shouldThrowWhenMaxPriceIsNegative() {
        assertThatThrownBy(() -> service.findProductsWithFilter("PREPAID", BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPrice cannot be negative");

        verifyNoInteractions(productCatalogueClient);
    }

    @Test
    void shouldAllowMaxPriceOfExactlyZero() {
        // compareTo(BigDecimal.ZERO) < 0 excludes only strictly-negative values, zero is allowed through
        when(productCatalogueClient.findProducts(eq("PREPAID"), eq(BigDecimal.ZERO))).thenReturn(List.of());

        service.findProductsWithFilter("PREPAID", BigDecimal.ZERO);

        verify(productCatalogueClient).findProducts("PREPAID", BigDecimal.ZERO);
    }

    @Test
    void shouldAllowNullMaxPrice() {
        when(productCatalogueClient.findProducts(eq("PREPAID"), isNull())).thenReturn(List.of());

        service.findProductsWithFilter("PREPAID", null);

        verify(productCatalogueClient).findProducts(eq("PREPAID"), isNull());
    }

    // ---- findProductsWithFilter: type normalization ----

    @Test
    void shouldTrimAndUppercaseType() {
        when(productCatalogueClient.findProducts(eq("PREPAID"), any())).thenReturn(List.of());

        service.findProductsWithFilter("  prepaid  ", BigDecimal.valueOf(100));

        verify(productCatalogueClient).findProducts("PREPAID", BigDecimal.valueOf(100));
    }

    @Test
    void shouldTreatBlankTypeAsNoFilter() {
        when(productCatalogueClient.findProducts(isNull(), any())).thenReturn(List.of());

        service.findProductsWithFilter("   ", BigDecimal.valueOf(100));

        verify(productCatalogueClient).findProducts(isNull(), eq(BigDecimal.valueOf(100)));
    }

    @Test
    void shouldTreatEmptyStringTypeAsNoFilter() {
        when(productCatalogueClient.findProducts(isNull(), any())).thenReturn(List.of());

        service.findProductsWithFilter("", BigDecimal.valueOf(100));

        verify(productCatalogueClient).findProducts(isNull(), eq(BigDecimal.valueOf(100)));
    }

    @Test
    void shouldPassNullTypeThroughAsNull() {
        when(productCatalogueClient.findProducts(isNull(), any())).thenReturn(List.of());

        service.findProductsWithFilter(null, BigDecimal.valueOf(100));

        verify(productCatalogueClient).findProducts(isNull(), eq(BigDecimal.valueOf(100)));
    }

    @Test
    void shouldReturnWhateverClientReturnsForValidFilter() {
        List<ProductCatalogue> expected = List.of(mock(ProductCatalogue.class));
        when(productCatalogueClient.findProducts("BUNDLE", BigDecimal.valueOf(150))).thenReturn(expected);

        List<ProductCatalogue> result = service.findProductsWithFilter("bundle", BigDecimal.valueOf(150));

        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldNotMutateTypeCasingWhenAlreadyUppercaseWithNoWhitespace() {
        when(productCatalogueClient.findProducts(eq("POSTPAID"), any())).thenReturn(List.of());

        service.findProductsWithFilter("POSTPAID", BigDecimal.valueOf(500));

        verify(productCatalogueClient).findProducts("POSTPAID", BigDecimal.valueOf(500));
    }
}