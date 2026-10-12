import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { signedOutState } from './authStorage'
import type { AuthState, Session } from './authTypes'

const authSlice = createSlice({
  name: 'auth',
  initialState: signedOutState as AuthState,
  reducers: {
    loggedIn(_state, action: PayloadAction<Session>) {
      const { token, expiresAt, user } = action.payload
      return { token, expiresAt, user }
    },
    loggedOut() {
      return signedOutState
    },
  },
})

export const { loggedIn, loggedOut } = authSlice.actions
export default authSlice.reducer
