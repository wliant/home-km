import { apiClient } from './client'
import type {
  CreateInvitationRequest,
  InvitationResponse,
  IssuedInvitationResponse,
  LoginRequest,
  LoginResponse,
  MfaVerifyLoginRequest,
  RegisterRequest,
  SessionResponse,
  UpdateMeRequest,
  UserResponse,
} from '../types/auth'

export interface MfaEnrollResponse {
  secret: string
  provisioningUri: string
}
export interface MfaStatusResponse {
  enabled: boolean
  unusedRecoveryCodes: number
}
export interface MfaRecoveryCodesResponse {
  recoveryCodes: string[]
}

export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<LoginResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/auth/login', data).then((r) => r.data),

  logout: (refreshToken: string | null) =>
    apiClient.post('/auth/logout', { refreshToken }),

  getMe: () =>
    apiClient.get<UserResponse>('/auth/me').then((r) => r.data),

  updateMe: (data: UpdateMeRequest) =>
    apiClient.put<UserResponse>('/auth/me', data).then((r) => r.data),

  listSessions: (currentRefreshToken: string | null) =>
    apiClient
      .get<SessionResponse[]>('/auth/sessions', {
        headers: currentRefreshToken ? { 'X-Refresh-Token': currentRefreshToken } : {},
      })
      .then((r) => r.data),

  revokeSession: (id: number) =>
    apiClient.delete(`/auth/sessions/${id}`),

  deleteMe: (password: string) =>
    apiClient.delete('/auth/me', { data: { password } }),

  verifyInvitation: (token: string) =>
    apiClient.get<InvitationResponse>(`/auth/invitations/${token}`).then((r) => r.data),

  listInvitations: () =>
    apiClient.get<InvitationResponse[]>('/admin/invitations').then((r) => r.data),

  createInvitation: (data: CreateInvitationRequest) =>
    apiClient.post<IssuedInvitationResponse>('/admin/invitations', data).then((r) => r.data),

  revokeInvitation: (id: number) =>
    apiClient.delete(`/admin/invitations/${id}`),

  verifyMfaLogin: (data: MfaVerifyLoginRequest) =>
    apiClient.post<LoginResponse>('/auth/mfa/verify-login', data).then((r) => r.data),

  mfaStatus: () =>
    apiClient.get<MfaStatusResponse>('/auth/mfa/status').then((r) => r.data),

  mfaEnroll: () =>
    apiClient.post<MfaEnrollResponse>('/auth/mfa/enroll').then((r) => r.data),

  mfaVerifyEnrollment: (code: string) =>
    apiClient.post<MfaRecoveryCodesResponse>('/auth/mfa/verify', { code }).then((r) => r.data),

  mfaDisable: (password: string) =>
    apiClient.post('/auth/mfa/disable', { password }),

  mfaRegenerateRecoveryCodes: () =>
    apiClient.post<MfaRecoveryCodesResponse>('/auth/mfa/recovery-codes').then((r) => r.data),
}
