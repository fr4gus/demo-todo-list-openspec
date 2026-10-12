import { SESSION_KEY } from '../features/auth/authStorage'
import type { Role, Session } from '../features/auth/authTypes'

export const HOUR = 60 * 60 * 1000

export function makeSession(role: Role = 'USER', expiresInMs = 24 * HOUR): Session {
  return {
    token: `token-${role.toLowerCase()}`,
    expiresAt: new Date(Date.now() + expiresInMs).toISOString(),
    user: {
      id: '7d7b2487-1582-473b-8c22-7577cee28e43',
      fullName: role === 'ADMIN' ? 'Site Admin' : 'Ana Perez',
      email: role === 'ADMIN' ? 'admin@example.com' : 'ana@example.com',
      role,
    },
  }
}

/** Preloaded Redux auth state for a signed-in user. */
export function signedInState(role: Role = 'USER') {
  const { token, expiresAt, user } = makeSession(role)
  return { auth: { token, expiresAt, user } }
}

export function storeSession(session: Session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}
