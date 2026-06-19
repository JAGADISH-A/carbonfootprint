import apiClient from './client'
import type {
  ApiResponse,
  CarbonAnalyticsResponse,
  CarbonInsightResponse,
  AICarbonCoachResponse,
  ReceiptUploadResponse,
  ChatRequest,
  ChatResponse,
} from '@/types/activity'

export async function getCarbonAnalytics(params?: {
  from?: string
  to?: string
}, signal?: AbortSignal): Promise<ApiResponse<CarbonAnalyticsResponse>> {
  const response = await apiClient.get<ApiResponse<CarbonAnalyticsResponse>>(
    '/api/v1/carbon/analytics',
    { params, signal }
  )
  return response.data
}

export async function getCarbonInsights(params?: {
  from?: string
  to?: string
}, signal?: AbortSignal): Promise<ApiResponse<CarbonInsightResponse>> {
  const response = await apiClient.get<ApiResponse<CarbonInsightResponse>>(
    '/api/v1/carbon/insights',
    { params, signal }
  )
  return response.data
}

export async function getAICoach(
  params?: { from?: string; to?: string },
  signal?: AbortSignal
): Promise<ApiResponse<AICarbonCoachResponse>> {
  const response = await apiClient.get<ApiResponse<AICarbonCoachResponse>>(
    '/api/v1/carbon/coach',
    { params, signal }
  )
  return response.data
}

export async function uploadReceipt(
  file: File,
  categoryHint?: string
): Promise<ApiResponse<ReceiptUploadResponse>> {
  const formData = new FormData()
  formData.append('file', file)
  if (categoryHint) {
    formData.append('categoryHint', categoryHint)
  }

  const response = await apiClient.post<ApiResponse<ReceiptUploadResponse>>(
    '/api/v1/documents/upload',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    }
  )
  return response.data
}

export async function sendChatMessage(
  request: ChatRequest,
  signal?: AbortSignal
): Promise<ApiResponse<ChatResponse>> {
  const response = await apiClient.post<ApiResponse<ChatResponse>>(
    '/api/v1/carbon/chat',
    request,
    { signal }
  )
  return response.data
}
