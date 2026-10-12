import type { AuthState, Role, Session } from './authTypes'

export const SESSION_KEY = 'todo.session'

export const signedOutState: AuthState = { token: null, expiresAt: null, user: null }

const ROLES: Role[] = ['USER', 'ADMIN']

function isSession(value: unknown): value is Session {
  if (typeof value !== 'object' || value === null) return false
  const s = value as Partial<Session>
  return (
    typeof s.token === 'string' &&
    typeof s.expiresAt === 'string' &&
    typeof s.user === 'object' &&
    s.user !== null &&
    typeof s.user.id === 'string' &&
    typeof s.user.fullName === 'string' &&
    typeof s.user.email === 'string' &&
    ROLES.includes(s.user.role)
  )
}

export function isExpired(expiresAt: string | null, now = Date.now()): boolean {
  if (expiresAt === null) return true
  const time = Date.parse(expiresAt)
  return Number.isNaN(time) || time <= now
}

/** Storage can throw (private windows, blocked site data); the app must work without it. */
export function saveSession(session: Session): void {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  } catch {
    // session simply won't survive a reload
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(SESSION_KEY)
  } catch {
    // nothing to clean up
  }
}

/** Restores a stored session; a missing, corrupt or expired one is dropped and counts as signed out. */
export function loadSession(now = Date.now()): AuthState {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (raw === null) return signedOutState
    const parsed: unknown = JSON.parse(raw)
    if (isSession(parsed) && !isExpired(parsed.expiresAt, now)) {
      return { token: parsed.token, expiresAt: parsed.expiresAt, user: parsed.user }
    }
  } catch {
    // fall through to dropping the entry
  }
  clearSession()
  return signedOutState
}
