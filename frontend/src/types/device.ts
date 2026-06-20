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
}

export interface PairingCodeResponse {
  pairingCode: string
  expiresAt: string
  expiresInSeconds: number
}
