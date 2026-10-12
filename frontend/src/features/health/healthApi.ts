import { baseApi } from '../../store/baseApi'

export interface HealthResponse {
  status: string
}

export const healthApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getHealth: build.query<HealthResponse, void>({ query: () => '/health' }),
  }),
})

export const { useGetHealthQuery } = healthApi
