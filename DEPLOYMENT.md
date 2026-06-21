# CarbonWise Deployment Guide

This guide details the deployment of the CarbonWise platform to Google Cloud for a reliable hackathon demo.

## Deployment Architecture

```
Android Companion
        │
        │ HTTPS (Direct)
        ▼
Spring Boot Backend (Google Cloud Run)
        ▲
        │ HTTPS (Direct)
React Frontend (Firebase Hosting or Google Cloud Run)
```

The frontend and Android companion apps communicate directly with the Spring Boot backend over HTTPS.

---

## 1. Environment Variables

### Backend Configuration

The backend is configured via environment variables. Ensure the following are set in the Cloud Run service configuration:

| Variable | Description | Required / Optional |
|---|---|---|
| `PORT` | The port the container listens on (default: `8080`). Cloud Run sets this automatically. | Required (Auto) |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` for production. | Required |
| `GCP_PROJECT_ID` | Your Google Cloud Project ID where Firestore is running. | Required |
| `GROQ_API_KEY` | Your Groq API key for transaction category mapping & AI Coach. | Required |
| `OCR_PROVIDER` | Set to `paddle` to use the OCR sidecar. | Required |
| `PADDLE_OCR_URL` | URL of the deployed PaddleOCR container service. | Required |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins (e.g. `https://your-frontend.web.app`). | Required |
| `GROQ_RECEIPT_MODEL` | Receipt analysis model (defaults to `openai/gpt-oss-20b`). | Optional |
| `GROQ_COACH_MODEL` | AI coach model (defaults to `llama-3.3-70b-versatile`). | Optional |

### Frontend Configuration

Inject this variable at build time when compiling the React frontend application:

| Variable | Description | Value |
|---|---|---|
| `VITE_API_URL` | Absolute URL of the backend service. | `https://YOUR_BACKEND_CLOUD_RUN_URL` |

### Android Configuration

Provide these environment variables to the Gradle build context when packaging the Android APK:

| Variable | Description | Default (Fallback) |
|---|---|---|
| `CARBON_API_DEBUG_URL` | API base URL for debug build type (ends with `/`). | `http://10.0.2.2:8080/` |
| `CARBON_API_RELEASE_URL` | API base URL for release build type (ends with `/`). | `https://api.carbonwise.app/` |

---

## 2. Secrets Management

Do **NOT** commit any raw credentials or service account JSON files to version control.
- In **Google Cloud Run**, configure the service to run under a dedicated Google Cloud Service Account that has the **Cloud Datastore User** role (for Firestore access). The Google Cloud client libraries will automatically locate these credentials at runtime.
- For local testing, download a service account JSON file and configure `GOOGLE_APPLICATION_CREDENTIALS` to point to it, or run `gcloud auth application-default login`.

---

## 3. Step-by-Step Deployment

### Step 3.1: Deploy OCR Sidecar to Cloud Run

First, deploy the lightweight OCR container. It will provide the REST endpoint used by the backend.

1. Navigate to the `ocr` directory:
   ```bash
   cd ocr
   ```
2. Build and submit the image to Google Artifact Registry:
   ```bash
   gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/ocr-sidecar
   ```
3. Deploy to Cloud Run:
   ```bash
   gcloud run deploy ocr-sidecar \
     --image gcr.io/YOUR_PROJECT_ID/ocr-sidecar \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated
   ```
4. Note the generated URL (e.g. `https://ocr-sidecar-xxxx-uc.a.run.app`).

### Step 3.2: Deploy Backend to Cloud Run

1. Build and submit the backend image:
   ```bash
   cd backend
   gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/carbon-backend
   ```
2. Deploy the backend service to Cloud Run, specifying environment variables:
   ```bash
   gcloud run deploy carbon-backend \
     --image gcr.io/YOUR_PROJECT_ID/carbon-backend \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars="SPRING_PROFILES_ACTIVE=prod,GCP_PROJECT_ID=YOUR_PROJECT_ID,GROQ_API_KEY=YOUR_GROQ_KEY,OCR_PROVIDER=paddle,PADDLE_OCR_URL=https://ocr-sidecar-xxxx-uc.a.run.app,CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_DOMAIN"
   ```
3. Note the generated URL (e.g. `https://carbon-backend-xxxx-uc.a.run.app`).

### Step 3.3: Deploy Frontend

#### Option A: Firebase Hosting (Recommended)
1. Initialize Firebase in the `frontend` folder if not already done:
   ```bash
   cd frontend
   npx firebase-tools init hosting
   ```
2. Set the production URL environment variable and build:
   ```bash
   # Windows (PowerShell)
   $env:VITE_API_URL="https://YOUR_BACKEND_CLOUD_RUN_URL"
   npm run build

   # Linux/macOS
   VITE_API_URL="https://YOUR_BACKEND_CLOUD_RUN_URL" npm run build
   ```
3. Deploy to Firebase:
   ```bash
   npx firebase-tools deploy --only hosting
   ```

#### Option B: Google Cloud Run (Containerized)
1. Write a minimal Nginx/static Dockerfile for the frontend and build:
   ```bash
   gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/carbon-frontend
   ```
2. Deploy to Cloud Run:
   ```bash
   gcloud run deploy carbon-frontend \
     --image gcr.io/YOUR_PROJECT_ID/carbon-frontend \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated
   ```

---

## 4. Android Release Packaging

To build the companion APK pointing to your deployed backend:

1. Define the environment variable in your terminal:
   ```bash
   # Windows (PowerShell)
   $env:CARBON_API_RELEASE_URL="https://YOUR_BACKEND_CLOUD_RUN_URL/"
   ```
2. Build the release APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
3. The compiled APK will be located under `android/app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if signing configs are set up).

---

## 5. Troubleshooting

- **CORS Errors**: Ensure that the exact URL of your frontend (e.g. `https://your-app.web.app`) is included in the backend's `CORS_ALLOWED_ORIGINS` environment variable (without trailing slashes).
- **Firestore Permission Denied**: Verify that the service account running your backend Cloud Run revision has the **Cloud Datastore User** or **Firestore Admin** IAM permission.
- **Timeout on Uploads**: The OCR processing sidecar can take up to 20-30 seconds for PDFs. Cloud Run requests timeout at 60 seconds by default, which is sufficient, but ensure your networks are healthy.
