import axios from 'axios'
import type { CreateUrlRequest, CreateUrlResponse, UrlStatsResponse, PageResponse } from '@/types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': import.meta.env.VITE_API_KEY || '',
  },
})

export async function createShortUrl(data: CreateUrlRequest): Promise<CreateUrlResponse> {
  const response = await api.post<CreateUrlResponse>('/api/urls', data)
  return response.data
}

export async function listUrls(page = 0, size = 20): Promise<PageResponse<CreateUrlResponse>> {
  const response = await api.get<PageResponse<CreateUrlResponse>>('/api/urls', {
    params: { page, size },
  })
  return response.data
}

export async function getUrlStats(shortCode: string): Promise<UrlStatsResponse> {
  const response = await api.get<UrlStatsResponse>(`/api/urls/${shortCode}/stats`)
  return response.data
}

export async function deleteUrl(shortCode: string): Promise<void> {
  await api.delete(`/api/urls/${shortCode}`)
}

export function getQrCodeUrl(shortCode: string): string {
  const base = import.meta.env.VITE_API_URL || ''
  return `${base}/api/urls/${shortCode}/qr`
}

export function getRedirectUrl(shortCode: string): string {
  const base = import.meta.env.VITE_API_URL || window.location.origin
  return `${base}/${shortCode}`
}
