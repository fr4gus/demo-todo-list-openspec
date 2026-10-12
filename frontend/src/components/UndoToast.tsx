import { useEffect } from 'react'

interface Props {
  /** Identifies the toast; a new key replaces the current toast and restarts the timer. */
  toastKey: string | null
  onUndo: () => void
  onDismiss: () => void
  durationMs?: number
}

export const TOAST_DURATION_MS = 5000

/** Bottom toast "Task Completed - Undo". The live region stays mounted so it is announced. */
export function UndoToast({
  toastKey,
  onUndo,
  onDismiss,
  durationMs = TOAST_DURATION_MS,
}: Props) {
  useEffect(() => {
    if (toastKey === null) return
    const timer = setTimeout(onDismiss, durationMs)
    return () => clearTimeout(timer)
  }, [toastKey, onDismiss, durationMs])

  return (
    <div
      aria-live="polite"
      className="pointer-events-none fixed inset-x-0 bottom-24 flex justify-center px-4"
    >
      {toastKey !== null && (
        <div className="pointer-events-auto flex items-center gap-2 rounded bg-gray-900 px-4 py-2 text-white shadow-lg">
          <span>Task Completed -</span>
          <button type="button" onClick={onUndo} className="font-semibold underline">
            Undo
          </button>
        </div>
      )}
    </div>
  )
}
