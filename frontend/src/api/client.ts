import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/activity'

/**
 * Pre-configured Axios instance for all backend API calls.
 *
 * Base URL is read from VITE_API_BASE_URL environment variable.
 * Falls back to relative path '/' to use the Vite dev server proxy.
 *
 * Auth: Bearer token is injected via request interceptor.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30_000,
})

// ── Request interceptor: attach Bearer token ───────────────────────────────
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor: unwrap ApiResponse envelope ─────────────────────
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => response,
  (error) => {
    // Extract meaningful error message from backend ApiResponse envelope
    const status = error.response?.status
    const body = error.response?.data

    let message = error.message
    if (body && typeof body === 'object') {
      // Backend returns ApiResponse { success, message, errorCode, ... }
      message = body.message || body.errorCode || `Server error (${status})`
    } else if (status) {
      message = `Server error (${status})`
    }

    console.error(`[API Error] ${error.config?.url ?? ''}`, { status, message, body })

    // Attach readable message to the error so callers can display it
    ;(error as Record<string, unknown>).userMessage = message
    return Promise.reject(error)
  }
)

export default apiClient
