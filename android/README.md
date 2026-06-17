# Android Companion App — Future Architecture

## Purpose

The Android Companion App enables SMS-based carbon tracking. 

> **Important**: A web application CANNOT read SMS messages due to browser security restrictions.
> SMS processing MUST live in a native Android app with `READ_SMS` permission.

## Architecture

```
┌──────────────────────────────────────────────────┐
│             Android Companion App                 │
│                                                   │
│  ┌─────────────────┐    ┌───────────────────────┐ │
│  │ SmsReceiver     │───▶│ SmsFilterService      │ │
│  │ (BroadcastRcvr) │    │ (filter relevant SMS) │ │
│  └─────────────────┘    └──────────┬────────────┘ │
│                                    │              │
│                         ┌──────────▼────────────┐ │
│                         │ CarbonApiClient       │ │
│                         │ POST /api/v1/ingestion │ │
│                         │ source=SMS             │ │
│                         └──────────┬────────────┘ │
└────────────────────────────────────│──────────────┘
                                     │
                         ┌───────────▼────────────┐
                         │ Spring Boot Backend     │
                         │ SmsIngestionSource      │
                         │ → RawDocument           │
                         │ → Validation            │
                         │ → DocumentParser        │
                         │ → Activity              │
                         │ → Firestore             │
                         └─────────────────────────┘
                                     │
                         ┌───────────▼────────────┐
                         │ Web Dashboard           │
                         │ (reads from Firestore)  │
                         └─────────────────────────┘
```

## SMS Sources to Track

| Pattern | Example |
|---|---|
| Fuel / petrol | "HPCL: You have purchased 35.5L of petrol for Rs.3200" |
| Electricity | "BESCOM: Your bill of Rs.850 is due on 15-Jan" |
| Food delivery | "Swiggy order delivered: Rs.450" |
| Flight booking | "IndiGo booking confirmed: BOM→DEL 2024-02-01" |
| Shopping | "Amazon order shipped: Rs.2,399" |

## Android Manifest Permissions

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Integration Contract

The Android app sends each relevant SMS as an `IngestionRequest` to the backend:

```json
POST /api/v1/ingestion
Authorization: Bearer <Google ID Token>
Content-Type: application/json

{
  "source": "SMS",
  "rawText": "HPCL: You have purchased 35.5L of petrol for Rs.3200",
  "context": {
    "sender": "+914412345678",
    "receivedAt": "2024-01-15T10:30:00Z",
    "carrier": "Jio"
  }
}
```

## Backend Readiness

The backend is already prepared for SMS:
- `ActivitySource.SMS` is defined in the enum
- `IngestionSource` interface is ready for `SmsIngestionSource` implementation
- `GmailIngestionSource` serves as implementation reference
- Pipeline is fully generic — SMS goes through the same validation/parsing/normalisation steps

## Implementation Steps (Future)

1. Create Android project (Kotlin + Jetpack Compose)
2. Implement `SmsReceiver` as `BroadcastReceiver`
3. Filter SMS by keyword patterns loaded from backend config API
4. Implement `CarbonApiClient` using Retrofit
5. Handle Google Sign-In for JWT token management
6. Create `SmsIngestionSource.java` in backend (`integration/sms/`)
7. Test end-to-end pipeline
