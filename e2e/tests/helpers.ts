import { request as playwrightRequest } from '@playwright/test'

const API_URL = process.env.API_URL ?? 'http://localhost:8080'

export async function registerAndLogin(email: string, password: string, displayName: string) {
  const ctx = await playwrightRequest.newContext({ baseURL: API_URL })
  await ctx.post('/api/auth/register', { data: { email, password, displayName } })
  const loginResp = await ctx.post('/api/auth/login', { data: { email, password } })
  const body = await loginResp.json()
  await ctx.dispose()
  return body.token as string
}

export function uniqueEmail() {
  return `e2e_${Date.now()}_${Math.random().toString(36).slice(2)}@test.local`
}
