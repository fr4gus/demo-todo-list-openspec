import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useLongPress } from './useLongPress'

function Target({
  onLongPress,
  onClick,
  enabled,
}: {
  onLongPress: () => void
  onClick: () => void
  enabled?: boolean
}) {
  const handlers = useLongPress(onLongPress, enabled)
  return (
    <div data-testid="t" onClick={onClick} {...handlers}>
      hold me
    </div>
  )
}

describe('useLongPress', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  function setup(enabled?: boolean) {
    const onLongPress = vi.fn()
    const onClick = vi.fn()
    render(<Target onLongPress={onLongPress} onClick={onClick} enabled={enabled} />)
    return { onLongPress, onClick, el: screen.getByTestId('t') }
  }

  it('fires after 500 ms', () => {
    const { onLongPress, el } = setup()
    fireEvent.pointerDown(el, { clientX: 5, clientY: 5 })
    act(() => void vi.advanceTimersByTime(499))
    expect(onLongPress).not.toHaveBeenCalled()
    act(() => void vi.advanceTimersByTime(1))
    expect(onLongPress).toHaveBeenCalledOnce()
  })

  it('cancels on early release', () => {
    const { onLongPress, el } = setup()
    fireEvent.pointerDown(el)
    act(() => void vi.advanceTimersByTime(300))
    fireEvent.pointerUp(el)
    act(() => void vi.advanceTimersByTime(1000))
    expect(onLongPress).not.toHaveBeenCalled()
  })

  it('cancels on movement but tolerates a small jitter', () => {
    const { onLongPress, el } = setup()
    fireEvent.pointerDown(el, { clientX: 0, clientY: 0 })
    fireEvent.pointerMove(el, { clientX: 3, clientY: 3 })
    act(() => void vi.advanceTimersByTime(500))
    expect(onLongPress).toHaveBeenCalledOnce()

    onLongPress.mockClear()
    fireEvent.pointerDown(el, { clientX: 0, clientY: 0 })
    fireEvent.pointerMove(el, { clientX: 40, clientY: 0 })
    act(() => void vi.advanceTimersByTime(1000))
    expect(onLongPress).not.toHaveBeenCalled()
  })

  it('suppresses the click that follows a long press, but not a normal click', () => {
    const { onClick, el } = setup()
    fireEvent.pointerDown(el)
    act(() => void vi.advanceTimersByTime(500))
    fireEvent.pointerUp(el)
    fireEvent.click(el)
    expect(onClick).not.toHaveBeenCalled()

    fireEvent.pointerDown(el)
    fireEvent.pointerUp(el)
    fireEvent.click(el)
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('does nothing when disabled', () => {
    const { onLongPress, el } = setup(false)
    fireEvent.pointerDown(el)
    act(() => void vi.advanceTimersByTime(1000))
    expect(onLongPress).not.toHaveBeenCalled()
  })
})
