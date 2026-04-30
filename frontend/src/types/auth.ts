export interface UserResponse {
  id: number
  email: string
  displayName: string
  isAdmin: boolean
  isChild: boolean
  isActive: boolean
  mfaEnabled: boolean
  createdAt: string
}

export interface LoginResponse {
  token: string | null
  refreshToken: string | null
  expiresAt: string | null
  user: UserResponse | null
  mfaRequired?: boolean
  mfaChallengeToken?: string | null
}

export interface MfaVerifyLoginRequest {
  challengeToken: string
  code: string
  rememberMe?: boolean
  deviceLabel?: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
  inviteToken?: string
}

export interface LoginRequest {
  email: string
  password: string
  rememberMe?: boolean
  deviceLabel?: string
}

export interface UpdateMeRequest {
  displayName?: string
  currentPassword?: string
  newPassword?: string
}

export interface SessionResponse {
  id: number
  deviceLabel: string | null
  userAgent: string | null
  ipAddress: string | null
  createdAt: string
  lastSeenAt: string | null
  expiresAt: string
  rememberMe: boolean
  current: boolean
}

export interface InvitationResponse {
  id: number
  email: string
  role: string
  createdAt: string
  expiresAt: string
  acceptedAt: string | null
  accepted: boolean
  expired: boolean
}

export interface IssuedInvitationResponse {
  invitation: InvitationResponse
  token: string
}

export interface CreateInvitationRequest {
  email: string
  role?: 'USER' | 'ADMIN'
}
