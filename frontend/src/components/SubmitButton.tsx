import type { ButtonHTMLAttributes } from 'react'

export function SubmitButton({
  children,
  ...props
}: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'>) {
  return (
    <button
      type="submit"
      className="rounded bg-blue-600 px-4 py-2 font-medium text-white disabled:opacity-50"
      {...props}
    >
      {children}
    </button>
  )
}
