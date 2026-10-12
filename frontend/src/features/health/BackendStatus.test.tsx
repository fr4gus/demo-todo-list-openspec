import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, it } from 'vitest'
import { server } from '../../test/server'
import { renderWithProviders } from '../../test/renderWithProviders'
import { BackendStatus } from './BackendStatus'

describe('BackendStatus (MSW integration)', () => {
  it('shows the status on success', async () => {
    server.use(http.get('*/api/v1/health', () => HttpResponse.json({ status: 'UP' })))
    renderWithProviders(<BackendStatus />)

    await screen.findByText('Backend: UP')
  })

  it('shows the problem detail on failure', async () => {
    server.use(
      http.get('*/api/v1/health', () =>
        HttpResponse.json(
          {
            status: 500,
            title: 'Internal Server Error',
            detail: 'An unexpected error occurred.',
          },
          { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderWithProviders(<BackendStatus />)

    await screen.findByText('Backend unavailable: An unexpected error occurred.')
  })
})
