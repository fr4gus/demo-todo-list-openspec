import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { AppRoutes } from '../../routing/AppRoutes'
import { renderWithProviders } from '../../test/renderWithProviders'
import { server } from '../../test/server'

const API = 'http://localhost:8080/api/v1'

function problem(status: number, code: string) {
  return HttpResponse.json({ status, code }, { status })
}

describe('ForgotPasswordPage', () => {
  async function submit(email = 'ana@example.com') {
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Email'), email)
    await user.click(screen.getByRole('button', { name: 'Send reset link' }))
  }

  it('shows the same neutral confirmation after a request', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/password-reset/request`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ message: 'sent' }, { status: 202 })
      }),
    )
    renderWithProviders(<AppRoutes />, { route: '/forgot-password' })

    await submit('nobody@example.com')

    expect(await screen.findByRole('status')).toHaveTextContent(
      'If an account exists for that email, we sent a link to reset your password.',
    )
    expect(body).toEqual({ email: 'nobody@example.com' })
    expect(
      screen.queryByRole('button', { name: 'Send reset link' }),
    ).not.toBeInTheDocument()
  })

  it('tells the user when the rate limit is hit', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/request`, () =>
        problem(429, 'TOO_MANY_REQUESTS'),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/forgot-password' })

    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests')
  })

  it('shows a generic message on other failures', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/request`, () => HttpResponse.error()),
    )
    renderWithProviders(<AppRoutes />, { route: '/forgot-password' })

    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })

  it('is reachable from sign-in', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AppRoutes />, { route: '/login' })

    await user.click(screen.getByRole('link', { name: 'Forgot your password?' }))

    expect(
      screen.getByRole('heading', { name: 'Forgot your password?' }),
    ).toBeInTheDocument()
  })
})

describe('ResetPasswordPage', () => {
  async function submit(password = 'brand-new-pass') {
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('New password'), password)
    await user.click(screen.getByRole('button', { name: 'Update password' }))
  }

  it('sends the token with the new password and redirects to sign-in with a success message', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderWithProviders(<AppRoutes />, { route: '/reset-password?token=abc_123' })

    await submit()

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Password updated')
    expect(body).toEqual({ token: 'abc_123', newPassword: 'brand-new-pass' })
  })

  it('explains an invalid or expired token and offers a new link', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, () =>
        problem(400, 'RESET_TOKEN_INVALID'),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/reset-password?token=expired' })

    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('invalid or has expired')
    expect(screen.getByRole('link', { name: 'Request a new link' })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })

  it('shows the invalid-link state straight away when the link has no token', () => {
    renderWithProviders(<AppRoutes />, { route: '/reset-password' })

    expect(screen.getByRole('alert')).toHaveTextContent('invalid or has expired')
    expect(screen.queryByLabelText('New password')).not.toBeInTheDocument()
  })

  it('keeps the form when the server rejects a weak password', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, () =>
        problem(400, 'VALIDATION_FAILED'),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/reset-password?token=abc' })

    await submit('longenough')

    expect(await screen.findByRole('alert')).toHaveTextContent('at least 8 characters')
    expect(screen.getByLabelText('New password')).toBeInTheDocument()
  })

  it('shows a generic message on other failures', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/confirm`, () => HttpResponse.error()),
    )
    renderWithProviders(<AppRoutes />, { route: '/reset-password?token=abc' })

    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })
})
