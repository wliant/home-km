import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppLayout from '../../components/AppLayout'

/**
 * Endpoint for the PWA Web Share Target. The manifest declares
 * {@code share_target.action = "/share"} so when another app shares text or a
 * link into Home KM the OS opens this route. We forward the shared content
 * to the new-note editor pre-filled and bail. File-based shares require a SW
 * POST interceptor and are not yet wired.
 */
export default function SharePage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const title = params.get('title')?.trim() ?? ''
    const text = params.get('text')?.trim() ?? ''
    const url = params.get('url')?.trim() ?? ''

    const body = [text, url].filter(Boolean).join('\n\n')

    navigate(
      {
        pathname: '/notes/new',
        search: new URLSearchParams({
          ...(title && { title }),
          ...(body && { body }),
        }).toString(),
      },
      { replace: true },
    )
  }, [params, navigate])

  return (
    <AppLayout>
      <div className="text-sm text-gray-500 dark:text-gray-400">Opening editor…</div>
    </AppLayout>
  )
}
