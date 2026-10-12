import type { ReactNode } from 'react'
import { Provider } from 'react-redux'
import { setupStore } from '../store/store'

const store = setupStore()

export function AppProviders({ children }: { children: ReactNode }) {
  return <Provider store={store}>{children}</Provider>
}
