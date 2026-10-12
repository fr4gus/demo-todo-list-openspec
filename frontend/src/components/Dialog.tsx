import { useEffect, useRef, type ReactNode } from 'react'

interface Props {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

/**
 * Modal built on the native <dialog>: focus trapping, Escape and the backdrop come from the
 * browser. Focus returns to the previously focused element when it closes.
 */
export function Dialog({ open, onClose, title, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const returnFocus = useRef<Element | null>(null)
  const closedByParent = useRef(false)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      returnFocus.current = document.activeElement
      dialog.showModal()
    } else if (!open && dialog.open) {
      closedByParent.current = true
      dialog.close()
    }
  }, [open])

  useEffect(
    () => () => {
      if (returnFocus.current instanceof HTMLElement) returnFocus.current.focus()
    },
    [],
  )

  return (
    <dialog
      ref={ref}
      aria-label={title}
      onClose={(event) => {
        // A nested dialog's close event reaches here through the React tree; ignore it.
        if (event.target !== event.currentTarget) return
        if (returnFocus.current instanceof HTMLElement) returnFocus.current.focus()
        // Only a user-initiated close (Escape) counts; a close driven by the parent does not.
        if (closedByParent.current) closedByParent.current = false
        else onClose()
      }}
      className="m-auto w-[90vw] max-w-md rounded-lg p-6 shadow-xl backdrop:bg-black/40"
    >
      {open && (
        <>
          <h2 className="mb-4 text-lg font-semibold">{title}</h2>
          {children}
        </>
      )}
    </dialog>
  )
}
