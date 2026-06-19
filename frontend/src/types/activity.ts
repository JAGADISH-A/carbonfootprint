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
  occurredAt: string
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

// ── Backend response types ─────────────────────────────────────────────────

export interface CategoryEmissionSummary {
  category: string
  carbonKg: number
  activityCount: number
  percentageOfTotal: number
}

export interface MonthlyEmissionTrend {
  month: string
  carbonKg: number
  activityCount: number
}

export interface TopEmissionActivity {
  activityId: string
  merchant: string | null
  category: string
  carbonKg: number
  description: string | null
  occurredAt: string
}

export interface CarbonAnalyticsResponse {
  totalCarbonKg: number
  categoryTotals: CategoryEmissionSummary[]
  monthlyTrend: MonthlyEmissionTrend[]
  topActivities: TopEmissionActivity[]
  averageDailyKg: number
  activityCount: number
  periodStart: string
  periodEnd: string
}

export interface CarbonInsightResponse {
  summary: string
  achievements: string[]
  warnings: string[]
  recommendations: string[]
  insights: string[]
}

export interface ActionPlanItem {
  priority: number
  title: string
  whyItMatters: string
  whatToDo: string
}

export interface AICarbonCoachResponse {
  summary: string
  strengths: string[]
  concerns: string[]
  recommendations: string[]
  weeklyChallenge: string
  motivation: string
  confidence: number
  aiGenerated: boolean
  // Storytelling fields (new format)
  whatHappened?: string
  whatSurprisedMe?: string
  whatsGoingWell?: string
  biggestOpportunity?: string
  actionPlan?: ActionPlanItem[]
  closing?: string
}

export interface ReceiptUploadResponse {
  filename: string
  mimeType: string
  fileSize: number
  receivedAt: string
}

// ── UI utility types ───────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

// ── Chat types ────────────────────────────────────────────────────────────

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
}

export interface ChatRequest {
  messages: ChatMessage[]
}

export interface ChatResponse {
  reply: string
}
