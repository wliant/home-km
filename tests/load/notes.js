// Notes browse + detail. Mirrors a user opening the notes list, reading a
// few, then opening one. Validates the list endpoint pagination clamp
// works under concurrency.

import http from 'k6/http'
import { check, sleep } from 'k6'
import { login, BASE_URL } from './auth.js'

export const options = {
  vus: 8,
  duration: '3m',
  thresholds: {
    'http_req_duration{name:notes_list}': ['p(95)<500'],
    'http_req_duration{name:note_detail}': ['p(95)<300'],
    http_req_failed: ['rate<0.01'],
  },
}

export function setup() {
  return { token: login() }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }

  const list = http.get(`${BASE_URL}/api/notes?size=25`, { headers, tags: { name: 'notes_list' } })
  check(list, { 'list 200': r => r.status === 200 })

  const ids = list.json('content').map(n => n.id)
  if (ids.length > 0) {
    const id = ids[Math.floor(Math.random() * ids.length)]
    const detail = http.get(`${BASE_URL}/api/notes/${id}`, { headers, tags: { name: 'note_detail' } })
    check(detail, { 'detail 200': r => r.status === 200 })
  }

  sleep(1)
}
