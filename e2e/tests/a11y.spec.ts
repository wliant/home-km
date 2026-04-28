import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { registerAndLogin, uniqueEmail } from './helpers'

test.describe('accessibility (axe-core)', () => {
  test('login page has no critical violations', async ({ page }) => {
    await page.goto('/login')
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze()
    const critical = results.violations.filter(v => v.impact === 'critical' || v.impact === 'serious')
    expect(critical, JSON.stringify(critical, null, 2)).toEqual([])
  })

  test('home page has no critical violations after login', async ({ page, context }) => {
    const email = uniqueEmail()
    const token = await registerAndLogin(email, 'Axe1234567', 'Axe User')
    await context.addInitScript((tok) => {
      window.localStorage.setItem(
        'homekm-auth',
        JSON.stringify({
          state: {
            token: tok,
            refreshToken: tok,
            user: { id: 1, email: 'axe@test.local', displayName: 'Axe', isAdmin: false, isChild: false, isActive: true, createdAt: new Date().toISOString() },
            expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
            isAuthenticated: true,
          },
          version: 0,
        }),
      )
    }, token)
    await page.goto('/')
    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
    const critical = results.violations.filter(v => v.impact === 'critical')
    expect(critical, JSON.stringify(critical, null, 2)).toEqual([])
  })
})
