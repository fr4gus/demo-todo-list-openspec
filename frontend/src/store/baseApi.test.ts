import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { clearToken, setToken } from '../features/auth/authSlice'
import { healthApi } from '../features/health/healthApi'
import { server } from '../test/server'
import { setupStore } from './store'

function captureAuthHeader() {
  const seen: (string | null)[] = []
  server.use(
    http.get('*/api/v1/health', ({ request }) => {
      seen.push(request.headers.get('Authorization'))
      return HttpResponse.json({ status: 'UP' })
    }),
  )
  return seen
}

describe('baseApi', () => {
  it('sends the Bearer token when one exists', async () => {
    const seen = captureAuthHeader()
    const store = setupStore()
    store.dispatch(setToken('abc.def.ghi'))

    await store.dispatch(healthApi.endpoints.getHealth.initiate())

    expect(seen).toEqual(['Bearer abc.def.ghi'])
  })

  it('omits the Authorization header when there is no token', async () => {
    const seen = captureAuthHeader()
    const store = setupStore()
    store.dispatch(setToken('abc'))
    store.dispatch(clearToken())

    await store.dispatch(healthApi.endpoints.getHealth.initiate())

    expect(seen).toEqual([null])
  })
})
