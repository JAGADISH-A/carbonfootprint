<div align="center">

# CarbonWise

### AI-Powered Carbon Footprint Intelligence Platform

Automatic carbon tracking from receipts, SMS, and notifications — powered by AI.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Python](https://img.shields.io/badge/Python-3.10-3776AB?style=flat-square&logo=python&logoColor=white)](https://www.python.org/)

[Live App](https://carbon-backend-890856260681.us-central1.run.app/) · [Deployment Guide](DEPLOYMENT.md)

</div>

---

## Overview

CarbonWise is a full-stack carbon intelligence platform that automatically tracks, calculates, and coaches users on their carbon footprint by ingesting data from multiple real-world sources — no manual entry required.

### Why CarbonWise?

- **Time-consuming** — manually logging every purchase and commute is impractical
- **Error-prone** — people forget or misclassify activities
- **Disconnected** — existing tools require manual data entry
- **Insight-poor** — users rarely get actionable guidance on how to reduce their impact

CarbonWise solves all of this with automatic data capture and AI-powered coaching.

---

## Features

- **Automatic Receipt Scanning** — Upload a receipt photo or PDF. AI extracts merchant, items, amount, and category, then estimates the carbon impact.
- **SMS & Notification Ingestion** — Android companion app passively reads purchase-related SMS and app notifications, extracting transaction data in the background.
- **AI Carbon Coach** — Conversational AI powered by Llama 3.3 analyzes your activity history and provides personalized, actionable reduction tips.
- **Real-time Analytics Dashboard** — Visualize your carbon footprint across categories, track monthly trends, compare against benchmarks, and see equivalencies (trees, flights, car miles).
- **Multi-Source Ingestion Pipeline** — Extensible pipeline architecture: receipts, SMS, email, and bank CSV — all normalized into a single activity model.
- **Background Sync** — Android app continuously collects data and syncs to the cloud via WorkManager, even when the app is closed.

---

## Screenshots

### Web Dashboard

- Dashboard Overview
- Receipt Upload
- Activity History
- Analytics
- AI Coach
- Settings

> *(Add screenshots here)*

---

### Android Companion

- Pairing
- Permissions
- Companion Status
- Sync Screen

> *(Add screenshots here)*

### Supported Data Sources

| Source | Status | Flow |
|---|---|---|
| Receipt (Image/PDF) | Active | Upload → OCR → AI Parse → Carbon Assessment |
| SMS | Active | Android collects → Filters → Sync to backend |
| Notifications | Active | Android listener → Classifies → Sync to backend |
| Email | Planned | Gmail API → Parse → Normalize |
| Bank CSV | Planned | CSV Upload → Parse → Normalize |

---

## Architecture

```
Android App ─┐
             │
Receipt OCR ─┼──► Spring Boot API ───► Firestore
             │
Web App ─────┘
                     │
                     ▼
               AI Carbon Coach
```

### Data Flow

```
Receipts / SMS / Notifications
            │
            ▼
      AI Processing
            │
            ▼
 Carbon Calculation
            │
            ▼
 Dashboard & AI Coach
```

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.3
- Google Cloud Firestore
- Groq API (Llama 3.3)
- PaddleOCR
- JWT Authentication

### Frontend
- React 18
- TypeScript
- Tailwind CSS
- Vite
- Framer Motion

### Android
- Kotlin 2.1
- Jetpack Compose + Material 3
- Dagger Hilt
- Room
- WorkManager
- Retrofit
- Min SDK: 26 (Android 8.0+)

### OCR Sidecar
- Python 3.10
- FastAPI
- PaddleOCR
- PyMuPDF

---

## Project Structure

```
backend/
frontend/
android/
ocr/
docs/
assets/
```

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 20+
- Python 3.10+
- Google Cloud SDK
- Android Studio

### Backend

```bash
cd backend
cp .env.example .env
gcloud auth application-default login
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend runs at `http://localhost:8080`

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/api/v1/health`

### OCR Sidecar

```bash
cd ocr
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

### Android

```bash
cd android
./gradlew assembleDebug
```

For release build:

```bash
$env:CARBON_API_RELEASE_URL="https://your-production-url/"
./gradlew assembleRelease
```

APK output: `android/app/build/outputs/apk/release/app-release.apk`

### Docker Compose

Start backend + OCR together:

```bash
docker-compose up --build
```

---

## Environment Variables

### Backend
- `GCP_PROJECT_ID` — Google Cloud project ID
- `GROQ_API_KEY` — Groq API key for AI features
- `PADDLE_OCR_URL` — OCR sidecar URL

### Frontend
- `VITE_API_URL` — Backend API base URL

### Android
- `CARBON_API_RELEASE_URL` — Production API endpoint

---

## Deployment

| Service | Target |
|---|---|
| Backend | Google Cloud Run |
| Frontend | Firebase Hosting / Cloud Run |
| Android | Release APK |

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

---

## Android Permissions

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Read incoming SMS messages |
| `READ_SMS` | Access SMS history |
| `INTERNET` | Sync data to backend |
| `POST_NOTIFICATIONS` | Show sync status |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read app notifications |

---

## Future Improvements

- Email ingestion via Gmail API
- Bank CSV import
- Team/organization carbon tracking
- Carbon offset marketplace integration
- Weekly email reports

---

## License

This project is for educational and demonstration purposes.
