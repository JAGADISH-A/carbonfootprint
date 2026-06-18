import apiClient from './client'
import type {
  ApiResponse,
  CarbonAnalyticsResponse,
  CarbonInsightResponse,
  AICarbonCoachResponse,
  ReceiptUploadResponse,
} from '@/types/activity'

export async function getCarbonAnalytics(params?: {
  from?: string
  to?: string
}): Promise<ApiResponse<CarbonAnalyticsResponse>> {
  const response = await apiClient.get<ApiResponse<CarbonAnalyticsResponse>>(
    '/api/v1/carbon/analytics',
    { params }
  )
  return response.data
}

export async function getCarbonInsights(params?: {
  from?: string
  to?: string
}): Promise<ApiResponse<CarbonInsightResponse>> {
  const response = await apiClient.get<ApiResponse<CarbonInsightResponse>>(
    '/api/v1/carbon/insights',
    { params }
  )
  return response.data
}

export async function getAICoach(params?: {
  from?: string
  to?: string
}): Promise<ApiResponse<AICarbonCoachResponse>> {
  const response = await apiClient.get<ApiResponse<AICarbonCoachResponse>>(
    '/api/v1/carbon/coach',
    { params }
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
