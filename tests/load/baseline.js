// Baseline mix used for the canonical numbers in LOAD-TESTING.md. Hits
// every endpoint that has a documented SLO. Thresholds gate at the SLO
// itself — runs that miss should block release.

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { login, BASE_URL } from './auth.js'

export const options = {
  vus: 5,
  duration: '3m',
  thresholds: {
    'http_req_duration{tag:read}': ['p(95)<500'],   // SLO: read p95 < 500 ms
    'http_req_duration{tag:write}': ['p(95)<1000'], // SLO: write p95 < 1 s
    http_req_failed: ['rate<0.005'],                // SLO: availability ≥ 99.5%
  },
}

export function setup() {
  return { token: login() }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }
  const tagsR = { tag: 'read' }
  const tagsW = { tag: 'write' }

  group('reads', () => {
    check(http.get(`${BASE_URL}/api/auth/me`, { headers, tags: tagsR }), { 'me': r => r.status === 200 })
    check(http.get(`${BASE_URL}/api/notes?size=20`, { headers, tags: tagsR }), { 'notes': r => r.status === 200 })
    check(http.get(`${BASE_URL}/api/folders`, { headers, tags: tagsR }), { 'folders': r => r.status === 200 })
    check(http.get(`${BASE_URL}/api/tags`, { headers, tags: tagsR }), { 'tags': r => r.status === 200 })
    check(http.get(`${BASE_URL}/api/search?q=the&size=10`, { headers, tags: tagsR }), { 'search': r => r.status === 200 })
  })

  group('writes', () => {
    const created = http.post(
      `${BASE_URL}/api/notes`,
      JSON.stringify({ title: `load-${Date.now()}`, body: 'x', label: 'custom' }),
      { headers: { ...headers, 'Content-Type': 'application/json' }, tags: tagsW },
    )
    check(created, { 'create 201': r => r.status === 201 })
    if (created.status === 201) {
      const id = created.json('id')
      check(
        http.del(`${BASE_URL}/api/notes/${id}`, null, { headers, tags: tagsW }),
        { 'delete 204': r => r.status === 204 },
      )
    }
  })

  sleep(1)
}
