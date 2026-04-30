// Search-heavy mix. Hits /api/search at three query shapes:
//   - short ("the")
//   - medium ("vacation 2024")
//   - long ("when we went to the lake last summer with the kids")
// Threshold: read p95 < 500ms (matches docs/slo.md).

import http from 'k6/http'
import { check, sleep } from 'k6'
import { login, BASE_URL } from './auth.js'

const QUERIES = [
  'the',
  'vacation 2024',
  'when we went to the lake last summer with the kids',
]

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '2m', target: 10 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{name:search}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export function setup() {
  return { token: login() }
}

export default function (data) {
  const q = QUERIES[Math.floor(Math.random() * QUERIES.length)]
  const res = http.get(
    `${BASE_URL}/api/search?q=${encodeURIComponent(q)}&size=20`,
    {
      headers: { Authorization: `Bearer ${data.token}` },
      tags: { name: 'search' },
    },
  )
  check(res, { 'search 200': r => r.status === 200 })
  sleep(1)
}
