export interface CreateUrlResponse {
  shortCode: string
  shortUrl: string
  originalUrl: string
  createdAt: string
  expiresAt: string | null
}

export interface UrlStatsResponse {
  shortCode: string
  totalAccesses: number
  dailyAccesses: DailyAccess[]
}

export interface DailyAccess {
  date: string
  count: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface CreateUrlRequest {
  originalUrl: string
  expirationMinutes?: number
  customAlias?: string
}
