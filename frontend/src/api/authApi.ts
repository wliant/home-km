import { apiClient } from './client'
import type {
  CreateInvitationRequest,
  InvitationResponse,
  IssuedInvitationResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  SessionResponse,
  UpdateMeRequest,
  UserResponse,
} from '../types/auth'

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
}
