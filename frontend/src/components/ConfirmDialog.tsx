import { Dialog } from './Dialog'

interface Props {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
  destructive?: boolean
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  onConfirm,
  onCancel,
  destructive = false,
}: Props) {
  return (
    <Dialog open={open} onClose={onCancel} title={title}>
      <p className="mb-6 text-gray-700">{message}</p>
      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded border border-gray-300 px-4 py-2"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={onConfirm}
          className={`rounded px-4 py-2 font-medium text-white ${destructive ? 'bg-red-600' : 'bg-blue-600'}`}
        >
          {confirmLabel}
        </button>
      </div>
    </Dialog>
  )
}
