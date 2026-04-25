import { apiClient } from './client'
import type { LoginRequest, LoginResponse, RegisterRequest, UpdateMeRequest, UserResponse } from '../types/auth'

export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<LoginResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/auth/login', data).then((r) => r.data),

  getMe: () =>
    apiClient.get<UserResponse>('/auth/me').then((r) => r.data),

  updateMe: (data: UpdateMeRequest) =>
    apiClient.put<UserResponse>('/auth/me', data).then((r) => r.data),
}
