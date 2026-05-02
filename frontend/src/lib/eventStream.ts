/**
 * Lightweight server-sent-events client over fetch+ReadableStream so we can
 * attach the JWT in the Authorization header (the native EventSource API
 * doesn't support headers, and we don't want to leak tokens via query string).
 *
 * Reconnects with exponential backoff on disconnect. Honors AbortController
 * for clean teardown when the calling component unmounts.
 *
 * Wire format follows the SSE spec: `event:` and `data:` lines per record,
 * separated by blank lines. Multi-line `data:` payloads are concatenated
 * with newlines per the spec.
 */
export interface EventStreamOptions {
  url: string
  token: string | null
  signal: AbortSignal
  onEvent: (type: string, data: unknown) => void
  onError?: (err: unknown) => void
}

export async function consumeEventStream(opts: EventStreamOptions): Promise<void> {
  let backoff = 1000
  while (!opts.signal.aborted) {
    try {
      const headers: Record<string, string> = { Accept: 'text/event-stream' }
      if (opts.token) headers.Authorization = `Bearer ${opts.token}`
      const res = await fetch(opts.url, { headers, signal: opts.signal })
      if (!res.ok || !res.body) {
        // 401 means our token expired — bail; the surrounding hook will
        // re-subscribe after the auth refresh interceptor renews it.
        if (res.status === 401) return
        throw new Error(`SSE ${res.status}`)
      }
      backoff = 1000 // reset on a successful connect
      await readEvents(res.body, opts)
    } catch (err) {
      if (opts.signal.aborted) return
      opts.onError?.(err)
      await sleep(backoff, opts.signal)
      backoff = Math.min(backoff * 2, 30_000)
    }
  }
}

async function readEvents(body: ReadableStream<Uint8Array>, opts: EventStreamOptions): Promise<void> {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (!opts.signal.aborted) {
    const { value, done } = await reader.read()
    if (done) return
    buffer += decoder.decode(value, { stream: true })
    let sepIdx: number
    // Each event ends in a blank line (\n\n). Process each completed record.
    while ((sepIdx = buffer.indexOf('\n\n')) !== -1) {
      const record = buffer.slice(0, sepIdx)
      buffer = buffer.slice(sepIdx + 2)
      const parsed = parseRecord(record)
      if (parsed) opts.onEvent(parsed.type, parsed.data)
    }
  }
}

function parseRecord(record: string): { type: string; data: unknown } | null {
  let type = 'message'
  const dataLines: string[] = []
  for (const raw of record.split('\n')) {
    if (!raw || raw.startsWith(':')) continue // blank or comment
    const idx = raw.indexOf(':')
    if (idx < 0) continue
    const field = raw.slice(0, idx)
    // Per spec a single space after the colon is optional; skip if present.
    const value = raw.slice(idx + 1).replace(/^ /, '')
    if (field === 'event') type = value
    else if (field === 'data') dataLines.push(value)
  }
  if (dataLines.length === 0) return null
  const raw = dataLines.join('\n')
  try {
    return { type, data: JSON.parse(raw) }
  } catch {
    return { type, data: raw }
  }
}

function sleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise(resolve => {
    if (signal.aborted) return resolve()
    const timeout = setTimeout(() => resolve(), ms)
    signal.addEventListener('abort', () => { clearTimeout(timeout); resolve() }, { once: true })
  })
}
