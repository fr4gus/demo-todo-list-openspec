import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { setupStore } from '../../store/store'
import { server } from '../../test/server'
import { makeSession } from '../../test/fixtures'
import { authApi } from './authApi'
import { getProblem } from './problem'

const API = 'http://localhost:8080/api/v1'

function problem(status: number, code: string, detail: string) {
  return HttpResponse.json({ status, code, detail }, { status })
}

describe('authApi', () => {
  it('login posts credentials and returns the session', async () => {
    const session = makeSession()
    let body: unknown
    server.use(
      http.post(`${API}/auth/login`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(session)
      }),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.login.initiate({
        email: 'ana@example.com',
        password: 'secret123',
      }),
    )

    expect(body).toEqual({ email: 'ana@example.com', password: 'secret123' })
    expect(result).toEqual({ data: session })
  })

  it('login surfaces the problem code for an unknown email', async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        problem(401, 'EMAIL_NOT_FOUND', "Email doesn't exist"),
      ),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.login.initiate({ email: 'x@example.com', password: 'secret123' }),
    )

    expect('error' in result && getProblem(result.error)).toMatchObject({
      status: 401,
      code: 'EMAIL_NOT_FOUND',
      detail: "Email doesn't exist",
    })
  })

  it('register posts the profile and accepts the empty 201', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/register`, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 201 })
      }),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.register.initiate({
        fullName: 'Ana Perez',
        email: 'ana@example.com',
        password: 'secret123',
      }),
    )

    expect(body).toEqual({
      fullName: 'Ana Perez',
      email: 'ana@example.com',
      password: 'secret123',
    })
    expect('error' in result).toBe(false)
  })

  it('register surfaces EMAIL_IN_USE', async () => {
    server.use(
      http.post(`${API}/auth/register`, () =>
        problem(409, 'EMAIL_IN_USE', 'Email is already in use.'),
      ),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.register.initiate({
        fullName: 'Ana',
        email: 'ana@example.com',
        password: 'secret123',
      }),
    )

    expect('error' in result && getProblem(result.error)?.code).toBe('EMAIL_IN_USE')
  })

  it('requestPasswordReset posts the email and accepts 202', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/password-reset/request`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ message: 'sent' }, { status: 202 })
      }),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.requestPasswordReset.initiate({ email: 'ana@example.com' }),
    )

    expect(body).toEqual({ email: 'ana@example.com' })
    expect('error' in result).toBe(false)
  })

  it('requestPasswordReset surfaces the rate limit', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/request`, () =>
        problem(429, 'TOO_MANY_REQUESTS', 'Too many requests.'),
      ),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.requestPasswordReset.initiate({ email: 'ana@example.com' }),
    )

    expect('error' in result && result.error).toMatchObject({ status: 429 })
  })

  it('confirmPasswordReset posts token and password and accepts 204', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.confirmPasswordReset.initiate({
        token: 'tok',
        newPassword: 'brand-new-pass',
      }),
    )

    expect(body).toEqual({ token: 'tok', newPassword: 'brand-new-pass' })
    expect('error' in result).toBe(false)
  })

  it('confirmPasswordReset surfaces RESET_TOKEN_INVALID', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, () =>
        problem(
          400,
          'RESET_TOKEN_INVALID',
          'This password reset link is invalid or has expired.',
        ),
      ),
    )

    const result = await setupStore().dispatch(
      authApi.endpoints.confirmPasswordReset.initiate({
        token: 'old',
        newPassword: 'brand-new-pass',
      }),
    )

    expect('error' in result && getProblem(result.error)?.code).toBe(
      'RESET_TOKEN_INVALID',
    )
  })
})

describe('getProblem', () => {
  it('returns undefined for errors without a problem body', () => {
    expect(getProblem(undefined)).toBeUndefined()
    expect(getProblem({ status: 'FETCH_ERROR', error: 'boom' })).toBeUndefined()
    expect(getProblem({ status: 500, data: 'plain text' })).toBeUndefined()
  })
})
