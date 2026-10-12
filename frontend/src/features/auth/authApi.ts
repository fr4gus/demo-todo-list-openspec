import { baseApi } from '../../store/baseApi'
import type { Session } from './authTypes'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}

export interface ConfirmResetRequest {
  token: string
  newPassword: string
}

export const authApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    login: build.mutation<Session, LoginRequest>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
      transformResponse: (response: {
        token: string
        expiresAt: string
        user: Session['user']
      }) => ({
        token: response.token,
        expiresAt: response.expiresAt,
        user: response.user,
      }),
    }),
    register: build.mutation<void, RegisterRequest>({
      query: (body) => ({ url: '/auth/register', method: 'POST', body }),
    }),
    requestPasswordReset: build.mutation<void, { email: string }>({
      query: (body) => ({ url: '/auth/password-reset/request', method: 'POST', body }),
    }),
    confirmPasswordReset: build.mutation<void, ConfirmResetRequest>({
      query: (body) => ({ url: '/auth/password-reset/confirm', method: 'POST', body }),
    }),
  }),
})

export const {
  useLoginMutation,
  useRegisterMutation,
  useRequestPasswordResetMutation,
  useConfirmPasswordResetMutation,
} = authApi
