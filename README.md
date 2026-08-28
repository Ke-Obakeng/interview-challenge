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

### Recommendations — `/api/v1/bundles`

| Method | Path                                    | Description |
|---|-----------------------------------------|---|
| `POST` | `/api/v1/bundles/bundle-recommendation` | Get up to 3 recommended products for a usage profile |

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
  "recommendationId": "REC-54213",
  "recommendations": [
    { "productCode": "PROD-004", "confidence": 0.78, "source": "Closest products to the supplied monthly usage" }
  ],
  "source": "AI",
  "promptVersion": "bundle-recommendations-v1"
}
```
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

