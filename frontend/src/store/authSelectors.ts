import type { RootState } from './store'

export const selectToken = (state: RootState) => state.auth.token
export const selectIsAuthenticated = (state: RootState) => selectToken(state) !== null
