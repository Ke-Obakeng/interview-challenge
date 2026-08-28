package co.za.sekgwa.my_interview_code.controller;

import co.za.sekgwa.my_interview_code.dto.ErrorResponse;
import co.za.sekgwa.my_interview_code.exception.ProductNotFoundException;
import co.za.sekgwa.my_interview_code.exception.PurchaseNotFoundException;
import co.za.sekgwa.my_interview_code.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTE: ErrorResponse is a Lombok @Builder DTO not shown here - this test assumes
 * conventional getters: getTimestamp(), getStatus(), getError(), getMessage(),
 * getPath(), getErrDetails(). Adjust if your actual DTO differs.
 */
@ExtendWith(MockitoExtension.class)
class ControllerAdviceTest {

    private final ControllerAdvice controllerAdvice = new ControllerAdvice();

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestId()).thenReturn(""); // matches real MockMvc/servlet behaviour observed
    }

    @Test
    void shouldReturn404WithStructuredErrorResponseForResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product not found: PROD-999");

        ResponseEntity<ErrorResponse> response = controllerAdvice.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found: PROD-999");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void shouldReturn500WithGenericMessageForUnhandledExceptionTypes() {
        // This is the catch-all that MethodArgumentTypeMismatchException currently falls into,
        // since there's no specific handler registered for it.
        Exception unexpected = new RuntimeException("some internal detail that shouldn't leak to the client");

        ResponseEntity<ErrorResponse> response = controllerAdvice.handleInternalErrorMsg(unexpected, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        // Confirms internal exception details are deliberately not leaked to the client - good practice
        assertThat(response.getBody().getMessage()).doesNotContain("internal detail");
    }

    @Test
    void productNotFoundShouldMapToNotFound() {
        // Previously mapped to 400 (documented as a likely bug); now correctly returns 404,
        // consistent with ResourceNotFoundException and PurchaseNotFoundException.
        ProductNotFoundException ex = new ProductNotFoundException("PROD-999");

        ResponseEntity<String> response = controllerAdvice.handleProductNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WithPlainTextBodyForPurchaseNotFoundException() {
        PurchaseNotFoundException ex = new PurchaseNotFoundException("PUR-999");

        ResponseEntity<String> response = controllerAdvice.handlePurchaseNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PUR-999");
    }

    @Test
    void shouldReturn400WithPlainTextBodyForIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("maxPrice cannot be negative");

        ResponseEntity<String> response = controllerAdvice.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("maxPrice cannot be negative");
    }

    // ---- BUG: "path" field is populated from getRequestId(), not getRequestURI() ----

    @Test
    void bugCheck_pathFieldReflectsRequestIdNotActualUriPath() {
        // KNOWN BUG: build() calls req.getRequestId(), which returns the servlet container's
        // internal per-request tracking identifier - NOT the request's URI. A field literally
        // named "path" should almost certainly be populated from req.getRequestURI() instead.
        // This test proves getRequestURI() is never even called, confirming the mismatch.
        when(request.getRequestId()).thenReturn("internal-tracking-id-123");

        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        ResponseEntity<ErrorResponse> response = controllerAdvice.handleResourceNotFoundException(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("internal-tracking-id-123");
        verify(request, never()).getRequestURI(); // the actual URI is never even read
    }

    @Test
    void errDetailsIsAlwaysNullSinceBuildIsNeverCalledWithAnyDetails() {
        // Both call sites pass `null` for errDetails - the field exists on ErrorResponse but is
        // currently dead: nothing in this class ever populates it with real validation detail.
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");

        ResponseEntity<ErrorResponse> response = controllerAdvice.handleResourceNotFoundException(ex, request);

        assertThat(response.getBody().getErrDetails()).isNull();
    }
}