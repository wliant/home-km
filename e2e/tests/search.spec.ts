import { test, expect } from '@playwright/test'
import { uniqueEmail } from './helpers'

const PASSWORD = 'E2eTest1234'

test.describe('Search', () => {
  test.beforeEach(async ({ page }) => {
    const email = uniqueEmail()
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('Search User')
    await page.getByLabel(/password/i).fill(PASSWORD)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')
  })

  test('search finds note by unique title term', async ({ page }) => {
    const unique = `srchterm${Date.now()}`

    // Create a note with the unique term
    await page.goto('/notes/new')
    await page.getByLabel(/title/i).fill(`${unique} note`)
    await page.getByRole('button', { name: /save|create/i }).click()

    // Search for it
    await page.goto('/search')
    await page.getByRole('searchbox').or(page.getByPlaceholder(/search/i)).fill(unique)
    await page.keyboard.press('Enter')

    await expect(page.getByText(new RegExp(unique, 'i'))).toBeVisible({ timeout: 5000 })
  })

  test('search with no match shows empty state', async ({ page }) => {
    await page.goto('/search')
    await page.getByRole('searchbox').or(page.getByPlaceholder(/search/i)).fill('zzznomatch999')
    await page.keyboard.press('Enter')

    await expect(
      page.getByText(/no results|nothing found|0 results/i),
    ).toBeVisible({ timeout: 5000 })
  })
})
