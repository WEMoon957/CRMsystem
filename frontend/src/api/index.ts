import http from './http'
import type {
  LoginResponse, UserVO, CustomerVO, PageResult, FollowUpRecord, IntentLevel,
  ProductVO, AttachmentVO
} from '@/types'

export interface CustomerQuery {
  keyword?: string
  intentLevel?: string
  page?: number
  size?: number
}

export interface CustomerSavePayload {
  name: string
  phone?: string
  company?: string
  position?: string
  source?: string
  industry?: string
  intentLevel: IntentLevel | string
  address?: string
  remark?: string
  ownerId?: number
  nextFollowupAt?: string | null
}

export const authApi = {
  login: (username: string, password: string) =>
    http.post<any, { code: number; data: LoginResponse }>('/api/auth/login', { username, password }).then(r => r.data),
  refresh: (refreshToken: string) =>
    http.post<any, { code: number; data: LoginResponse }>('/api/auth/refresh', { refreshToken }).then(r => r.data),
  logout: (refreshToken: string) =>
    http.post<any, { code: number; data?: any }>('/api/auth/logout', { refreshToken }).then(r => r.data),
  me: () => http.post<any, never>('/api/auth/me') as never as Promise<any>
}

export const customerApi = {
  page: (query: CustomerQuery) =>
    http.get<any, { code: number; data: PageResult<CustomerVO> }>('/api/customers', { params: query }).then(r => r.data),
  stats: () =>
    http.get<any, { code: number; data: Record<string, number> }>('/api/customers/stats').then(r => r.data),
  detail: (id: number) =>
    http.get<any, { code: number; data: CustomerVO }>(`/api/customers/${id}`).then(r => r.data),
  create: (payload: CustomerSavePayload) =>
    http.post<any, { code: number; data: CustomerVO }>('/api/customers', payload).then(r => r.data),
  update: (id: number, payload: CustomerSavePayload) =>
    http.put<any, { code: number; data: CustomerVO }>(`/api/customers/${id}`, payload).then(r => r.data),
  remove: (id: number) =>
    http.delete<any, { code: number; data?: any }>(`/api/customers/${id}`).then(r => r.data),
  assign: (id: number, ownerId: number) =>
    http.put<any, { code: number; data: CustomerVO }>(`/api/customers/${id}/assign`, { ownerId }).then(r => r.data)
}

export const followUpApi = {
  list: (customerId: number) =>
    http.get<any, { code: number; data: FollowUpRecord[] }>(`/api/customers/${customerId}/followups`).then(r => r.data),
  create: (customerId: number, payload: { content: string; resultLevel?: string; nextFollowupAt?: string | null }) =>
    http.post<any, { code: number; data?: any }>(`/api/customers/${customerId}/followups`, payload).then(r => r.data)
}

export const userApi = {
  list: () =>
    http.get<any, { code: number; data: UserVO[] }>('/api/users').then(r => r.data),
  create: (payload: { username: string; password: string; realName?: string; phone?: string; role?: string }) =>
    http.post<any, { code: number; data: UserVO }>('/api/users', payload).then(r => r.data),
  update: (id: number, payload: { realName?: string; phone?: string; status?: number; newPassword?: string }) =>
    http.put<any, { code: number; data: UserVO }>(`/api/users/${id}`, payload).then(r => r.data),
  remove: (id: number) =>
    http.delete<any, { code: number; data?: any }>(`/api/users/${id}`).then(r => r.data)
}

export const productApi = {
  list: (customerId: number) =>
    http.get<any, { code: number; data: ProductVO[] }>(`/api/customers/${customerId}/products`).then(r => r.data),
  create: (customerId: number, payload: ProductPayload) =>
    http.post<any, { code: number; data: ProductVO }>(`/api/customers/${customerId}/products`, payload).then(r => r.data),
  update: (customerId: number, productId: number, payload: ProductPayload) =>
    http.put<any, { code: number; data: ProductVO }>(`/api/customers/${customerId}/products/${productId}`, payload).then(r => r.data),
  remove: (customerId: number, productId: number) =>
    http.delete<any, { code: number; data?: any }>(`/api/customers/${customerId}/products/${productId}`).then(r => r.data)
}

export interface ProductPayload {
  productName: string
  spec?: string
  quantity?: number
  unitPrice?: number
  remark?: string
}

export const attachmentApi = {
  list: (customerId: number) =>
    http.get<any, { code: number; data: AttachmentVO[] }>(`/api/customers/${customerId}/attachments`).then(r => r.data),
  upload: (customerId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<any, { code: number; data: AttachmentVO }>(`/api/customers/${customerId}/attachments`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  },
  remove: (customerId: number, attachmentId: number) =>
    http.delete<any, { code: number; data?: any }>(`/api/customers/${customerId}/attachments/${attachmentId}`).then(r => r.data)
}
