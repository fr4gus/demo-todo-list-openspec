import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

// Placeholder: the auth change replaces this slice (and `selectToken`) with the real one.
interface AuthState {
  token: string | null
}

const initialState: AuthState = { token: null }

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setToken(state, action: PayloadAction<string>) {
      state.token = action.payload
    },
    clearToken(state) {
      state.token = null
    },
  },
})

export const { setToken, clearToken } = authSlice.actions
export default authSlice.reducer
