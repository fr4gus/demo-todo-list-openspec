import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { AppRoutes } from '../../routing/AppRoutes'
import { renderWithProviders } from '../../test/renderWithProviders'
import { makeSession } from '../../test/fixtures'
import { server } from '../../test/server'

const API = 'http://localhost:8080/api/v1'

function problem(status: number, code: string, detail: string) {
  return HttpResponse.json({ status, code, detail }, { status })
}

async function signIn(email: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('SignInPage', () => {
  it('shows "Email doesn\'t exist" for an unknown email', async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        problem(401, 'EMAIL_NOT_FOUND', "Email doesn't exist"),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/login' })

    await signIn('nobody@example.com', 'secret123')

    expect(await screen.findByRole('alert')).toHaveTextContent("Email doesn't exist")
  })

  it('shows "invalid password" for a wrong password', async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        problem(401, 'INVALID_PASSWORD', 'invalid password'),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/login' })

    await signIn('ana@example.com', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('invalid password')
  })

  it('shows a generic message when the server is unreachable', async () => {
    server.use(http.post(`${API}/auth/login`, () => HttpResponse.error()))
    renderWithProviders(<AppRoutes />, { route: '/login' })

    await signIn('ana@example.com', 'secret123')

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })

  it('sends a USER to the TODO list and stores the session', async () => {
    const session = makeSession('USER')
    server.use(
      http.post(`${API}/auth/login`, () => HttpResponse.json(session)),
      http.get(`${API}/health`, () => HttpResponse.json({ status: 'UP' })),
    )
    const { store } = renderWithProviders(<AppRoutes />, { route: '/login' })

    await signIn('ana@example.com', 'secret123')

    expect(await screen.findByRole('heading', { name: 'TODO' })).toBeInTheDocument()
    expect(store.getState().auth.token).toBe(session.token)
  })

  it('links to sign-up and password recovery', () => {
    renderWithProviders(<AppRoutes />, { route: '/login' })

    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute(
      'href',
      '/signup',
    )
    expect(screen.getByRole('link', { name: 'Forgot your password?' })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })
})

describe('SignUpPage', () => {
  async function fillAndSubmit() {
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Full name'), 'Ana Perez')
    await user.type(screen.getByLabelText('Email'), 'ana@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign up' }))
  }

  it('sends the user back to sign-in after a successful sign-up', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/register`, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 201 })
      }),
    )
    renderWithProviders(<AppRoutes />, { route: '/signup' })

    await fillAndSubmit()

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Account created')
    expect(body).toEqual({
      fullName: 'Ana Perez',
      email: 'ana@example.com',
      password: 'secret123',
    })
  })

  it('says the email is already in use and stays on the form', async () => {
    server.use(
      http.post(`${API}/auth/register`, () =>
        problem(409, 'EMAIL_IN_USE', 'Email is already in use.'),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/signup' })

    await fillAndSubmit()

    expect(await screen.findByRole('alert')).toHaveTextContent('already in use')
    expect(screen.getByRole('button', { name: 'Sign up' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveValue('ana@example.com')
  })

  it('lists server-side validation errors', async () => {
    server.use(
      http.post(`${API}/auth/register`, () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'email', message: 'must be a valid email' }],
          },
          { status: 400 },
        ),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/signup' })

    await fillAndSubmit()

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('email: must be a valid email'),
    )
  })

  it('falls back to a generic message for unexpected failures', async () => {
    server.use(http.post(`${API}/auth/register`, () => HttpResponse.error()))
    renderWithProviders(<AppRoutes />, { route: '/signup' })

    await fillAndSubmit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })
})
