# my-interview-code

A Spring Boot service demonstrating a "mock-alike" BUY-BUNDLES journey on DC (DigitalChannels).

This would get ProductCatalogue, an AI-assisted product recommendation engine, and an idempotent purchase flow
with provisioning retry

---

## Tech Stack

| Component | Details |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Framework 7) |
| Web | `spring-boot-starter-webmvc` |
| Persistence | Spring Data JPA + H2 (in-memory) |
| API Docs | springdoc-openapi 3.1.0 (Swagger UI) |
| JSON | Jackson 3 (`tools.jackson`, not `com.fasterxml.jackson`) |
| Boilerplate | Lombok 1.18.x |
| Testing | JUnit 5, Mockito, AssertJ, Spring's `@WebMvcTest` / `MockMvc` |

---

## Running the Service

```bash
mvn clean spring-boot:run
```

The app starts on **`http://localhost:8080`**.

### Key local URLs

| Purpose | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON spec | `http://localhost:8080/v3/api-docs` |
| H2 Console | `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:tbl_product_offers`, user `sa`, blank password) |

### Running Tests

```bash
mvn clean test
```

---

## Architecture

The service follows a **Ports and Adapters (Hexagonal)** style:

```
Controller → Service → Client Interface (port) → Concrete Adapter (JPA-backed, mock, or AI-backed)
```

Business/domain logic depends only on interfaces (`ProductCatalogueClient`, `PaymentClient`,
`ProvisioningClient`, `BundleRecommendationProvider`), never on concrete vendor/implementation
classes. This means:
- Real implementations (H2-backed, HTTP-backed) and mock implementations can be swapped without
  touching service or controller code.
- Business logic is unit-testable with Mockito mocks of the interfaces, without a Spring context,
  a real database, or a real HTTP call.

### Package Structure

```
co.za.sekgwa.my_interview_code
├── controller/       REST controllers + global exception handling
├── service/          Business logic, orchestration
├── client/           Interfaces (ports) + concrete adapters (JPA, mock, AI/HTTP)
├── provider/         Recommendation strategy implementations (AI + deterministic fallback)
├── repository/       Spring Data JPA repositories
├── entity/           JPA @Entity classes
├── model/            Domain models (ProductCatalogue, payment/provisioning value objects)
├── dto/               Request/response DTOs
└── exception/        Custom exceptions
```

---

## API Reference

### Product Catalogue — `/api/v1/bundles`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/bundles/{productCode}` | Fetch a single product by its code |
| `GET` | `/api/v1/bundles?type={type}&maxPrice={maxPrice}` | List/filter products; both params optional |

**Filtering behaviour:**
- `type` is trimmed and upper-cased before querying (H2 data is stored upper-case); blank/empty
  string is treated the same as "no filter".

**Sample response — `GET /api/v1/bundles/PROD-001`:**
```json
{
  "productCode": "PROD-001",
  "bundleName": "Unlimited Data 20GB",
  "price": 299.00,
  "validity": "30",
  "type": "PREPAID"
}
```

---

### Recommendations — `/api/v2/bundles`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v2/bundles/bundle-recommendation` | Get up to 3 recommended products for a usage profile |

**Request body:**
```json
{
  "customerReference": "CUST-10291",
  "usageProfile": {
    "averageMonthlyDataMb": 3200,
    "averageMonthlyVoiceMinutes": 40,
    "preferredValidityDays": 30,
    "maximumBudget": 160
  }
}
```

**Response :**
```json
{
  "recommendationId": "REC54213.778...",
  "recommendations": [
    { "productCode": "PROD-004", "confidence": 0.78, "source": "Closest products to the supplied monthly usage" }
  ],
  "source": "AI",
  "promptVersion": "bundle-recommendations-v1"
}
```

**How recommendation source selection works:**
1. `AiBundleRecommendationProvider` is tried first — it calls out over HTTP to
   `http://localhost:8080/api/v1/bundle-recommendations` (see **Mock AI Endpoint** below).
2. If that call throws `AiRecommendationException` (network failure, malformed response, etc.),
   `DeterministicBundleRecomProvider` is used instead — a rules-based fallback (budget filter,
   closest-validity-match ranking, price tiebreak).
3. The top-level response's `source` field (`"AI"` or `"fallback"`) reflects which path actually
   served the request.

**Mock AI Endpoint** — `/api/v1/bundle-recommendations` (internal, called by the service itself,
not intended for direct external use): accepts `{customerReference, usageProfile,
availableProducts}` and returns `{"recommendedId": [...]}`. Currently delegates to the same
`DeterministicBundleRecomProvider` logic to guarantee valid, in-catalogue product codes.

---

### Purchases — `/api/bundle-purchases`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/bundle-purchases` | Purchase a product (idempotent) |
| `GET` | `/api/bundle-purchases/{purchaseId}` | Check purchase status |

**`POST` requires an `Idempotency-Key` header.** Retrying the same request with the same key
returns the original result instead of charging again — no duplicate purchase is created and
`PaymentClient.charge()` is not called a second time.

**Request body:**
```json
{
  "customerReference": "CUST-10291",
  "productCode": "PROD-004",
  "paymentMethod": "CARD",
  "channel": "APP",
  "msisdn": "+27821234567"
}
```

**Response:**
```json
{
  "purchaseId": "PUR-20260828-00001",
  "status": "SUCCESSFUL",
  "productCode": "PROD-004",
  "amount": 99.00,
  "currency": "ZAR"
}
```

**Purchase lifecycle (`purchaseStatus`):** `RECEIVED` → `VALIDATING` → `PROCESSING` →
`SUCCESSFUL` / `FAILED`. Provisioning is only attempted after payment succeeds. If provisioning
comes back `PROVISIONING_UNKNOWN`, the service retries the status check (not the payment) up to
`MAX_PROVISIONING_STATUS_RETRIES` (3) times with increasing backoff, without ever re-charging the
customer.

**`purchaseId` format:** `PUR-yyyyMMdd-NNNNN` (date + 5-digit random suffix).

**Simulating failures locally** (via magic trigger values in the mock clients):

| Trigger | Where | Effect |
|---|---|---|
| `paymentMethod: "DECLINE"` | request body | Forces `PaymentClient.charge()` to fail |
| `productCode: "PROD-FAIL"` | request body | Forces provisioning to genuinely fail → payment reversed |
| `productCode: "PROD-STUCK"` | request body | Provisioning stays `PROVISIONING_UNKNOWN` through all retries |

`PROD-FAIL`/`PROD-STUCK` must exist in the seeded catalogue data for the product-validation step
to pass before reaching payment.

---

## Global Error Handling

Handled centrally in `ControllerAdvice` (`@RestControllerAdvice`), automatically picked up by
`@WebMvcTest`'s component scan:

| Exception | Status | Body shape |
|---|---|---|
| `ResourceNotFoundException` | 404 | Structured JSON (`ErrorResponse`) |
| `ProductNotFoundException` | 404* | Plain text |
| `PurchaseNotFoundException` | 404 | Plain text |
| `IllegalArgumentException` | 400 | Plain text |
| `MethodArgumentTypeMismatchException` | 400 | Structured JSON (`ErrorResponse`) |
| Anything else (catch-all) | 500 | Structured JSON, generic `"An unexpected error occurred"` message (internal details not leaked) |

\* `ProductNotFoundException` originally mapped to `400`; corrected to `404` for consistency with
the other not-found exceptions.

`RecommendationController` additionally has its **own local** `@ExceptionHandler(IllegalArgumentException.class)`,
which takes precedence over the global advice for that specific controller — any other exception
type there still falls through to the global catch-all.

**`ErrorResponse` shape:**
```json
{
  "timestamp": "2026-08-28T15:20:48.449813",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value 'not-a-number' for parameter 'maxPrice'",
  "path": "",
  "errDetails": null
}
```

---

## Known Issues / Design Notes

Documented here rather than silently fixed, so they can be discussed intentionally (e.g. in an
interview) rather than discovered by surprise:

1. **`PurchaseEntity.purchaseStatus` doesn't transition to `FAILED`** when payment fails or when
   provisioning genuinely fails (only `paymentStatus` is updated in those branches). The overall
   purchase status stays `"PROCESSING"` even though the purchase did not succeed. Covered by
   `bugCheck_*` tests in `PurchaseServiceImplTest`.
2. **`RecommendationItem`'s `source` field is populated with the `reason` text**, not the actual
   `"AI"`/`"fallback"` value — only the top-level `RecommendationResponse.source` is correct.
   Covered by a `bugCheck_*` test in `RecommendationServiceImplTest`.
3. **`recommendationId` format** (`"REC" + Math.random() double`) produces an unwieldy, decimal-point-containing
   ID with a small but non-zero collision chance — worth revisiting, especially once used as a
   URL path variable.
4. **`ErrorResponse.path`** is populated via `req.getRequestId()` (the servlet container's
   internal per-request tracking ID) rather than `req.getRequestURI()` — so it's currently
   always blank/meaningless in practice.
5. **Two latent exceptions in confidence scoring** (`RecommendationServiceImpl.calculateConfidence`):
   a non-null-but-unparsable `validity` string (e.g. `"unlimited"`) throws `NullPointerException`
   on auto-unboxing; a product priced at exactly `0` with a `0` maximum budget throws
   `ArithmeticException` (divide by zero). Both covered by explicit tests.
6. **Response shape inconsistency** across `ControllerAdvice` handlers — some return plain text,
   others return structured JSON, depending on which exception fired.

---

## Testing Summary

Unit tests exist for every layer, using Mockito mocks for true external collaborators and real
constructed instances for simple value objects/DTOs (rather than mocking data classes):

| Class | Test focus |
|---|---|
| `DeterministicBundleRecomProviderTest` | Budget filtering, validity ranking, price tiebreak, parsing edge cases |
| `AiBundleRecommendationProviderTest` | Request construction, response parsing, failure wrapping into `AiRecommendationException` |
| `ProductCatalogueServiceImplTest` | Validation, type/maxPrice normalization |
| `RecommendationServiceImplTest` | AI/fallback routing, confidence scoring, both known bugs |
| `PurchaseServiceImplTest` | Full purchase state machine, idempotency dedup, retry resolution/exhaustion, both known bugs |
| `ControllerAdviceTest` | Each exception handler in isolation, both known bugs |
| `ProductCatalogueControllerTest`, `RecommendationControllerTest`, `PurchaseControllerTest` | HTTP-layer behaviour via `MockMvc`, request/response shape, status codes |

Run the full suite with:
```bash
mvn clean test
```

---

## Suggested Next Steps

- Fix the `purchaseStatus`/`RecommendationItem.source` bugs above (tests are written to flip
  green once fixed — update the corresponding assertions when you do).
- Add a handler for `MissingRequestHeaderException` (or a shared handler for
  `ServletRequestBindingException`, which covers both that and `MethodArgumentTypeMismatchException`
  in one place) so a missing `Idempotency-Key` header returns `400` instead of `500`.
- Consider replacing the `recommendationId` generation with `"REC-" + UUID.randomUUID()`.
- If recommendation lookup-by-ID is needed, a `RecommendationEntity` + repository + service method
    + controller endpoint were drafted but not yet confirmed merged — check whether that's in place.