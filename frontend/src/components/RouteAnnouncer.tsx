import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Announces page changes to assistive tech and moves focus to the new page's
 * <h1>. Without this, React Router transitions are silent — screen readers
 * stay on the navigation element that was clicked, and the user has no way
 * to know the page changed.
 *
 * Drop one instance at the App root, inside the Router.
 */
export default function RouteAnnouncer() {
  const location = useLocation()
  const liveRegion = useRef<HTMLDivElement>(null)
  const isFirst = useRef(true)

  useEffect(() => {
    if (isFirst.current) {
      // Skip the initial mount — focus on first paint stays where the browser
      // put it (usually the URL bar or wherever the user came from).
      isFirst.current = false
      return
    }

    // Wait one frame so the new page's <h1> has rendered.
    const id = requestAnimationFrame(() => {
      const h1 = document.querySelector<HTMLHeadingElement>('main h1, h1')
      if (h1) {
        // tabIndex=-1 makes the heading programmatically focusable without
        // putting it in tab order.
        if (!h1.hasAttribute('tabindex')) h1.setAttribute('tabindex', '-1')
        h1.focus({ preventScroll: false })
      }
      if (liveRegion.current) {
        // document.title is updated by individual pages (or kept generic).
        // Either way, surfacing it after the route change is the most useful
        // announcement for screen-reader users.
        liveRegion.current.textContent = document.title || 'Page changed'
      }
    })
    return () => cancelAnimationFrame(id)
  }, [location.pathname])

  return (
    <div
      ref={liveRegion}
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className="sr-only"
    />
  )
}
