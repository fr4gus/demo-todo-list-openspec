import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { HOUR, makeSession, signedInState, storeSession } from '../test/fixtures'
import { renderWithProviders } from '../test/renderWithProviders'
import { server } from '../test/server'
import { AppRoutes } from './AppRoutes'

beforeEach(() => {
  server.use(http.get('*/api/v1/health', () => HttpResponse.json({ status: 'UP' })))
})

describe('ProtectedRoute', () => {
  it('redirects a signed-out visitor to sign-in', () => {
    renderWithProviders(<AppRoutes />, { route: '/' })

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('redirects a signed-out visitor away from the admin area too', () => {
    renderWithProviders(<AppRoutes />, { route: '/admin/users' })

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('treats an expired token as signed out', () => {
    const { token, user } = makeSession('USER')
    renderWithProviders(<AppRoutes />, {
      route: '/',
      preloadedState: {
        auth: { token, user, expiresAt: new Date(Date.now() - HOUR).toISOString() },
      },
    })

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('shows the TODO screen to a USER', async () => {
    renderWithProviders(<AppRoutes />, {
      route: '/',
      preloadedState: signedInState('USER'),
    })

    expect(screen.getByRole('heading', { name: 'TODO' })).toBeInTheDocument()
    expect(await screen.findByText('Backend: UP')).toBeInTheDocument()
  })

  it('redirects a USER who opens an admin route to /', () => {
    renderWithProviders(<AppRoutes />, {
      route: '/admin/users',
      preloadedState: signedInState('USER'),
    })

    expect(screen.getByRole('heading', { name: 'TODO' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Users' })).not.toBeInTheDocument()
  })

  it('redirects an ADMIN who opens the user route to /admin/users', () => {
    renderWithProviders(<AppRoutes />, {
      route: '/',
      preloadedState: signedInState('ADMIN'),
    })

    expect(screen.getByRole('heading', { name: 'Users' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'TODO' })).not.toBeInTheDocument()
  })

  it('shows the admin screen to an ADMIN', () => {
    renderWithProviders(<AppRoutes />, {
      route: '/admin/users',
      preloadedState: signedInState('ADMIN'),
    })

    expect(screen.getByRole('heading', { name: 'Users' })).toBeInTheDocument()
  })

  it('sends an ADMIN to the admin overview after sign-in', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () => HttpResponse.json(makeSession('ADMIN'))),
    )
    const user = userEvent.setup()
    renderWithProviders(<AppRoutes />, { route: '/login' })

    await user.type(screen.getByLabelText('Email'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'admin-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument()
  })

  it('keeps a signed-in user on the page after a reload (session restored from storage)', () => {
    storeSession(makeSession('USER'))

    renderWithProviders(<AppRoutes />, { route: '/' })

    expect(screen.getByRole('heading', { name: 'TODO' })).toBeInTheDocument()
  })
})
