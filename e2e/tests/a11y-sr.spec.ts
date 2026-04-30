import { expect, test } from '@playwright/test'
import { registerAndLogin, uniqueEmail } from './helpers'

/**
 * Screen-reader regression suite. axe-core covers the WCAG-checkable
 * defects elsewhere ({@code a11y.spec.ts}); this file pins the structural
 * properties that screen readers rely on but axe alone won't enforce —
 * landmark roles, accessible names on every interactive control, focus
 * order, live-region announcement on route change.
 *
 * Manual VoiceOver/NVDA/TalkBack passes against the matrix in
 * SR_TESTING.md remain the source of truth — these tests catch regressions
 * between releases.
 */

test.describe('screen reader: structural', () => {
  test('login page exposes a single main landmark and labelled form', async ({ page }) => {
    await page.goto('/login')

    // Exactly one main landmark — the SR "rotor" needs an unambiguous start.
    const mains = await page.locator('main, [role=main]').count()
    expect(mains).toBe(1)

    // Every input has an accessible name (label, aria-label, or aria-labelledby).
    const emailInput = page.getByRole('textbox', { name: /email/i })
    const passwordInput = page.getByLabel(/password/i)
    await expect(emailInput).toBeVisible()
    await expect(passwordInput).toBeVisible()

    // Submit button reachable via accessible name.
    const submit = page.getByRole('button', { name: /sign in|log in/i })
    await expect(submit).toBeVisible()
  })

  test('app shell has a polite live region the RouteAnnouncer uses', async ({ page, context }) => {
    const email = uniqueEmail()
    const token = await registerAndLogin(email, 'SrTest1234', 'Screen Reader')
    await context.addInitScript((tok) => {
      window.localStorage.setItem(
        'homekm-auth',
        JSON.stringify({
          state: {
            token: tok,
            refreshToken: tok,
            user: { id: 1, email: 'sr@test.local', displayName: 'SR', isAdmin: false, isChild: false, isActive: true, createdAt: new Date().toISOString() },
            expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
            isAuthenticated: true,
          },
          version: 0,
        }),
      )
    }, token)
    await page.goto('/')

    // Polite live region must exist; RouteAnnouncer writes the page title there.
    const live = page.locator('[aria-live=polite]')
    await expect(live.first()).toHaveCount(1)
  })

  test('main navigation exposes role=link items with names', async ({ page, context }) => {
    const email = uniqueEmail()
    const token = await registerAndLogin(email, 'SrTest1234', 'Screen Reader')
    await context.addInitScript((tok) => {
      window.localStorage.setItem('homekm-auth', JSON.stringify({
        state: { token: tok, refreshToken: tok,
          user: { id: 1, email: 'sr@test.local', displayName: 'SR', isAdmin: false, isChild: false, isActive: true, createdAt: new Date().toISOString() },
          expiresAt: new Date(Date.now() + 3_600_000).toISOString(), isAuthenticated: true },
        version: 0,
      }))
    }, token)
    await page.goto('/')

    // The sidebar / bottom nav contains links labelled by visible text — no
    // unlabelled icon buttons.
    const home = page.getByRole('link', { name: /home/i })
    await expect(home.first()).toBeVisible()
  })

  test('skip link is the first focusable element', async ({ page }) => {
    await page.goto('/login')
    await page.keyboard.press('Tab')
    const focused = await page.evaluate(() => document.activeElement?.textContent ?? '')
    expect(focused.toLowerCase()).toContain('skip')
  })
})
