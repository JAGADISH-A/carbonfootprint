# Carbon Intelligence Platform

An AI-powered platform for automatic carbon footprint tracking.

## Architecture

```
Hexagonal Architecture (Ports & Adapters) + Domain-Driven Design

Ingestion Pipeline:
  IngestionSource → RawDocument → Validation → DocumentParser
  → Normalization → Activity → Repository → (future) CarbonAssessment
```

## Module Structure

| Module | Path | Phase |
|---|---|---|
| Backend API | `backend/` | Phase 1 |
| Frontend Dashboard | `frontend/` | Phase 1 |
| Android Companion | `android/` | Future |

## Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- Google Cloud SDK (`gcloud`)
- Application Default Credentials: `gcloud auth application-default login`

### Backend

```bash
# Copy and configure environment
cp .env.example .env
# Edit .env with your GCP project ID and Gemini API key

# Run backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend starts at: `http://localhost:8080`
Health check: `http://localhost:8080/api/v1/health`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at: `http://localhost:5173`

## Environment Variables

See [.env.example](.env.example) for the full list of required variables.

## Ingestion Pipeline

Every data source produces a `RawDocument`. Every `RawDocument` becomes an `Activity`.

```
Source        → RawDocument → Validated → Parsed   → Normalised → Activity
RECEIPT         OCR text       fields      Gemini      merchant     saved
GMAIL           email body     dups        extracts    currency     saved
SMS (future)    SMS text       timestamp   activity    units        saved
BANK (future)   CSV row        category    fields      location     saved
```

## Adding a New Ingestion Source

1. Create a class implementing `IngestionSource` interface.
2. Annotate with `@Component`.
3. Implement `getSource()`, `supports()`, and `ingest()`.
4. Done — the pipeline picks it up automatically. **No other changes required.**

## Project Structure (Backend)

```
com.carbonfootprint.platform/
├── platform/         ← Spring config, web, exceptions
├── ingestion/        ← Pipeline orchestration, validation, normalization
├── document/         ← RawDocument domain + ports
├── activity/         ← Activity domain + ports + Firestore adapter
├── carbon/           ← CarbonAssessment domain (future calculations)
├── integration/      ← External adapters (OCR, Gemini, Gmail)
└── shared/           ← Utilities, constants
```
