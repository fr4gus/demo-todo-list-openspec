import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { AppRoutes } from '../../routing/AppRoutes'
import { makeSession, signedInState, storeSession } from '../../test/fixtures'
import { renderWithProviders } from '../../test/renderWithProviders'
import { server } from '../../test/server'
import { SESSION_KEY } from './authStorage'

describe('sign out', () => {
  it.each(['USER', 'ADMIN'] as const)(
    'clears the token and shows sign-in for a %s',
    async (role) => {
      server.use(http.get('*/api/v1/health', () => HttpResponse.json({ status: 'UP' })))
      storeSession(makeSession(role))
      const user = userEvent.setup()
      const { store } = renderWithProviders(<AppRoutes />, {
        route: role === 'ADMIN' ? '/admin/users' : '/',
        preloadedState: signedInState(role),
      })
      expect(screen.getByText(makeSession(role).user.fullName)).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: 'Sign out' }))

      expect(store.getState().auth.token).toBeNull()
      expect(localStorage.getItem(SESSION_KEY)).toBeNull()
      expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    },
  )
})
