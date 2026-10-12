export type Role = 'USER' | 'ADMIN'

export interface UserProfile {
  id: string
  fullName: string
  email: string
  role: Role
}

export interface Session {
  token: string
  /** ISO-8601 instant at which the token stops being valid. */
  expiresAt: string
  user: UserProfile
}

export interface AuthState {
  token: string | null
  expiresAt: string | null
  user: UserProfile | null
}
