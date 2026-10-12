import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { selectToken } from './authSelectors'
import type { RootState } from './store'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

/** RFC 7807 problem document returned by the backend for every error. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  code?: string
  errors?: { field: string; message: string }[]
}

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: API_BASE_URL,
    prepareHeaders: (headers, { getState }) => {
      const token = selectToken(getState() as RootState)
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: [],
  endpoints: () => ({}),
})
