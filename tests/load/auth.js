// Shared login helper. Each k6 scenario calls `setup()` once to fetch a
// JWT, then re-uses the token across iterations.

import http from 'k6/http'
import { check, fail } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const EMAIL = __ENV.LOAD_EMAIL || 'load@example.com'
const PASSWORD = __ENV.LOAD_PASSWORD || 'LoadTest1234'

export function login() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  if (!check(res, { 'login 200': r => r.status === 200 })) {
    fail(`login failed: ${res.status} ${res.body}`)
  }
  return res.json('token')
}

export { BASE_URL }
