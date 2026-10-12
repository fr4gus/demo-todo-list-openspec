import type { Role } from '../features/auth/authTypes'

/** Where each role lands after signing in, and where the other role is sent back to. */
export function homePath(role: Role): string {
  return role === 'ADMIN' ? '/admin/users' : '/'
}
