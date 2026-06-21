export interface Device {
  id: string
  deviceId: string
  deviceName: string
  manufacturer: string
  model: string
  androidVersion: string
  appVersion: string
  lastSeenAt?: string
  createdAt?: string
  lastSyncAt?: string
  pendingUploadCount?: number
  lastUploadStatus?: string
}

export interface PairingCodeResponse {
  pairingCode: string
  expiresAt: string
  expiresInSeconds: number
}

export interface CompanionRelease {
  available: boolean
  version: string
  downloadUrl: string
  fileSizeBytes: number
  releaseDate: string
  releaseNotes: string[]
}
