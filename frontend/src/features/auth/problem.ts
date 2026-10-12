import type { ProblemDetail } from '../../store/baseApi'

/** Extracts the RFC 7807 body from an RTK Query error, if the server sent one. */
export function getProblem(error: unknown): ProblemDetail | undefined {
  if (typeof error === 'object' && error !== null && 'data' in error) {
    const data = (error as { data: unknown }).data
    if (typeof data === 'object' && data !== null && 'status' in data) {
      return data as ProblemDetail
    }
  }
  return undefined
}
