<div align="center">

# CarbonWise

### AI-Powered Carbon Footprint Intelligence Platform

Automatic carbon tracking from receipts, SMS, and notifications — powered by AI.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Python](https://img.shields.io/badge/Python-3.10-3776AB?style=flat-square&logo=python&logoColor=white)](https://www.python.org/)

**Live Backend** • **Web Dashboard** • **Android Companion**

[Backend API](https://carbon-backend-890856260681.us-central1.run.app/) · [Deployment Guide](DEPLOYMENT.md)

</div>

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Solution](#solution)
- [Live Deployment](#live-deployment)
- [Screenshots](#screenshots)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Ingestion Pipeline](#ingestion-pipeline)
- [Android Companion](#android-companion)
- [Deployment](#deployment)
- [License](#license)

---

## Problem Statement

Individuals and organizations lack an accessible, automated way to measure their carbon footprint. Manual carbon tracking is:

- **Time-consuming** — manually logging every purchase, commute, and transaction is impractical.
- **Error-prone** — people forget or misclassify activities.
- **Disconnected** — existing tools require manual data entry and don't integrate with real-world transaction sources.
- **Insight-poor** — even when data is collected, users rarely get actionable guidance on how to reduce their impact.

There is no unified system that automatically captures carbon-relevant transactions from receipts, SMS messages, and app notifications, then provides AI-powered analysis and personalized coaching.

---

## Solution

CarbonWise is a full-stack carbon intelligence platform that automatically tracks, calculates, and coaches users on their carbon footprint by ingesting data from multiple real-world sources.

### Core Capabilities

- **Automatic Receipt Scanning** — Upload a receipt photo or PDF. AI extracts merchant, items, amount, and category, then estimates the carbon impact.
- **SMS & Notification Ingestion** — An Android companion app passively reads purchase-related SMS and app notifications, extracting transaction data in the background.
- **AI Carbon Coach** — A conversational AI coach powered by Llama 3.3 analyzes your activity history and provides personalized, actionable reduction tips.
- **Real-time Analytics Dashboard** — Visualize your carbon footprint across categories, track monthly trends, compare against benchmarks, and see equivalencies (trees, flights, car miles).
- **Multi-Source Ingestion Pipeline** — Extensible pipeline architecture: receipts, SMS, email, and bank CSV — all normalized into a single activity model.
- **Background Sync** — The Android app continuously collects data and syncs to the cloud via WorkManager, even when the app is closed.

---

## Live Deployment

| Service | URL | Status |
|---|---|---|
| Backend API | [carbon-backend-890856260681.us-central1.run.app](https://carbon-backend-890856260681.us-central1.run.app/) | Running |
| Swagger UI | [Backend /swagger-ui.html](https://carbon-backend-890856260681.us-central1.run.app/swagger-ui.html) | Running |
| Health Check | [Backend /api/v1/health](https://carbon-backend-890856260681.us-central1.run.app/api/v1/health) | Healthy |

> **Note:** The frontend dashboard and Android APK are built locally. See [Deployment](#deployment) for full production deployment instructions.

---

## Screenshots

> Add your screenshots to the `assets/` directory and reference them below.

### Web Dashboard

#### Home — Carbon Overview
<!-- Add screenshot: assets/web-home.png -->
*Coming soon*

#### Upload — Receipt Scanning
<!-- Add screenshot: assets/web-upload.png -->
*Coming soon*

#### Activities — Transaction History
<!-- Add screenshot: assets/web-activities.png -->
*Coming soon*

#### Analytics — Carbon Footprint Breakdown
<!-- Add screenshot: assets/web-analytics.png -->
*Coming soon*

#### Analytics — Monthly Trends & Insights
<!-- Add screenshot: assets/web-insights.png -->
*Coming soon*

#### AI Coach — Personalized Carbon Guidance
<!-- Add screenshot: assets/web-coach.png -->
*Coming soon*

#### Settings — Account & Preferences
<!-- Add screenshot: assets/web-settings.png -->
*Coming soon*

### Android Companion App

#### Pairing — Device Setup
<!-- Add screenshot: android-pairing.png -->
*Coming soon*

#### Permissions — SMS & Notification Access
<!-- Add screenshot: android-permissions.png -->
*Coming soon*

#### Companion Status — Health Dashboard
<!-- Add screenshot: android-status.png -->
*Coming soon*

#### Settings — Sync & Preferences
<!-- Add screenshot: android-settings.png -->
*Coming soon*

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Carbon Intelligence Platform              │
├──────────────────┬──────────────────┬───────────────────────┤
│   Web Dashboard  │ Android Companion│     OCR Sidecar       │
│   React + TS     │   Kotlin         │   Python + PaddleOCR  │
│   Vite + Tailwind│   Jetpack Compose│   FastAPI             │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         │ HTTPS            │ HTTPS               │ HTTP
         ▼                  ▼                     │
┌────────────────────────────────────────────────┴────────────┐
│                   Spring Boot Backend                        │
│               Java 21 · Hexagonal Architecture               │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐   │
│  │  Ingestion   │  │   Carbon     │  │   Mobile / Auth   │   │
│  │  Pipeline    │  │   Assessment │  │   (JWT + Pairing) │   │
│  └──────┬──────┘  └──────┬───────┘  └─────────┬─────────┘   │
│         │                │                     │             │
│         └────────────────┼─────────────────────┘             │
│                          ▼                                   │
│                ┌──────────────────┐                           │
│                │  Google Cloud    │                           │
│                │  Firestore (NoSQL)│                          │
│                └──────────────────┘                           │
└──────────────────────────────────────────────────────────────┘
```

### Ingestion Pipeline

```
Source          RawDocument    Validated    Parsed      Normalized    Activity
─────────       ──────────     ─────────    ──────      ──────────    ────────
RECEIPT (OCR)   OCR text       fields       AI          merchant      saved
SMS             SMS text       timestamp    extract     currency      saved
NOTIFICATION    notif body     relevance    classify    category      saved
EMAIL (future)  email body     dups         extract     units         saved
BANK (future)   CSV row        category     fields      location      saved
```

---

## Tech Stack

### Backend

| Component | Technology | Purpose |
|---|---|---|
| Language | Java 21 | Core runtime |
| Framework | Spring Boot 3.3.0 | REST API, DI, security |
| Database | Google Cloud Firestore | NoSQL document store |
| AI (Parsing) | Groq API (GPT-OSS-20B) | Receipt OCR post-processing |
| AI (Coach) | Groq API (Llama 3.3 70B) | Conversational carbon coach |
| OCR | PaddleOCR (Python sidecar) | Image/PDF text extraction |
| Auth | JWT (JJWT 0.12.5) | Device token authentication |
| API Docs | SpringDoc OpenAPI 2.5.0 | Swagger UI |
| Build | Maven | Dependency management |
| Architecture | Hexagonal (Ports & Adapters) + DDD | Clean separation of concerns |

### Frontend

| Component | Technology | Purpose |
|---|---|---|
| Language | TypeScript 5.4 | Type-safe development |
| Framework | React 18.3 | UI framework |
| Build | Vite 5.3 | Fast dev server & bundler |
| Styling | Tailwind CSS 3.4 | Utility-first CSS |
| Routing | React Router DOM 6.23 | SPA navigation |
| HTTP | Axios 1.7 | API communication |
| Animation | Framer Motion 12.4 | Smooth transitions |
| Icons | Lucide React 0.39 | Icon library |
| Server | Nginx 1.25 (production) | Static file serving |

### Android Companion

| Component | Technology | Purpose |
|---|---|---|
| Language | Kotlin 2.1 | Native Android |
| UI | Jetpack Compose + Material 3 | Modern declarative UI |
| DI | Dagger Hilt 2.53 | Dependency injection |
| Database | Room 2.6 | Local persistence |
| Networking | Retrofit 2.11 + OkHttp 4.12 | HTTP client |
| Background | WorkManager 2.10 | Scheduled background sync |
| Security | EncryptedSharedPreferences | Token storage |
| Min SDK | 26 (Android 8.0) | Device compatibility |

### OCR Sidecar

| Component | Technology | Purpose |
|---|---|---|
| Language | Python 3.10 | Scripting runtime |
| Framework | FastAPI 0.111 | REST API |
| OCR Engine | PaddleOCR 2.7 | Text extraction |
| PDF | PyMuPDF 1.24 | PDF processing |
| Server | Uvicorn 0.30 | ASGI server |

---

## Project Structure

```
carbonfootprint/
├── backend/                  # Spring Boot REST API
│   ├── src/main/java/        # Application source
│   │   └── com/carbonfootprint/platform/
│   │       ├── platform/     # Spring config, health, exceptions
│   │       ├── ingestion/    # Pipeline orchestration & normalization
│   │       ├── document/     # RawDocument domain
│   │       ├── activity/     # Activity domain + Firestore adapter
│   │       ├── carbon/       # Carbon assessment + AI coach
│   │       ├── integration/  # OCR, Groq AI, Gmail adapters
│   │       ├── mobile/       # Android companion API
│   │       └── shared/       # Utilities, constants
│   ├── src/test/             # Unit & integration tests
│   ├── Dockerfile            # Multi-stage container build
│   └── pom.xml               # Maven dependencies
│
├── frontend/                 # React dashboard
│   ├── src/
│   │   ├── pages/            # Home, Upload, Activities, Analytics, Coach, Settings
│   │   ├── components/       # Reusable UI components
│   │   ├── api/              # Axios client & service functions
│   │   └── types/            # TypeScript type definitions
│   ├── Dockerfile            # Multi-stage Nginx build
│   └── package.json          # npm dependencies
│
├── android/                  # Android companion app
│   ├── app/src/main/java/    # Kotlin source
│   │   └── com/carbonwise/connect/
│   │       ├── data/         # Network, local DB, auth, repositories
│   │       ├── di/           # Hilt modules
│   │       ├── ingestion/    # Notification & SMS processing pipeline
│   │       ├── sms/          # SMS collection, filtering, normalization
│   │       ├── service/      # WorkManager sync worker
│   │       ├── ui/           # Compose screens & viewmodels
│   │       └── navigation/   # Navigation graph
│   ├── app/build.gradle.kts  # Android build config
│   └── build.gradle.kts      # Root Gradle config
│
├── ocr/                      # PaddleOCR Python sidecar
│   ├── app.py                # FastAPI application
│   ├── requirements.txt      # Python dependencies
│   └── Dockerfile            # Container build
│
├── assets/                   # Screenshots & media
├── docs/                     # Documentation
├── docker-compose.yml        # Local multi-service orchestration
├── DEPLOYMENT.md             # Production deployment guide
└── README.md                 # This file
```

---

## Getting Started

### Prerequisites

| Requirement | Version | Purpose |
|---|---|---|
| Java | 21+ | Backend runtime |
| Node.js | 20+ | Frontend build |
| Python | 3.10+ | OCR sidecar |
| Google Cloud SDK | Latest | `gcloud` CLI for deployment |
| Android Studio | Latest | Android app development |

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/carbonfootprint.git
cd carbonfootprint
```

### 2. Backend Setup

```bash
# Configure environment
cp .env.example .env
# Edit .env with your GCP project ID, Groq API key, etc.

# Authenticate with Google Cloud
gcloud auth application-default login

# Start the backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend runs at `http://localhost:8080`

- Health: `http://localhost:8080/api/v1/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

### 3. OCR Sidecar Setup

```bash
cd ocr
python -m venv venv
source venv/bin/activate        # Linux/macOS
venv\Scripts\activate           # Windows
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

Or use Docker Compose to start both backend + OCR:

```bash
docker-compose up --build
```

### 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

### 5. Android Setup

```bash
cd android
# Debug build (connects to localhost backend via emulator)
./gradlew assembleDebug

# Release build (points to production backend)
$env:CARBON_API_RELEASE_URL="https://carbon-backend-890856260681.us-central1.run.app/"
./gradlew assembleRelease
```

APK output: `android/app/build/outputs/apk/release/app-release.apk`

---

## Environment Variables

### Backend

| Variable | Description | Required |
|---|---|---|
| `PORT` | Server port (default: 8080) | Auto (Cloud Run) |
| `SPRING_PROFILES_ACTIVE` | `local` or `prod` | Yes |
| `GCP_PROJECT_ID` | Google Cloud project ID | Yes |
| `GROQ_API_KEY` | Groq API key for AI features | Yes |
| `OCR_PROVIDER` | `paddle` for PaddleOCR | Yes |
| `PADDLE_OCR_URL` | OCR sidecar URL | Yes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Yes |
| `GROQ_RECEIPT_MODEL` | Receipt parsing model | Optional |
| `GROQ_COACH_MODEL` | AI coach model | Optional |

### Frontend

| Variable | Description | Required |
|---|---|---|
| `VITE_API_URL` | Backend API base URL | Yes (build time) |

### Android

| Variable | Description | Default |
|---|---|---|
| `CARBON_API_DEBUG_URL` | Debug build API URL | `http://10.0.2.2:8080/` |
| `CARBON_API_RELEASE_URL` | Release build API URL | `https://carbon-backend-890856260681.us-central1.run.app/` |

---

## Ingestion Pipeline

The platform uses a **pluggable ingestion pipeline** where every data source produces a `RawDocument` that flows through validation, parsing, normalization, and finally becomes an `Activity`.

### Adding a New Source

1. Implement the `IngestionSource` interface
2. Annotate with `@Component`
3. Implement `getSource()`, `supports()`, and `ingest()`
4. The pipeline picks it up automatically — no other changes required

### Currently Supported Sources

| Source | Status | Data Flow |
|---|---|---|
| Receipt (Image/PDF) | Active | Upload → OCR → AI Parse → Carbon Assessment |
| SMS | Active | Android collects → Filters → Normalizes → Sync to backend |
| Notifications | Active | Android listener → Classifies → Enriches → Sync to backend |
| Email | Planned | Gmail API → Parse → Normalize |
| Bank CSV | Planned | CSV Upload → Parse → Normalize |

---

## Android Companion

The **CarbonWise Connect** Android app provides automatic carbon activity detection from your phone.

### Features

- **SMS Detection** — Passively reads incoming SMS messages, identifies purchase-related texts (fuel, food delivery, shopping, flights, electricity), and extracts transaction data.
- **Notification Interception** — Listens for payment and shopping notifications from apps like Google Pay, banking apps, and e-commerce platforms.
- **Local Processing** — All data is processed on-device first. Only relevant, enriched transactions are synced to the cloud.
- **Background Sync** — WorkManager ensures periodic data upload even when the app is closed. Respects battery optimization settings.
- **Device Pairing** — Secure JWT-based pairing with the backend via a simple code.
- **Offline Queue** — If network is unavailable, activities are queued locally and synced when connectivity returns.

### Permissions Required

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Read incoming SMS messages |
| `READ_SMS` | Access SMS history for initial sync |
| `INTERNET` | Sync data to backend |
| `POST_NOTIFICATIONS` | Show sync status notifications |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read app notifications |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Reliable background sync |

---

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full production deployment guide covering:

- Google Cloud Run deployment (backend + OCR)
- Firebase Hosting or Cloud Run (frontend)
- Android APK release packaging
- Environment variable configuration
- Secrets management
- Troubleshooting

### Quick Deploy (Docker Compose — Local)

```bash
docker-compose up --build
```

This starts:
- Backend at `http://localhost:8080`
- OCR sidecar at `http://localhost:8001`

---

## License

This project is for educational and demonstration purposes.
