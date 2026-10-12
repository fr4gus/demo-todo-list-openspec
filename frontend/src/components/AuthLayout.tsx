import type { ReactNode } from 'react'

interface Props {
  title: string
  children: ReactNode
}

export function AuthLayout({ title, children }: Props) {
  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center gap-6 p-6">
      <h1 className="text-2xl font-semibold">{title}</h1>
      {children}
    </main>
  )
}
