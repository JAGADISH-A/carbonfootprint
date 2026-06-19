# Carbon Intelligence Platform - Comprehensive Audit Report

**Audit Date:** December 2024  
**Auditor:** Opencode AI Assistant  
**Scope:** Full system audit including backend APIs, security, data layer, OCR/AI pipelines, and mobile readiness

---

## Executive Summary

The Carbon Intelligence Platform is a well-architected Spring Boot + React application implementing hexagonal architecture for carbon footprint tracking from receipts. The system uses PaddleOCR (via Python sidecar) for text extraction and Groq AI for semantic field extraction.

### Key Findings

| Category | Status | Critical Issues |
|----------|--------|-----------------|
| **Architecture** | ✅ Solid | Clean hexagonal architecture with ports/adapters pattern |
| **Backend APIs** | ⚠️ Functional | No authentication enforcement, hardcoded "anonymous" userId |
| **Security** | ❌ Critical | All endpoints public, no JWT validation, no RBAC |
| **OCR/AI Pipeline** | ✅ Working | PaddleOCR sidecar + Groq integration operational |
| **Data Layer** | ⚠️ Functional | Missing Firestore composite indexes, no migrations |
| **Mobile/Android** | 📋 Planned | Architecture documented, no implementation |

---

## 1. Backend APIs, Controllers, and DTOs

### 1.1 REST Endpoints

| HTTP Method | Path | Controller | Auth Required | Profile |
|-------------|------|------------|---------------|---------|
| `GET` | `/api/v1/health` | `HealthController` | No | Always |
| `GET` | `/api/v1/carbon/insights` | `CarbonInsightController` | **Not enforced** | Always |
| `GET` | `/api/v1/carbon/analytics` | `CarbonAnalyticsController` | **Not enforced** | Always |
| `GET` | `/api/v1/carbon/coach` | `AICarbonCoachController` | **Not enforced** | `!stub` only |
| `POST` | `/api/v1/carbon/chat` | `CarbonChatController` | **Not enforced** | `!stub` only |
| `POST` | `/api/v1/documents/upload` | `ReceiptUploadController` | **Not enforced** | Always |

**Critical Issue:** All controllers use hardcoded `"anonymous"` userId with TODO comments:
- `ReceiptUploadController.java:78` - "Extract userId from authenticated SecurityContext"
- `AICarbonCoachController.java:95` - "Extract from SecurityContext after Google Auth integration"
- `CarbonInsightController.java:95` - Same
- `CarbonAnalyticsController.java:84` - Same

### 1.2 DTOs Summary

**Request DTOs:**
- `ChatRequest` - `List<ChatMessage>` with `@NotEmpty`
- `ChatMessage` - `role` + `content` fields
- `IngestionRequest` - Source, file bytes, raw text, context map

**Response DTOs:**
- `ApiResponse<T>` - Standard envelope (success, data, message, errorCode, timestamp)
- `CarbonAnalyticsResponse` - Total emissions, category totals, monthly trends
- `CarbonInsightResponse` - Analytics + summary, achievements, warnings, recommendations
- `AICarbonCoachResponse` - Personalized coaching with confidence scores
- `ChatResponse` - Reply text + analysis cards
- `ReceiptUploadResponse` - Filename, mimeType, fileSize, receivedAt

### 1.3 Architectural Issues

1. **`CarbonChatController` bypasses UseCase pattern** - Injects `CarbonChatService` directly instead of using `UseCase` port interface like other controllers
2. **MIME type validation duplicated** - Both controller and `ReceiptIngestionSource` validate MIME types
3. **Unused constants** - `ApiConstants.INGESTION_PATH` and `ACTIVITIES_PATH` defined but no controllers mapped

---

## 2. Authentication and Security Design

### 2.1 Current State: NO Authentication

**SecurityConfig.java** configuration:
- **CSRF:** Disabled (REST API with JWT tokens)
- **Session Management:** Stateless
- **Authorization:** `.anyRequest().permitAll()` (temporary placeholder)
- **OAuth2 Resource Server:** Entirely commented out

### 2.2 Planned (Phase 2): Google OAuth 2.0

Configuration exists but is inactive:
- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `.env.example`
- JWK Set URI configured: `https://www.googleapis.com/oauth2/v3/certs`
- Spring Security OAuth2 Resource Server dependencies present

### 2.3 CORS Configuration

**WebConfig.java:**
- Allowed origins: Configurable via `carbon.cors.allowed-origins`
- Default: `http://localhost:5173,http://localhost:3000`
- Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Credentials: Allowed
- Preflight cache: 1 hour

### 2.4 Security Issues

| Severity | Issue | Impact |
|----------|-------|--------|
| **CRITICAL** | All endpoints publicly accessible | Any user can access all data without authentication |
| **CRITICAL** | No user identity in controllers | All requests operate against single "anonymous" user |
| **MODERATE** | No rate limiting | Vulnerable to abuse |
| **MODERATE** | Swagger UI publicly accessible | API documentation exposed in production |
| **LOW** | No `@PreAuthorize` annotations | No data ownership verification |

### 2.5 Secrets Management

✅ **Good Practice:** All secrets externalized via environment variables:
- Groq API Key: `GROQ_API_KEY`
- Google OAuth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- GCP Project: `GOOGLE_CLOUD_PROJECT`

✅ **Git Protection:** `.env` files correctly excluded from version control

---

## 3. OCR, AI, and Receipt Ingestion Pipelines

### 3.1 Ingestion Sources

| Source | Status | Implementation |
|--------|--------|----------------|
| `RECEIPT` | ✅ Active | `ReceiptIngestionSource` with PaddleOCR |
| `GMAIL` | ❌ Stub | `GmailIngestionSource` returns `PHASE_2_PENDING` |
| `SMS` | 📋 Planned | `SmsIngestionSource` for Android app |
| `BANK_STATEMENT` | 📋 Planned | Not implemented |

### 3.2 OCR Integration

**PaddleOcrProvider** (Production):
- Endpoint: `POST /ocr` on sidecar service
- Retry logic: 3 attempts with exponential backoff
- Error handling: Detailed HTTP error mapping (400, 404, 413, 415, 500, 502-504)

**PaddleOCR Sidecar** (Python FastAPI):
- Engine: PaddleOCR 2.7.3 with PaddlePaddle 2.6.2
- PDF handling: PyMuPDF (primary) + pdf2image (fallback)
- Health check: `GET /health`

### 3.3 AI/Groq Integration

**GroqClient** configuration:
- API Endpoint: `POST /openai/v1/chat/completions`
- Receipt model: `openai/gpt-oss-20b` (configurable)
- Coach model: `llama-3.3-70b-versatile`
- Retry logic: 3 attempts, exponential backoff (500ms initial)

**Receipt Extraction Prompt:**
```json
{
  "merchant": "str",
  "merchantType": "Restaurant|Supermarket|Hotel|...",
  "amount": 0,
  "currency": "INR|USD|EUR|GBP",
  "category": "ELECTRICITY|FOOD|FUEL|FLIGHT|SHOPPING|...",
  "confidence": 0.0
}
```

**Coach System Prompt:**
- Role: Proactive sustainability coach
- Output: summary, strengths, concerns, recommendations, weeklyChallenge, motivation
- Rules: Never invent numbers, always reference specific data

### 3.4 Carbon Calculation

**Strategy Pattern** with 5 calculators (all Order 10):
1. `FuelEmissionCalculator` - litres × emission factor
2. `ElectricityEmissionCalculator` - kWh × emission factor
3. `FlightEmissionCalculator` - distance × factor × cabin multiplier
4. `TransportEmissionCalculator` - distance × emission factor
5. `ShoppingEmissionCalculator` - spend × emission factor

**Missing Calculators:** FOOD, WATER, GAS, ACCOMMODATION categories pass through without carbon assessment.

### 3.5 Pipeline Flow

```
Receipt Upload → OCR (PaddleOCR) → AI Parsing (Groq) → Normalization → Carbon Calculation → Firestore
```

12-step pipeline in `IngestionPipelineService` with validation at multiple stages.

---

## 4. Firestore Models and Data Layer

### 4.1 Document Models

| Collection | Model | Key Fields |
|------------|-------|------------|
| `raw_documents` | `RawDocument` | id, source, rawText, confidence, metadata |
| `activities` | `Activity` | id, userId, category, merchant, amount, carbonKg (in metadata) |
| `emission_factors` | `EmissionFactor` | id, value, unit, category, region, validFrom/validTo |
| `carbon_assessments` | `CarbonAssessment` | assessmentId, activityId, carbonKg, methodology |

### 4.2 Repository Methods

**ActivityRepository:**
- `save(Activity)` - Upsert by ID
- `findByUserId(String userId)` - Ordered by occurredAt
- `findByUserIdAndOccurredAtBetween(userId, from, to)` - Time range query
- `existsByUserIdAndRawDocumentId(userId, rawDocumentId)` - Duplicate detection

**RawDocumentRepository:**
- `save(RawDocument)` - Upsert by ID
- `findById(String id)` - Get by ID

**EmissionFactorRegistry:**
- `find(category, fuelType, transportMode, region, validAt)` - Multi-dimensional lookup
- All factors loaded at startup, cached in-memory

### 4.3 Critical Issues

| Severity | Issue | Impact |
|----------|-------|--------|
| **HIGH** | Missing composite indexes | Queries will fail at runtime |
| **MEDIUM** | `CarbonAssessment` collection unused | Assessments stored in Activity.metadata instead |
| **MEDIUM** | `schemaVersion` never read | Dead weight, no migration logic |
| **MEDIUM** | Silent data corruption | `parseEnum()` and `parseInstant()` return null silently |
| **LOW** | BigDecimal inconsistency | Activity uses String, EmissionFactor uses double |
| **LOW** | No cache refresh | EmissionFactorRegistry requires restart to pick up changes |

### 4.4 Required Firestore Indexes

**Missing composite indexes (will cause runtime failures):**
1. `activities` collection: `userId ASC, occurredAt ASC`
2. `activities` collection: `userId ASC, rawDocumentId ASC`

---

## 5. Mobile/Android References and TODOs

### 5.1 Android Architecture (Planned)

**android/README.md** documents SMS-based carbon tracking:
- BroadcastReceiver → SmsFilterService → CarbonApiClient → Backend
- Permissions: `RECEIVE_SMS`, `READ_SMS`, `INTERNET`
- Integration: POST to `/api/v1/ingestion` with `source=SMS`

### 5.2 Backend Readiness

✅ **Already Prepared:**
- `ActivitySource.SMS` enum value defined
- `IngestionRequest` supports text-based sources
- `RawDocument.metadata` designed for `{sender, carrier}`
- `CategoryValidator` accepts `SMS` source
- All APIs are REST + JSON, suitable for mobile consumption

❌ **Missing:**
- `SmsIngestionSource` implementation (referenced but not created)
- Push notification infrastructure (zero references)
- Device management code (zero references)
- Mobile-specific authentication flows (OAuth2 commented out)

### 5.3 TODOs Related to Mobile

All are Phase 2 prerequisites:
- `SecurityConfig.java:57,62` - Enable OAuth2 Resource Server
- All controllers - Extract userId from SecurityContext
- `GmailIngestionSource.java:29` - Implement Gmail API client

---

## 6. Critical TODOs Summary

### 6.1 Authentication (CRITICAL)

| File | Line | TODO |
|------|------|------|
| `SecurityConfig.java` | 57 | `TODO (Phase 2): Uncomment when OAuth2 Resource Server is configured` |
| `SecurityConfig.java` | 62 | `TODO (Phase 2): Enable JWT validation` |
| `ReceiptUploadController.java` | 78 | `TODO: Extract userId from authenticated SecurityContext` |
| `AICarbonCoachController.java` | 95 | `TODO: Extract from SecurityContext after Google Auth integration` |
| `CarbonAnalyticsController.java` | 84 | `TODO: Extract from SecurityContext after Google Auth integration` |
| `CarbonInsightController.java` | 95 | `TODO: Extract from SecurityContext after Google Auth integration` |

### 6.2 Infrastructure (Phase 1 & 2)

| File | Line | TODO |
|------|------|------|
| `ReceiptIngestionSource.java` | 33 | `TODO (Phase 1): Wire real OCR provider` - **STALE** - Already wired |
| `GmailIngestionSource.java` | 29,51,64 | `TODO (Phase 2): Implement Gmail API client, OAuth` |
| `CurrencyNormalizer.java` | 25 | `TODO (Phase 2): Load mapping from Firestore config` |
| `DateNormalizer.java` | 20 | `TODO (Phase 2): Accept timezone hint from metadata` |
| `LocationNormalizer.java` | 22,36 | `TODO (Phase 2): Integrate with geocoding API` |
| `MerchantNormalizer.java` | 22,35 | `TODO (Phase 2): Load merchant alias map from Firestore` |
| `UnitNormalizer.java` | 26 | `TODO (Phase 2): Load mapping from Firestore config` |

---

## 7. Positive Findings

### 7.1 Architecture
- Clean hexagonal architecture with ports/adapters pattern
- Strategy pattern for calculators, validators, and normalizers
- Chain of Responsibility for validation and normalization
- Profile-based bean loading (`!stub` vs `stub`) for testing

### 7.2 Code Quality
- All controllers use standard `ApiResponse<T>` envelope
- OpenAPI/Swagger annotations on all controllers
- Centralized exception handling via `GlobalExceptionHandler`
- Configurable CORS (not hardcoded)

### 7.3 AI/ML Integration
- Robust retry logic with exponential backoff
- Typed exceptions for different error scenarios
- Graceful fallbacks (deterministic responses when AI fails)
- Response parsing with multiple fallback strategies

### 7.4 Data Integrity
- Duplicate detection via `rawDocumentId` check
- Timestamp validation (reject future dates >30 seconds)
- Confidence scoring for extraction quality
- Emission factor temporal validity (validFrom/validTo)

---

## 8. Recommendations

### 8.1 Immediate (Phase 1 Completion)

1. **Enable Authentication**
   - Uncomment OAuth2 Resource Server in `SecurityConfig.java`
   - Replace `"anonymous"` with actual JWT subject claim extraction
   - Add `@PreAuthorize` annotations for data ownership verification

2. **Fix Firestore Indexes**
   - Create composite indexes: `userId + occurredAt`, `userId + rawDocumentId`
   - Deploy `firestore.indexes.json` or manually create in Firebase Console

3. **Complete Missing Calculators**
   - Implement FOOD, WATER, GAS, ACCOMMODATION calculators
   - Or explicitly mark as "no calculation" in pipeline

### 8.2 Phase 2 Enhancements

4. **Rate Limiting**
   - Add rate limiting middleware (e.g., Bucket4j)
   - Protect AI endpoints from abuse

5. **Gmail Integration**
   - Implement `GmailIngestionSource` with OAuth token refresh
   - Add email body extraction logic

6. **Mobile Foundation**
   - Implement `SmsIngestionSource`
   - Add push notification infrastructure (FCM)
   - Create device management endpoints

### 8.3 Technical Debt

7. **Remove Stale References**
   - Update `DocumentParser.java` Javadoc (references `GeminiDocumentParser`)
   - Fix `StubInfrastructureConfig` log message (mentions "Gemini")
   - Clean up `schemaVersion` fields or implement migration logic

8. **Improve Data Layer**
   - Add `@Component` to `StubOcrProvider`
   - Implement cache refresh for `EmissionFactorRegistry`
   - Add batch writes for multi-document operations

---

## 9. Conclusion

The Carbon Intelligence Platform demonstrates solid architectural foundations with clean hexagonal design and well-separated concerns. The OCR/AI pipeline is operational and the data layer is functional despite missing indexes.

**Critical blockers for production:**
1. Authentication enforcement (all endpoints currently public)
2. User isolation (all data tied to "anonymous")
3. Firestore composite indexes (queries will fail at runtime)

**Ready for Phase 2:**
- Mobile/Android integration path is well-documented
- Backend structurally ready for SMS ingestion
- OAuth2 dependencies in place (just needs activation)

The project is well-positioned for completion with clear TODOs and architectural documentation guiding the next phases.

---

**Report Generated:** December 2024  
**Next Audit:** Recommended after Phase 2 authentication implementation
