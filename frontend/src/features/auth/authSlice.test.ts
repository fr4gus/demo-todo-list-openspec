import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { healthApi } from '../health/healthApi'
import { setupStore } from '../../store/store'
import { server } from '../../test/server'
import { HOUR, makeSession, storeSession } from '../../test/fixtures'
import authReducer, { loggedIn, loggedOut } from './authSlice'
import { SESSION_KEY, signedOutState } from './authStorage'

describe('authSlice', () => {
  it('holds the session after loggedIn and clears it on loggedOut', () => {
    const session = makeSession('ADMIN')

    const signedIn = authReducer(signedOutState, loggedIn(session))
    expect(signedIn).toEqual({
      token: session.token,
      expiresAt: session.expiresAt,
      user: session.user,
    })
    expect(authReducer(signedIn, loggedOut())).toEqual(signedOutState)
  })
})

describe('session persistence', () => {
  it('writes the session to localStorage on login and removes it on logout', () => {
    const store = setupStore()
    const session = makeSession()

    store.dispatch(loggedIn(session))
    expect(JSON.parse(localStorage.getItem(SESSION_KEY)!)).toEqual(session)

    store.dispatch(loggedOut())
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('hydrates a stored unexpired session on load', () => {
    const session = makeSession('ADMIN')
    storeSession(session)

    const { auth } = setupStore().getState()

    expect(auth.token).toBe(session.token)
    expect(auth.user?.role).toBe('ADMIN')
  })

  it('drops an expired stored token on load', () => {
    storeSession(makeSession('USER', -HOUR))

    const { auth } = setupStore().getState()

    expect(auth).toEqual(signedOutState)
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it.each([
    ['corrupt JSON', '{nope'],
    ['an unexpected shape', JSON.stringify({ token: 1 })],
    [
      'an unknown role',
      JSON.stringify({ ...makeSession(), user: { ...makeSession().user, role: 'ROOT' } }),
    ],
  ])('treats %s as signed out', (_label, raw) => {
    localStorage.setItem(SESSION_KEY, raw)

    expect(setupStore().getState().auth).toEqual(signedOutState)
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('starts signed out when nothing is stored', () => {
    expect(setupStore().getState().auth).toEqual(signedOutState)
  })
})

describe('401 handling', () => {
  it('signs the user out and wipes the stored session when the API answers 401', async () => {
    server.use(
      http.get('*/api/v1/health', () =>
        HttpResponse.json({ status: 401, code: 'UNAUTHENTICATED' }, { status: 401 }),
      ),
    )
    const store = setupStore()
    store.dispatch(loggedIn(makeSession()))

    await store.dispatch(healthApi.endpoints.getHealth.initiate())

    expect(store.getState().auth).toEqual(signedOutState)
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('clears cached API data on logout', async () => {
    server.use(http.get('*/api/v1/health', () => HttpResponse.json({ status: 'UP' })))
    const store = setupStore()
    store.dispatch(loggedIn(makeSession()))
    await store.dispatch(healthApi.endpoints.getHealth.initiate())
    expect(Object.keys(store.getState().api.queries)).toHaveLength(1)

    store.dispatch(loggedOut())

    expect(Object.keys(store.getState().api.queries)).toHaveLength(0)
  })

  it('leaves a signed-out visitor alone when a request is rejected', async () => {
    server.use(
      http.get('*/api/v1/health', () =>
        HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    )
    const store = setupStore()

    const result = await store.dispatch(healthApi.endpoints.getHealth.initiate())

    expect(result.error).toMatchObject({ status: 401 })
    expect(store.getState().auth).toEqual(signedOutState)
  })
})
