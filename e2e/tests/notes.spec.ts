import { test, expect } from '@playwright/test'
import { uniqueEmail, registerAndLogin } from './helpers'

const PASSWORD = 'E2eTest1234'

test.describe('Notes', () => {
  test.beforeEach(async ({ page }) => {
    const email = uniqueEmail()
    await page.goto('/register')
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/display name/i).fill('Notes User')
    await page.getByLabel(/password/i).fill(PASSWORD)
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page).toHaveURL('/')
  })

  test('create a note and see it in the list', async ({ page }) => {
    const title = `Test note ${Date.now()}`
    await page.goto('/notes/new')
    await page.getByLabel(/title/i).fill(title)
    await page.getByRole('button', { name: /save|create/i }).click()
    await page.goto('/notes')
    await expect(page.getByText(title)).toBeVisible()
  })

  test('edit note title', async ({ page }) => {
    const title = `Edit me ${Date.now()}`
    const newTitle = `Edited ${Date.now()}`

    // Create
    await page.goto('/notes/new')
    await page.getByLabel(/title/i).fill(title)
    await page.getByRole('button', { name: /save|create/i }).click()

    // Edit
    await page.getByRole('link', { name: /edit/i }).click()
    await page.getByLabel(/title/i).fill(newTitle)
    await page.getByRole('button', { name: /save|update/i }).click()

    await expect(page.getByRole('heading', { name: newTitle })).toBeVisible()
  })

  test('delete note removes it from list', async ({ page }) => {
    const title = `Delete me ${Date.now()}`
    await page.goto('/notes/new')
    await page.getByLabel(/title/i).fill(title)
    await page.getByRole('button', { name: /save|create/i }).click()

    // Delete from detail page
    page.on('dialog', d => d.accept())
    await page.getByRole('button', { name: /delete/i }).click()

    await page.goto('/notes')
    await expect(page.getByText(title)).not.toBeVisible()
  })
})
