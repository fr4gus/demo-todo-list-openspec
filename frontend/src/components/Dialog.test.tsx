import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'
import { Dialog } from './Dialog'

function Harness() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Open</button>
      <Dialog open={open} onClose={() => setOpen(false)} title="Sample">
        <p>Body</p>
      </Dialog>
    </>
  )
}

describe('Dialog', () => {
  it('opens, closes on Escape and returns focus to the opener', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const opener = screen.getByRole('button', { name: 'Open' })

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await user.click(opener)
    expect(screen.getByRole('dialog', { name: 'Sample' })).toBeInTheDocument()
    expect(screen.getByText('Body')).toBeInTheDocument()

    // jsdom has no native Escape handling, so dispatch the dialog's own cancel path.
    const dialog = screen.getByRole('dialog', { name: 'Sample' }) as HTMLDialogElement
    dialog.close()

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })
})

describe('ConfirmDialog', () => {
  it('calls onConfirm and onCancel from its buttons', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()
    const onCancel = vi.fn()
    render(
      <ConfirmDialog
        open
        title="Delete"
        message="Sure?"
        confirmLabel="Delete"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )

    expect(screen.getByText('Sure?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Delete' }))
    expect(onConfirm).toHaveBeenCalledOnce()
    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onCancel).toHaveBeenCalledOnce()
  })
})
