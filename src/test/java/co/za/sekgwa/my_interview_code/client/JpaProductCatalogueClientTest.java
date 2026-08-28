package co.za.sekgwa.my_interview_code.client;

import co.za.sekgwa.my_interview_code.entity.ProductCatalogueDB;
import co.za.sekgwa.my_interview_code.exception.ResourceNotFoundException;
import co.za.sekgwa.my_interview_code.model.ProductCatalogue;
import co.za.sekgwa.my_interview_code.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaProductCatalogueClientTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private JpaProductCatalogueClient client;

    private ProductCatalogueDB sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = mock(ProductCatalogueDB.class);
        lenient().when(sampleEntity.getProductCode()).thenReturn("P001");
        lenient().when(sampleEntity.getBundleName()).thenReturn("Data Bundle 1GB");
        lenient().when(sampleEntity.getPrice()).thenReturn(new BigDecimal("100.00"));
        lenient().when(sampleEntity.getValidity()).thenReturn("30 Days");
        lenient().when(sampleEntity.getType()).thenReturn("DATA");
    }

    // --- findProduct Tests ---

    @Test
    void findProduct_WhenProductExists_ShouldReturnMappedProductCatalogue() {
        when(productRepository.findById("P001")).thenReturn(Optional.of(sampleEntity));

        ProductCatalogue result = client.findProduct("P001");

        assertThat(result).isNotNull();
        assertThat(result.getProductCode()).isEqualTo("P001");
        assertThat(result.getBundleName()).isEqualTo("Data Bundle 1GB");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getValidity()).isEqualTo("30 Days");
        assertThat(result.getType()).isEqualTo("DATA");
        verify(productRepository).findById("P001");
    }

    @Test
    void findProduct_WhenProductDoesNotExist_ShouldThrowResourceNotFoundException() {
        when(productRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> client.findProduct("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(productRepository).findById("INVALID");
    }

    // --- findAllProducts Tests ---

    @Test
    void findAllProducts_ShouldReturnListOfMappedProductCatalogues() {
        when(productRepository.findAll()).thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductCode()).isEqualTo("P001");
        verify(productRepository).findAll();
    }

    // --- findProducts Branch Coverage Tests ---

    @Test
    void findProducts_WhenBothTypeAndMaxPriceProvided_ShouldCallFindByTypeAndPriceLessThanEqual() {
        BigDecimal maxPrice = new BigDecimal("150.00");
        when(productRepository.findByTypeAndPriceLessThanEqual("DATA", maxPrice))
                .thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findProducts("DATA", maxPrice);

        assertThat(result).hasSize(1);
        verify(productRepository).findByTypeAndPriceLessThanEqual("DATA", maxPrice);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void findProducts_WhenOnlyTypeProvided_ShouldCallFindByType() {
        when(productRepository.findByType("DATA")).thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findProducts("DATA", null);

        assertThat(result).hasSize(1);
        verify(productRepository).findByType("DATA");
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void findProducts_WhenOnlyMaxPriceProvided_ShouldCallFindByPriceLessThanEqual() {
        BigDecimal maxPrice = new BigDecimal("150.00");
        when(productRepository.findByPriceLessThanEqual(maxPrice))
                .thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findProducts(null, maxPrice);

        assertThat(result).hasSize(1);
        verify(productRepository).findByPriceLessThanEqual(maxPrice);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void findProducts_WhenTypeIsBlankAndPriceIsNull_ShouldCallFindAll() {
        when(productRepository.findAll()).thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findProducts("   ", null);

        assertThat(result).hasSize(1);
        verify(productRepository).findAll();
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void findProducts_WhenBothTypeAndMaxPriceAreNull_ShouldCallFindAll() {
        when(productRepository.findAll()).thenReturn(List.of(sampleEntity));

        List<ProductCatalogue> result = client.findProducts(null, null);

        assertThat(result).hasSize(1);
        verify(productRepository).findAll();
        verifyNoMoreInteractions(productRepository);
    }
}