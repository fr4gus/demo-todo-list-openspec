import { BackendStatus } from '../features/health/BackendStatus'

// Placeholder screen; the UI change replaces it with the TODO list.
export function HomePage() {
  return (
    <main className="mx-auto max-w-md p-6">
      <h1 className="text-2xl font-semibold">TODO</h1>
      <BackendStatus />
    </main>
  )
}
