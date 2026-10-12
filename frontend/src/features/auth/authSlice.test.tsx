import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/renderWithProviders'
import { useAppDispatch } from '../../store/hooks'
import { clearToken, setToken } from './authSlice'

function SampleSessionControls() {
  const dispatch = useAppDispatch()
  return (
    <>
      <button onClick={() => dispatch(setToken('t-1'))}>sign in</button>
      <button onClick={() => dispatch(clearToken())}>sign out</button>
    </>
  )
}

describe('redux wiring in components', () => {
  it('updates the store when a component dispatches an action', async () => {
    const user = userEvent.setup()
    const { store } = renderWithProviders(<SampleSessionControls />)
    expect(store.getState().auth.token).toBeNull()

    await user.click(screen.getByRole('button', { name: 'sign in' }))
    expect(store.getState().auth.token).toBe('t-1')

    await user.click(screen.getByRole('button', { name: 'sign out' }))
    expect(store.getState().auth.token).toBeNull()
  })
})
