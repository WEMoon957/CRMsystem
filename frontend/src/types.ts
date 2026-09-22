export interface UserVO {
  id: number
  username: string
  realName: string | null
  phone: string | null
  role: 'ADMIN' | 'MEMBER'
  status: number
  customerCount?: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserVO
}

export interface CustomerVO {
  id: number
  name: string
  phone: string | null
  company: string | null
  position: string | null
  source: string | null
  industry: string | null
  intentLevel: IntentLevel
  intentLabel: string
  intentTone: string
  address: string | null
  remark: string | null
  ownerId: number
  ownerName: string
  nextFollowupAt: string | null
  lastContactedAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type IntentLevel = 'PENDING' | 'INTERESTED' | 'NOT_INTERESTED' | 'COOPERATED' | 'REFUSED'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface FollowUpRecord {
  id: number
  content: string
  resultLevel: string
  nextFollowupAt: string
  createdBy: string
  createdAt: string
}

export const INTENT_OPTIONS: { value: IntentLevel; label: string; tone: string }[] = [
  { value: 'PENDING', label: '待跟进', tone: 'info' },
  { value: 'INTERESTED', label: '有意向', tone: 'success' },
  { value: 'NOT_INTERESTED', label: '无意向', tone: 'warning' },
  { value: 'COOPERATED', label: '已合作', tone: 'primary' },
  { value: 'REFUSED', label: '不合作', tone: 'danger' }
]

export interface ProductVO {
  id: number
  customerId: number
  productName: string
  spec: string | null
  quantity: number | null
  unitPrice: number | null
  remark: string | null
  createdAt: string | null
}

export interface AttachmentVO {
  id: number
  customerId: number
  fileName: string
  fileType: string
  filePath: string
  fileSize: number | null
  createdBy: number | null
  createdAt: string | null
}

/** 将后端返回的相对路径 /uploads/xxx 转为可访问 URL（开发走 vite 代理，生产走同源反代） */
export function fileUrl(path: string): string {
  if (!path) return ''
  return path
}
