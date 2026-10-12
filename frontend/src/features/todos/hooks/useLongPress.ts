import { useCallback, useEffect, useRef, type PointerEvent, type MouseEvent } from 'react'

export const LONG_PRESS_MS = 500
const MOVE_TOLERANCE_PX = 10

/**
 * Pointer handlers that call `onLongPress` after the press has been held for 500 ms.
 * Releasing early or moving away cancels it; after it fires, the click that follows the
 * release is swallowed so the row's own click action does not also run.
 */
export function useLongPress(onLongPress: () => void, enabled = true) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const origin = useRef<{ x: number; y: number } | null>(null)
  const fired = useRef(false)
  const callback = useRef(onLongPress)
  useEffect(() => {
    callback.current = onLongPress
  })

  const cancel = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    origin.current = null
  }, [])

  useEffect(() => cancel, [cancel])

  const onPointerDown = (event: PointerEvent) => {
    fired.current = false
    if (!enabled) return
    cancel()
    origin.current = { x: event.clientX, y: event.clientY }
    timer.current = setTimeout(() => {
      timer.current = null
      fired.current = true
      callback.current()
    }, LONG_PRESS_MS)
  }

  const onPointerMove = (event: PointerEvent) => {
    if (!origin.current) return
    const dx = event.clientX - origin.current.x
    const dy = event.clientY - origin.current.y
    if (Math.hypot(dx, dy) > MOVE_TOLERANCE_PX) cancel()
  }

  const onClickCapture = (event: MouseEvent) => {
    if (fired.current) {
      fired.current = false
      event.stopPropagation()
      event.preventDefault()
    }
  }

  return {
    onPointerDown,
    onPointerMove,
    onPointerUp: cancel,
    onPointerLeave: cancel,
    onPointerCancel: cancel,
    onClickCapture,
    // Touch browsers open a context menu on long press; the app handles it instead.
    onContextMenu: (event: MouseEvent) => {
      if (enabled) event.preventDefault()
    },
  }
}
