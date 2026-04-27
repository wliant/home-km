import { describe, it, expect, beforeEach } from 'vitest'
import { useThemeStore } from './themeStore'

describe('themeStore', () => {
  beforeEach(() => {
    useThemeStore.setState({ theme: 'auto' })
  })

  it('starts with auto theme', () => {
    expect(useThemeStore.getState().theme).toBe('auto')
  })

  it('setTheme updates the theme', () => {
    useThemeStore.getState().setTheme('dark')
    expect(useThemeStore.getState().theme).toBe('dark')
  })

  it('cycles through themes correctly', () => {
    expect(useThemeStore.getState().theme).toBe('auto')

    useThemeStore.getState().setTheme('light')
    expect(useThemeStore.getState().theme).toBe('light')

    useThemeStore.getState().setTheme('dark')
    expect(useThemeStore.getState().theme).toBe('dark')

    useThemeStore.getState().setTheme('auto')
    expect(useThemeStore.getState().theme).toBe('auto')
  })
})
