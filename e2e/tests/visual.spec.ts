import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { uniqueEmail } from './helpers'

// Visual tests are skipped on CI runs that haven't received committed baselines
// yet. Once an operator runs `--update-snapshots` and commits the PNGs, the
// directory exists and the tests start enforcing the contract.
const SNAPSHOT_DIR = path.join(__dirname, 'visual.spec.ts-snapshots')
const HAS_BASELINES = fs.existsSync(SNAPSHOT_DIR)

/**
 * Visual regression baseline. Snapshots are committed under
 * tests/visual.spec.ts-snapshots/. To refresh after intentional UI changes:
 *
 *   npx playwright test visual.spec.ts --update-snapshots
 *
 * Then commit the new PNGs.
 *
 * Snapshots are platform-sensitive (different fonts/AA on Mac vs Linux), so
 * the recommended workflow is to update them inside the official Playwright
 * Docker image so they match CI:
 *
 *   docker run --rm -v $(pwd):/work -w /work/e2e mcr.microsoft.com/playwright:v1.47.0-jammy \
 *     npx playwright test visual.spec.ts --update-snapshots
 */

test.describe('Visual regression', () => {
  test.skip(!HAS_BASELINES && !!process.env.CI,
    'No committed snapshots; bootstrap with `--update-snapshots` and commit the PNGs.')
  test.use({ viewport: { width: 1280, height: 800 } })

  test('login page', async ({ page }) => {
    await page.goto('/login')
    // Wait for the form to be interactable so animations have settled.
    await expect(page.getByRole('button', { name: /sign in|log in/i })).toBeVisible()
    await expect(page).toHaveScreenshot('login.png', {
      maxDiffPixelRatio: 0.02,
      fullPage: true,
    })
  })

  test('register page', async ({ page }) => {
    await page.goto('/register')
    await expect(page.getByRole('button', { name: /register|sign up|create/i })).toBeVisible()
    await expect(page).toHaveScreenshot('register.png', {
      maxDiffPixelRatio: 0.02,
      fullPage: true,
    })
  })

  test('home (logged in)', async ({ page }) => {
    const email = uniqueEmail()
    const password = 'VisualTest1234'
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('Visual Test')
    await page.getByLabel(/password/i).fill(password)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')
    // Allow async data + lazy chunks to settle.
    await page.waitForLoadState('networkidle')
    await expect(page).toHaveScreenshot('home.png', {
      maxDiffPixelRatio: 0.05,
      fullPage: true,
    })
  })
})
