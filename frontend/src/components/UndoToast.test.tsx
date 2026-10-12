import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { UndoToast } from './UndoToast'

describe('UndoToast', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('shows the message with an Undo action in a polite live region', () => {
    const { container } = render(
      <UndoToast toastKey="a" onUndo={() => {}} onDismiss={() => {}} />,
    )
    expect(screen.getByText('Task Completed -')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Undo' })).toBeInTheDocument()
    expect(container.querySelector('[aria-live="polite"]')).not.toBeNull()
  })

  it('dismisses after 5 seconds', () => {
    const onDismiss = vi.fn()
    render(<UndoToast toastKey="a" onUndo={() => {}} onDismiss={onDismiss} />)

    act(() => void vi.advanceTimersByTime(4999))
    expect(onDismiss).not.toHaveBeenCalled()
    act(() => void vi.advanceTimersByTime(1))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('restarts the timer when a newer toast replaces it', () => {
    const onDismiss = vi.fn()
    const { rerender } = render(
      <UndoToast toastKey="a" onUndo={() => {}} onDismiss={onDismiss} />,
    )
    act(() => void vi.advanceTimersByTime(4000))
    rerender(<UndoToast toastKey="b" onUndo={() => {}} onDismiss={onDismiss} />)
    act(() => void vi.advanceTimersByTime(4000))
    expect(onDismiss).not.toHaveBeenCalled()
    act(() => void vi.advanceTimersByTime(1000))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('renders nothing visible and sets no timer without a toast', () => {
    const onDismiss = vi.fn()
    render(<UndoToast toastKey={null} onUndo={() => {}} onDismiss={onDismiss} />)
    expect(screen.queryByRole('button', { name: 'Undo' })).not.toBeInTheDocument()
    act(() => void vi.advanceTimersByTime(10000))
    expect(onDismiss).not.toHaveBeenCalled()
  })
})
