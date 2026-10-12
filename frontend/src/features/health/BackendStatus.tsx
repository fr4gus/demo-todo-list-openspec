import type { ProblemDetail } from '../../store/baseApi'
import { useGetHealthQuery } from './healthApi'

export function BackendStatus() {
  const { data, error, isLoading } = useGetHealthQuery()

  if (isLoading) return <p role="status">Checking backend…</p>
  if (error) {
    const problem =
      'data' in error ? (error.data as ProblemDetail | undefined) : undefined
    return (
      <p role="alert" className="text-red-600">
        Backend unavailable: {problem?.detail ?? 'unexpected error'}
      </p>
    )
  }
  return (
    <p role="status" className="text-green-600">
      Backend: {data?.status}
    </p>
  )
}
