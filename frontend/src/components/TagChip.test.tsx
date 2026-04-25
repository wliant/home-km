import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import TagChip from './TagChip'

describe('TagChip', () => {
  it('renders the tag name', () => {
    render(<TagChip name="kitchen" />)
    expect(screen.getByText('kitchen')).toBeInTheDocument()
  })

  it('applies the color as background style', () => {
    render(<TagChip name="red tag" color="#ff0000" />)
    const chip = screen.getByText('red tag').closest('span')
    expect(chip).toHaveStyle({ backgroundColor: '#ff0000' })
  })

  it('shows remove button when onRemove is provided', () => {
    render(<TagChip name="removable" onRemove={() => {}} />)
    expect(screen.getByRole('button')).toBeInTheDocument()
  })

  it('does not show remove button when onRemove is absent', () => {
    render(<TagChip name="read-only" />)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('calls onRemove when remove button is clicked', async () => {
    const onRemove = vi.fn()
    render(<TagChip name="clickable" onRemove={onRemove} />)
    await userEvent.click(screen.getByRole('button'))
    expect(onRemove).toHaveBeenCalledOnce()
  })

  it('uses default color when none provided', () => {
    render(<TagChip name="default color" />)
    const chip = screen.getByText('default color').closest('span')
    expect(chip).toHaveStyle({ backgroundColor: '#6366f1' })
  })
})
