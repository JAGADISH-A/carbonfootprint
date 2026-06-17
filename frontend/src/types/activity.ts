/**
 * TypeScript types mirroring the backend domain model.
 * These must stay in sync with Java enums and domain objects.
 */

// ── Enums (mirror Java enums) ──────────────────────────────────────────────

export enum ActivitySource {
  RECEIPT       = 'RECEIPT',
  GMAIL         = 'GMAIL',
  SMS           = 'SMS',
  BANK_STATEMENT = 'BANK_STATEMENT',
  IOT           = 'IOT',
  MANUAL        = 'MANUAL',
  UNKNOWN       = 'UNKNOWN',
}

export enum ActivityCategory {
  ELECTRICITY   = 'ELECTRICITY',
  FOOD          = 'FOOD',
  FUEL          = 'FUEL',
  FLIGHT        = 'FLIGHT',
  SHOPPING      = 'SHOPPING',
  TRANSPORT     = 'TRANSPORT',
  ACCOMMODATION = 'ACCOMMODATION',
  GAS           = 'GAS',
  WATER         = 'WATER',
  OTHER         = 'OTHER',
}

// ── Domain objects ─────────────────────────────────────────────────────────

export interface Activity {
  id: string
  userId: string
  source: ActivitySource
  category: ActivityCategory
  merchant: string | null
  amount: number | null
  currency: string | null
  unit: string | null
  location: string | null
  occurredAt: string // ISO 8601 UTC
  description: string | null
  rawDocumentId: string | null
  metadata: Record<string, unknown>
  createdAt: string
  updatedAt: string | null
}

export interface RawDocument {
  id: string
  source: ActivitySource
  mimeType: string
  rawText: string
  language: string | null
  confidence: number | null
  userId: string
  metadata: Record<string, unknown>
  createdAt: string
}

export interface CarbonAssessment {
  assessmentId: string
  activityId: string
  userId: string
  carbonKg: number
  methodology: string
  emissionFactorVersion: string
  emissionFactorValue: number | null
  emissionFactorUnit: string | null
  calculatedAt: string
  metadata: Record<string, unknown>
}

// ── API types ──────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string | null
  errorCode: string | null
  timestamp: string
}

export interface IngestionResult {
  success: boolean
  rawDocument: RawDocument
  activity: Activity | null
  message: string
  processedAt: string
}

// ── UI utility types ───────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}
