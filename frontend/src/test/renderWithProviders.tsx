import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { setupStore, type RootState } from '../store/store'

interface Options {
  preloadedState?: Partial<RootState>
  route?: string
}

export function renderWithProviders(
  ui: ReactElement,
  { preloadedState, route = '/' }: Options = {},
) {
  const store = setupStore(preloadedState)
  const utils = render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </Provider>,
  )
  return { store, ...utils }
}
