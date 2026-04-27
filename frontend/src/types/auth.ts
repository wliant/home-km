export interface UserResponse {
  id: number
  email: string
  displayName: string
  isAdmin: boolean
  isChild: boolean
  isActive: boolean
  createdAt: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  expiresAt: string
  user: UserResponse
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateMeRequest {
  displayName?: string
  currentPassword?: string
  newPassword?: string
}
