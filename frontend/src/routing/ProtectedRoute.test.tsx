import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../test/renderWithProviders'
import { AppRoutes } from './AppRoutes'

describe('ProtectedRoute', () => {
  it('redirects an unauthenticated visitor to /login', () => {
    renderWithProviders(<AppRoutes />, { route: '/' })

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('renders the protected page when a token exists', async () => {
    server.use(http.get('*/api/v1/health', () => HttpResponse.json({ status: 'UP' })))
    renderWithProviders(<AppRoutes />, {
      route: '/',
      preloadedState: { auth: { token: 'abc' } },
    })

    expect(screen.getByRole('heading', { name: 'TODO' })).toBeInTheDocument()
    expect(await screen.findByText('Backend: UP')).toBeInTheDocument()
  })
})
