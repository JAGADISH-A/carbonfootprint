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
    // Log and re-throw for component-level handling
    console.error('[API Error]', error.response?.data ?? error.message)
    return Promise.reject(error)
  }
)

export default apiClient
