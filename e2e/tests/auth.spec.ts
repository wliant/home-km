import { test, expect } from '@playwright/test'
import { uniqueEmail } from './helpers'

const PASSWORD = 'E2eTest1234'

test.describe('Auth', () => {
  test('register then redirect to home', async ({ page }) => {
    const email = uniqueEmail()
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('E2E User')
    await page.getByLabel(/password/i).fill(PASSWORD)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')
  })

  test('login with wrong password shows error', async ({ page }) => {
    const email = uniqueEmail()
    // Register first
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('E2E User')
    await page.getByLabel(/password/i).fill(PASSWORD)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')

    // Clear auth and try wrong password
    await page.goto('/login')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/password/i).fill('WrongPass9')
    await page.getByRole('button', { name: /log in|sign in/i }).click()
    await expect(page.getByRole('alert').or(page.locator('[class*=error],[class*=red]'))).toBeVisible()
  })

  test('protected route without auth redirects to login', async ({ page }) => {
    await page.goto('/notes')
    await expect(page).toHaveURL(/\/login/)
  })

  test('sign out clears session', async ({ page }) => {
    const email = uniqueEmail()
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('E2E User')
    await page.getByLabel(/password/i).fill(PASSWORD)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')

    // Sign out
    await page.getByRole('button', { name: /sign out/i }).click()
    await expect(page).toHaveURL(/\/login/)

    // Protected route should redirect
    await page.goto('/notes')
    await expect(page).toHaveURL(/\/login/)
  })
})
