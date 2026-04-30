// Smoke test: validates the rig works end-to-end without putting load
// on anything. Run before any longer scenario to catch broken
// credentials, unreachable host, etc.

import http from 'k6/http'
import { check, sleep } from 'k6'
import { login, BASE_URL } from './auth.js'

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
}

export function setup() {
  return { token: login() }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }

  const me = http.get(`${BASE_URL}/api/auth/me`, { headers })
  check(me, { 'me 200': r => r.status === 200 })

  const health = http.get(`${BASE_URL}/actuator/health`)
  check(health, { 'health 200': r => r.status === 200 })

  sleep(1)
}
