import { isExpired } from '../features/auth/authStorage'
import type { RootState } from './store'

export const selectToken = (state: RootState) => state.auth.token
export const selectUser = (state: RootState) => state.auth.user
export const selectRole = (state: RootState) => state.auth.user?.role ?? null

/** Signed in means holding a token that has not expired yet. */
export const selectIsAuthenticated = (state: RootState) =>
  state.auth.token !== null && !isExpired(state.auth.expiresAt)
